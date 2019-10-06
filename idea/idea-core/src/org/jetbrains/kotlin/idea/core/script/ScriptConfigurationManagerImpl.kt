/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileEditorManager
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
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.ui.EditorNotifications
import com.intellij.util.containers.ConcurrentFactoryMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager.Companion.toVfsRoots
import org.jetbrains.kotlin.idea.core.script.dependencies.*
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.isNonScript
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrNull

class ScriptConfigurationManagerImpl internal constructor(private val project: Project) : ScriptConfigurationManager {
    private val rootsManager = ScriptClassRootsManager(project)

    private val memoryCache: ScriptConfigurationCache = ScriptConfigurationMemoryCache(project)
    private val fileAttributesCache = ScriptConfigurationFileAttributeCache()

    private val fromRefinedLoader = FromRefinedConfigurationLoader()
    private val loaders = arrayListOf(
        OutsiderFileDependenciesLoader(this),
        fromRefinedLoader,
        fileAttributesCache
    )

    private val backgroundLoader = BackgroundLoader(project, rootsManager, ::reloadConfigurationAsync)

    private val listener = ScriptsListener(project, this)

    private val allConfigurations: List<CachedConfiguration> = TODO()

    /**
     * Save configurations into cache.
     * Start indexing for new class/source roots.
     * Re-highlight opened scripts with changed configuration.
     */
    override fun saveCompilationConfigurationAfterImport(files: List<Pair<VirtualFile, ScriptCompilationConfigurationResult>>) {
        rootsManager.transaction {
            for ((file, result) in files) {
                saveConfiguration(file, result, skipNotification = true)
            }
        }
    }

    /**
     * Start configuration update for files if configuration isn't up to date.
     * Start indexing for new class/source roots.
     *
     * @return true if update was started for any file, false if all configurations are cached
     */
    override fun updateConfigurationsIfNotCached(files: List<KtFile>): Boolean {
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return false

        val notCached = files.filterNot { isConfigurationUpToDate(it.originalFile.virtualFile) }
        if (notCached.isNotEmpty()) {
            rootsManager.transaction {
                for (file in notCached) {
                    reloadConfiguration(file)
                }
            }
            return true
        }

        return false
    }

    /**
     * Check if configuration is already cached for [file] (in cache or FileAttributes).
     * Don't check if file was changed after the last update.
     * Supposed to be used to switch highlighting off for scripts without configuration.
     * to avoid all file being highlighted in red.
     */
    override fun isConfigurationCached(file: KtFile): Boolean {
        return isConfigurationCached(file.originalFile.virtualFile)
    }

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

    @Deprecated("Use getScriptClasspath(KtFile) instead")
    override fun getScriptClasspath(file: VirtualFile): List<VirtualFile> {
        val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return emptyList()
        return getScriptClasspath(ktFile)
    }

    override fun getScriptClasspath(file: KtFile): List<VirtualFile> =
        toVfsRoots(getConfiguration(file)?.dependenciesClassPath.orEmpty())

    private fun getConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile? = null
    ): ScriptCompilationConfigurationWrapper? {
        val cached = getCachedConfiguration(virtualFile)
        if (cached != null) {
            return cached
        }

        if (ScriptDefinitionsManager.getInstance(project).isReady() && !isConfigurationUpToDate(virtualFile)) {
            val ktFile = if (preloadedKtFile != null) preloadedKtFile.also { check(it.virtualFile == virtualFile) }
            else runReadAction { PsiManager.getInstance(project).findFile(virtualFile) as? KtFile } ?: return null

            rootsManager.transaction {
                reloadConfiguration(ktFile)
            }
        }

        return getCachedConfiguration(virtualFile)
    }

    override fun getConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper? {
        return getConfiguration(file.virtualFile, file)
    }

    @TestOnly
    fun updateScriptDependenciesSynchronously(file: PsiFile) {
        val scriptDefinition = file.findScriptDefinition() ?: return
        assert(file is KtFile) {
            "PsiFile should be a KtFile, otherwise script dependencies cannot be loaded"
        }

        if (isConfigurationUpToDate(file.virtualFile)) return

        rootsManager.transaction {
            val result = fromRefinedLoader.loadDependencies(true, file as KtFile, scriptDefinition)
            if (result != null) {
                saveConfiguration(file.originalFile.virtualFile, result, skipNotification = true, skipSaveToAttributes = false)
            }
        }
    }

    private fun reloadConfiguration(file: KtFile) {
        val virtualFile = file.originalFile.virtualFile

        TODO("lock to do it only once at same time")
//        memoryCache.setUpToDate(virtualFile)

        val scriptDefinition = file.findScriptDefinition() ?: return

        val (asyncLoaders, syncLoaders) = loaders.partition { it.isAsync(file, scriptDefinition) }

        reloadConfigurationBy(file, scriptDefinition, syncLoaders)

        if (asyncLoaders.isNotEmpty()) {
            backgroundLoader.scheduleAsync(file)
        }
    }

    private fun reloadConfigurationAsync(file: KtFile) {
        val scriptDefinition = file.findScriptDefinition() ?: return

        val asyncLoaders = loaders.filter { it.isAsync(file, scriptDefinition) }

        if (asyncLoaders.size > 1) {
            LOG.warn("There are more than one async compilation configuration loader. " +
                             "This mean that the last one will overwrite the results of the previous ones: " +
                             asyncLoaders.joinToString { it.javaClass.name })
        }

        reloadConfigurationBy(file, scriptDefinition, asyncLoaders)
    }

    private fun reloadConfigurationBy(file: KtFile, scriptDefinition: ScriptDefinition, loaders: List<ScriptDependenciesLoader>) {
        val firstLoad = memoryCache[file.originalFile.virtualFile]?.result == null

        loaders.forEach { loader ->
            val result = loader.loadDependencies(firstLoad, file, scriptDefinition)
            if (result != null) {
                return saveConfiguration(file.originalFile.virtualFile, result, loader.skipNotification, loader.skipSaveToAttributes)
            }
        }
    }

    /**
     * Save [newResult] for [file] into caches and update highlih.
     * Should be called inside `rootsManager.transaction { ... }`.
     *
     * @param skipNotification forces loading new configuration even if auto reload is disabled.
     * @param skipSaveToAttributes skips saving to FileAttributes (used in [ScriptConfigurationFileAttributeCache] only).
     *
     * @sample ScriptConfigurationManager.getConfiguration
     */
    private fun saveConfiguration(
        file: VirtualFile,
        newResult: ScriptCompilationConfigurationResult,
        skipNotification: Boolean = false,
        skipSaveToAttributes: Boolean = false
    ) {
        debug(file) { "configuration received = $newResult" }

        saveReports(file, newResult.reports)

        val newConfiguration = newResult.valueOrNull()
        if (newConfiguration != null) {
            val oldConfiguration = getCachedConfiguration(file)
            if (oldConfiguration == newConfiguration) {
                file.removeScriptDependenciesNotificationPanel(project)
            } else {
                val autoReload = skipNotification
                        || oldConfiguration == null
                        || KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled
                        || ApplicationManager.getApplication().isUnitTestMode

                if (autoReload) {
                    if (oldConfiguration != null) {
                        file.removeScriptDependenciesNotificationPanel(project)
                    }
                    saveChangedConfiguration(file, newConfiguration, skipSaveToAttributes)
                } else {
                    debug(file) {
                        "configuration changed, notification is shown: old = $oldConfiguration, new = $newConfiguration"
                    }
                    file.addScriptDependenciesNotificationPanel(
                        newConfiguration, project,
                        onClick = {
                            file.removeScriptDependenciesNotificationPanel(project)
                            rootsManager.transaction {
                                saveChangedConfiguration(file, it, skipSaveToAttributes)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun saveChangedConfiguration(
        file: VirtualFile,
        newConfiguration: ScriptCompilationConfigurationWrapper?,
        skipSaveToAttributes: Boolean
    ) {
        rootsManager.checkInTransaction()
        debug(file) { "configuration changed = $newConfiguration" }

        if (newConfiguration != null) {
            if (hasNotCachedRoots(newConfiguration)) {
                rootsManager.markNewRoot(file, newConfiguration)
            }

            if (!skipSaveToAttributes) {
                debug(file) { "configuration saved to file attributes: $newConfiguration" }

                fileAttributesCache.save(file, newConfiguration)
            }

            memoryCache[file] = newConfiguration
            clearClassRootsCaches()
        }

        updateHighlighting(listOf(file))
    }

    private fun saveReports(
        file: VirtualFile,
        newReports: List<ScriptDiagnostic>
    ) {
        val oldReports = IdeScriptReportSink.getReports(file)
        if (oldReports != newReports) {
            debug(file) { "new script reports = $newReports" }

            ServiceManager.getService(project, ScriptReportSink::class.java).attachReports(file, newReports)

            GlobalScope.launch(EDT(project)) {
                if (project.isDisposed) return@launch

                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        }
    }

    private fun updateHighlighting(files: List<VirtualFile>) {
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

    private fun getCachedConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? =
        memoryCache[file]?.result

    private fun isConfigurationCached(file: VirtualFile): Boolean {
        return getCachedConfiguration(file) != null || file in fileAttributesCache
    }

    private fun isConfigurationUpToDate(file: VirtualFile): Boolean {
        return isConfigurationCached(file) && memoryCache[file]?.isUpToDate ?: false
    }

    private fun rehighlightOpenedScripts() {
        val openedScripts = FileEditorManager.getInstance(project).openFiles.filterNot { it.isNonScript() }
        updateHighlighting(openedScripts)
    }

    init {
        val connection = project.messageBus.connect()
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
            Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()

        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
    }

    val firstScriptSdk: Sdk?
        get() {
            val firstCachedScript = allConfigurations.firstOrNull()?.file ?: return null
            return scriptsSdksCache[firstCachedScript]
        }

    private val cacheLock = ReentrantReadWriteLock()

    val allSdks by ClearableLazyValue(cacheLock) {
        allConfigurations
            .mapNotNull { scriptsSdksCache[it.file] }
            .distinct()
    }

    val allNonIndexedSdks by ClearableLazyValue(cacheLock) {
        allConfigurations
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

        val scriptDependenciesClasspath = allConfigurations
            .flatMap { it.result?.dependenciesClassPath }.distinct()

        sdkFiles + toVfsRoots(scriptDependenciesClasspath)
    }

    val allDependenciesSources by ClearableLazyValue(cacheLock) {
        val sdkSources = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.SOURCES).toList() }

        val scriptDependenciesSources = allConfigurations
            .flatMap { it.result.dependenciesSources }.distinct()
        sdkSources + toVfsRoots(scriptDependenciesSources)
    }

    val allDependenciesClassFilesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    val allDependenciesSourcesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile) = scriptDependenciesClassFilesScope(file)
    override fun getScriptSdk(file: VirtualFile) = scriptSdk(file)

    override fun getFirstScriptsSdk() = firstScriptSdk

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
                return@createWeakMap NonClasspathDirectoriesScope.compose(toVfsRoots(roots))
            }

            return@createWeakMap NonClasspathDirectoriesScope.compose(
                sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() + toVfsRoots(roots)
            )
        }

    private fun scriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope {
        return scriptsDependenciesClasspathScopeCache[file] ?: GlobalSearchScope.EMPTY_SCOPE
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

    fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean {
        val scriptSdk = getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(project)
        val wasSdkChanged = scriptSdk != null && !allSdks.contains(scriptSdk)
        if (wasSdkChanged) {
            debug { "sdk was changed: $compilationConfiguration" }
            return true
        }

        val newClassRoots = toVfsRoots(compilationConfiguration.dependenciesClassPath)
        for (newClassRoot in newClassRoots) {
            if (!allDependenciesClassFiles.contains(newClassRoot)) {
                debug { "class root was changed: $newClassRoot" }
                return true
            }
        }

        val newSourceRoots = toVfsRoots(compilationConfiguration.dependenciesSources)
        for (newSourceRoot in newSourceRoots) {
            if (!allDependenciesSources.contains(newSourceRoot)) {
                debug { "source root was changed: $newSourceRoot" }
                return true
            }
        }
        return false
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