/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptConfigurationFileAttributeCache
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

interface ScriptConfigurationCache {
    val scriptsDependenciesClasspathScopeCache: MutableMap<VirtualFile, GlobalSearchScope>
    val scriptsSdksCache: MutableMap<VirtualFile, Sdk?>
    val firstScriptSdk: Sdk?
    val allSdks: List<Sdk>
    val allNonIndexedSdks: List<Sdk>
    val allDependenciesClassFiles: List<VirtualFile>
    val allDependenciesSources: List<VirtualFile>
    val allDependenciesClassFilesScope: GlobalSearchScope
    val allDependenciesSourcesScope: GlobalSearchScope
    fun getCachedConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper?
    fun isConfigurationUpToDate(file: VirtualFile): Boolean
    fun setUpToDate(file: VirtualFile)
    fun replaceConfiguration(file: VirtualFile, new: ScriptCompilationConfigurationWrapper)
    fun clearConfigurationCaches(): List<VirtualFile>
    fun clearClassRootsCaches()
    fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean
}

data class CachedConfiguration(
    val file: VirtualFile,
    val result: ScriptCompilationConfigurationWrapper,
    val modificationStamp: Long
)

abstract class AbstractScriptConfigurationCache(val project: Project): ScriptConfigurationCache {
    private val cacheLock = ReentrantReadWriteLock()

    abstract operator fun get(file: VirtualFile): CachedConfiguration?
    abstract val allConfigurations: List<CachedConfiguration>

    override fun getCachedConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? =
        this[file]?.result

    override fun isConfigurationUpToDate(file: VirtualFile): Boolean {
        return this[file]?.modificationStamp == file.modificationStamp
    }

//    override fun setUpToDate(file: VirtualFile) {
//        scriptsModificationStampsCache.replace(file, file.modificationStamp)
//    }
//
//    override fun replaceConfiguration(file: VirtualFile, new: ScriptCompilationConfigurationWrapper) {
//        scriptDependenciesCache.replace(file, new)
//    }

    override val scriptsDependenciesClasspathScopeCache: MutableMap<VirtualFile, GlobalSearchScope> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = getConfiguration(file)
                ?: return@createWeakMap GlobalSearchScope.EMPTY_SCOPE

            val roots = compilationConfiguration.dependenciesClassPath
            val sdk = scriptsSdksCache[file]

            @Suppress("FoldInitializerAndIfToElvis")
            if (sdk == null) {
                return@createWeakMap NonClasspathDirectoriesScope.compose(ScriptConfigurationManager.toVfsRoots(roots))
            }

            return@createWeakMap NonClasspathDirectoriesScope.compose(
                sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() +
                        ScriptConfigurationManager.toVfsRoots(roots)
            )
        }

    override val scriptsSdksCache: MutableMap<VirtualFile, Sdk?> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = getConfiguration(file)
            return@createWeakMap getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(project)
        }

    private fun getConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? {
        val configuration = getCachedConfiguration(file)

        if (configuration != null) return configuration

        val ktFile = runReadAction { PsiManager.getInstance(project).findFile(file) as? KtFile } ?: return null
        return ScriptConfigurationManager.getInstance(project).getConfiguration(ktFile)
    }

    private fun getScriptSdk(compilationConfiguration: ScriptCompilationConfigurationWrapper?): Sdk? {
        // workaround for mismatched gradle wrapper and plugin version
        val javaHome = try {
            compilationConfiguration?.javaHome?.let { VfsUtil.findFileByIoFile(it, true) }
        } catch (e: Throwable) {
            null
        } ?: return null

        return getAllProjectSdks().find { it.homeDirectory == javaHome }
    }

    override val firstScriptSdk: Sdk?
        get() {
            val firstCachedScript = allConfigurations.firstOrNull()?.file ?: return null
            return scriptsSdksCache[firstCachedScript]
        }

    override val allSdks by ClearableLazyValue(cacheLock) {
        allConfigurations
            .mapNotNull { scriptsSdksCache[it.file] }
            .distinct()
    }

    override val allNonIndexedSdks by ClearableLazyValue(cacheLock) {
        allConfigurations
            .mapNotNull { scriptsSdksCache[it.file] }
            .filterNonModuleSdk()
            .distinct()
    }

    override val allDependenciesClassFiles by ClearableLazyValue(cacheLock) {
        val sdkFiles = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.CLASSES).toList() }

        val scriptDependenciesClasspath = allConfigurations
            .flatMap { it.result?.dependenciesClassPath }.distinct()

        sdkFiles + ScriptConfigurationManager.toVfsRoots(scriptDependenciesClasspath)
    }

    override val allDependenciesSources by ClearableLazyValue(cacheLock) {
        val sdkSources = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.SOURCES).toList() }

        val scriptDependenciesSources = allConfigurations
            .flatMap { it.result.dependenciesSources }.distinct()
        sdkSources + ScriptConfigurationManager.toVfsRoots(scriptDependenciesSources)
    }

    override val allDependenciesClassFilesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    override val allDependenciesSourcesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    private fun List<Sdk>.filterNonModuleSdk(): List<Sdk> {
        val moduleSdks = ModuleManager.getInstance(project).modules.map { ModuleRootManager.getInstance(it).sdk }
        return filterNot { moduleSdks.contains(it) }
    }

    override fun clearConfigurationCaches(): List<VirtualFile> {
        debug { "configuration caches cleared" }

       return clear()
    }

    abstract protected fun clear(): List<VirtualFile>

    override fun clearClassRootsCaches() {
        debug { "class roots caches cleared" }

        this::allSdks.clearValue()
        this::allNonIndexedSdks.clearValue()

        this::allDependenciesClassFiles.clearValue()
        this::allDependenciesClassFilesScope.clearValue()

        this::allDependenciesSources.clearValue()
        this::allDependenciesSourcesScope.clearValue()

        scriptsDependenciesClasspathScopeCache.clear()
        scriptsSdksCache.clear()

        val kotlinScriptDependenciesClassFinder =
            Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()

        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
    }

    override fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean {
        val scriptSdk = getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(project)
        val wasSdkChanged = scriptSdk != null && !allSdks.contains(scriptSdk)
        if (wasSdkChanged) {
            debug { "sdk was changed: $compilationConfiguration" }
            return true
        }

        val newClassRoots = ScriptConfigurationManager.toVfsRoots(compilationConfiguration.dependenciesClassPath)
        for (newClassRoot in newClassRoots) {
            if (!allDependenciesClassFiles.contains(newClassRoot)) {
                debug { "class root was changed: $newClassRoot" }
                return true
            }
        }

        val newSourceRoots = ScriptConfigurationManager.toVfsRoots(compilationConfiguration.dependenciesSources)
        for (newSourceRoot in newSourceRoots) {
            if (!allDependenciesSources.contains(newSourceRoot)) {
                debug { "source root was changed: $newSourceRoot" }
                return true
            }
        }
        return false
    }


}

class ScriptConfigurationCompositeCache(
    val memoryCache: ScriptConfigurationMemoryCache,
    val fileAttributesCache: ScriptConfigurationFileAttributeCache
): ScriptConfigurationCache {
}

private fun <R> KProperty0<R>.clearValue() {
    isAccessible = true
    (getDelegate() as ClearableLazyValue<*>).clear()
}

private class ClearableLazyValue<out T : Any>(
    private val lock: ReentrantReadWriteLock,
    private val compute: () -> T
) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        lock.write {
            if (value == null) {
                value = compute()
            }
            return value!!
        }
    }

    private var value: T? = null


    fun clear() {
        lock.write {
            value = null
        }
    }
}
