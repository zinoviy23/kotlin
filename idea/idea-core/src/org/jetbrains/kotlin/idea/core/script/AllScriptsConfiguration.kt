/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.ProjectTopics
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

interface AllScriptsConfiguration {
    fun getScriptSdk(file: VirtualFile): Sdk?
    fun getFirstScriptsSdk(): Sdk?

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope

    fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope
    fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope
    fun getAllScriptsDependenciesClassFiles(): List<VirtualFile>
    fun getAllScriptDependenciesSources(): List<VirtualFile>
}

interface InternalScriptConfigurationsProvider {
    val project: Project

    fun getConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile? = null
    ): ScriptCompilationConfigurationWrapper?

    val allConfigurations: List<CachedConfiguration>
}

class AllScriptsConfigurationImpl(
    val provider: InternalScriptConfigurationsProvider
): AllScriptsConfiguration {
    init {
        val connection = provider.project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                clearClassRootsCaches()
            }
        })
    }

    fun clearClassRootsCaches() {
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
            Extensions.getArea(provider.project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()

        ScriptDependenciesModificationTracker.getInstance(provider.project).incModificationCount()
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

    private val scriptsSdksCache: MutableMap<VirtualFile, Sdk?> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = provider.getConfiguration(file)
            return@createWeakMap getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(provider.project)
        }

    private fun scriptSdk(file: VirtualFile): Sdk? {
        return scriptsSdksCache[file]
    }

    internal fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean {
        val scriptSdk = getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(provider.project)
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

    override fun getFirstScriptsSdk(): Sdk? {
        val firstCachedScript = provider.allConfigurations.firstOrNull()?.file ?: return null
        return scriptsSdksCache[firstCachedScript]
    }

    private val cacheLock = ReentrantReadWriteLock()

    val allSdks by ClearableLazyValue(cacheLock) {
        provider.allConfigurations
            .mapNotNull { scriptsSdksCache[it.file] }
            .distinct()
    }

    val allNonIndexedSdks by ClearableLazyValue(cacheLock) {
        provider.allConfigurations
            .mapNotNull { scriptsSdksCache[it.file] }
            .filterNonModuleSdk()
            .distinct()
    }

    private fun List<Sdk>.filterNonModuleSdk(): List<Sdk> {
        val moduleSdks = ModuleManager.getInstance(provider.project).modules.map { ModuleRootManager.getInstance(it).sdk }
        return filterNot { moduleSdks.contains(it) }
    }

    val allDependenciesClassFiles by ClearableLazyValue(cacheLock) {
        val sdkFiles = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.CLASSES).toList() }

        val scriptDependenciesClasspath = provider.allConfigurations
            .flatMap { it.result.dependenciesClassPath }.distinct()

        sdkFiles + ScriptConfigurationManager.toVfsRoots(scriptDependenciesClasspath)
    }

    val allDependenciesSources by ClearableLazyValue(cacheLock) {
        val sdkSources = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.SOURCES).toList() }

        val scriptDependenciesSources = provider.allConfigurations
            .flatMap { it.result.dependenciesSources }.distinct()
        sdkSources + ScriptConfigurationManager.toVfsRoots(scriptDependenciesSources)
    }

    val allDependenciesClassFilesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    val allDependenciesSourcesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    override fun getScriptSdk(file: VirtualFile) = scriptSdk(file)

    override fun getAllScriptsDependenciesClassFilesScope() = allDependenciesClassFilesScope
    override fun getAllScriptDependenciesSourcesScope() = allDependenciesSourcesScope

    override fun getAllScriptsDependenciesClassFiles() = allDependenciesClassFiles
    override fun getAllScriptDependenciesSources() = allDependenciesSources

    private val scriptsDependenciesClasspathScopeCache: MutableMap<VirtualFile, GlobalSearchScope> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = provider.getConfiguration(file)
                ?: return@createWeakMap GlobalSearchScope.EMPTY_SCOPE

            val roots = compilationConfiguration.dependenciesClassPath
            val sdk = scriptsSdksCache[file]

            @Suppress("FoldInitializerAndIfToElvis")
            if (sdk == null) {
                return@createWeakMap NonClasspathDirectoriesScope.compose(ScriptConfigurationManager.toVfsRoots(roots))
            }

            return@createWeakMap NonClasspathDirectoriesScope.compose(
                sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() + ScriptConfigurationManager.toVfsRoots(roots)
            )
        }

    private fun scriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope {
        return scriptsDependenciesClasspathScopeCache[file] ?: GlobalSearchScope.EMPTY_SCOPE
    }

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile) = scriptDependenciesClassFilesScope(file)
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