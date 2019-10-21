/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileEditorManager
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager.Companion.toVfsRoots
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationCache
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.highlighter.OutsidersPsiFileSupportUtils
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.isNonScript
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible
import kotlin.script.experimental.api.valueOrNull

internal abstract class AbstractScriptConfigurationManager(protected val project: Project) : ScriptConfigurationManager {
    protected abstract val cache: ScriptConfigurationCache

    protected val rootsManager = ScriptClassRootsManager(project)

    @Deprecated("Use getScriptClasspath(KtFile) instead")
    override fun getScriptClasspath(file: VirtualFile): List<VirtualFile> {
        val ktFile = getKtFile(file) ?: return emptyList()
        return getScriptClasspath(ktFile)
    }

    override fun getScriptClasspath(file: KtFile): List<VirtualFile> =
        toVfsRoots(getConfiguration(file)?.dependenciesClassPath.orEmpty())

    fun getCachedConfiguration(file: VirtualFile?): ScriptCompilationConfigurationWrapper? {
        if (file == null) return null
        return cache[file]?.result
    }

    override fun isConfigurationCached(file: KtFile): Boolean {
        return getCachedConfiguration(file.originalFile.virtualFile) != null
    }

    override fun getConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper? {
        return getConfiguration(file.originalFile.virtualFile, file)
    }

    fun getConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile? = null
    ): ScriptCompilationConfigurationWrapper? {
        val fileOrigin = OutsidersPsiFileSupportUtils.getOutsiderFileOrigin(project, virtualFile)
        if (fileOrigin != null) {
            return getCachedConfiguration(fileOrigin)
        }

        val cached = cache[virtualFile]
        if (cached != null) return cached.result

        val ktFile = getKtFile(virtualFile, preloadedKtFile) ?: return null
        return rootsManager.transaction {
            reloadConfigurationInTransaction(ktFile, true)?.valueOrNull()
        }
    }

    override fun ensureUpToDate(files: List<KtFile>, loadEvenWillNotBeApplied: Boolean): Boolean {
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return false

        rootsManager.transaction {
            files.forEach { file ->
                val virtualFile = file.originalFile.virtualFile
                if (virtualFile != null) {
                    val state = cache[virtualFile]
                    if (state == null || !state.isUpToDate) {
                        reloadConfigurationInTransaction(file, state == null, loadEvenWillNotBeApplied)
                    }
                }
            }
        }

        return false
    }

    internal fun forceReload(file: KtFile) {
        rootsManager.transaction {
            reloadConfigurationInTransaction(file)
        }
    }
    
    protected abstract fun reloadConfigurationInTransaction(
        file: KtFile,
        isFirstLoad: Boolean = getCachedConfiguration(file.originalFile.virtualFile) == null,
        loadEvenWillNotBeApplied: Boolean = true
    ): ScriptCompilationConfigurationResult?

    /**
     * Clear configuration caches
     * Start re-highlighting for opened scripts
     */
    override fun clearConfigurationCachesAndRehighlight() {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        if (project.isOpen) {
            rehighlightOpenedScripts()
        }
    }

    private fun rehighlightOpenedScripts() {
        val openedScripts = FileEditorManager.getInstance(project).openFiles.filterNot { it.isNonScript() }
        updateHighlighting(openedScripts)
    }

    protected fun updateHighlighting(files: List<VirtualFile>) {
        if (files.isEmpty()) return

        GlobalScope.launch(EDT(project)) {
            if (project.isDisposed) return@launch

            val openFiles = FileEditorManager.getInstance(project).openFiles
            val openScripts = files.filter { it.isValid && openFiles.contains(it) }

            openScripts.forEach {
                PsiManager.getInstance(project).findFile(it)?.let { psiFile ->
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }
    }

    private fun getKtFile(
        virtualFile: VirtualFile?,
        ktFile: KtFile? = null
    ): KtFile? {
        if (virtualFile == null) return null
        if (ktFile != null) {
            check(ktFile.virtualFile == virtualFile)
            return ktFile
        } else {
            return runReadAction { PsiManager.getInstance(project).findFile(virtualFile) as? KtFile }
        }
    }

    ///////////////////

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
            Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()

        ScriptDependenciesModificationTracker.getInstance(project)
            .incModificationCount()
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
            val compilationConfiguration = getConfiguration(file)
            return@createWeakMap getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(project)
        }

    private fun scriptSdk(file: VirtualFile): Sdk? {
        return scriptsSdksCache[file]
    }

    internal fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean {
        val scriptSdk = getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(project)

        val wasSdkChanged = scriptSdk != null && !allSdks.contains(scriptSdk)
        if (wasSdkChanged) {
            debug { "sdk was changed: $compilationConfiguration" }
            return true
        }

        val newClassRoots = ScriptConfigurationManager.toVfsRoots(
            compilationConfiguration.dependenciesClassPath
        )
        for (newClassRoot in newClassRoots) {
            if (!allDependenciesClassFiles.contains(newClassRoot)) {
                debug { "class root was changed: $newClassRoot" }
                return true
            }
        }

        val newSourceRoots = ScriptConfigurationManager.toVfsRoots(
            compilationConfiguration.dependenciesSources
        )
        for (newSourceRoot in newSourceRoots) {
            if (!allDependenciesSources.contains(newSourceRoot)) {
                debug { "source root was changed: $newSourceRoot" }
                return true
            }
        }
        return false
    }

    override fun getFirstScriptsSdk(): Sdk? {
        val firstCachedScript = cache.all().firstOrNull()?.file ?: return null
        return scriptsSdksCache[firstCachedScript]
    }

    private val cacheLock = ReentrantReadWriteLock()

    val allSdks by ClearableLazyValue(cacheLock) {
        cache.all()
            .mapNotNull { scriptsSdksCache[it.file] }
            .distinct()
    }

    val allNonIndexedSdks by ClearableLazyValue(cacheLock) {
        cache.all()
            .mapNotNull { scriptsSdksCache[it.file] }
            .filterNonModuleSdk()
            .distinct()
    }

    private fun List<Sdk>.filterNonModuleSdk(): List<Sdk> {
        val moduleSdks = ModuleManager.getInstance(project).modules.map { ModuleRootManager.getInstance(it).sdk }
        return filterNot { moduleSdks.contains(it) }
    }

    val allDependenciesClassFiles by ClearableLazyValue(cacheLock) {
        val sdkFiles = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.CLASSES).toList() }

        val scriptDependenciesClasspath = cache.all()
            .flatMap { it.result.dependenciesClassPath }.distinct()

        sdkFiles + ScriptConfigurationManager.toVfsRoots(
            scriptDependenciesClasspath
        )
    }

    val allDependenciesSources by ClearableLazyValue(cacheLock) {
        val sdkSources = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.SOURCES).toList() }

        val scriptDependenciesSources = cache.all()
            .flatMap { it.result.dependenciesSources }.distinct()
        sdkSources + ScriptConfigurationManager.toVfsRoots(
            scriptDependenciesSources
        )
    }

    val allDependenciesClassFilesScope by ClearableLazyValue(
        cacheLock
    ) {
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
            val compilationConfiguration = getConfiguration(file)
                ?: return@createWeakMap GlobalSearchScope.EMPTY_SCOPE

            val roots = compilationConfiguration.dependenciesClassPath
            val sdk = scriptsSdksCache[file]

            @Suppress("FoldInitializerAndIfToElvis")
            if (sdk == null) {
                return@createWeakMap NonClasspathDirectoriesScope.compose(
                    ScriptConfigurationManager.toVfsRoots(
                        roots
                    )
                )
            }

            return@createWeakMap NonClasspathDirectoriesScope.compose(
                sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() + ScriptConfigurationManager.toVfsRoots(
                    roots
                )
            )
        }

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope {
        return scriptsDependenciesClasspathScopeCache[file] ?: GlobalSearchScope.EMPTY_SCOPE
    }
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