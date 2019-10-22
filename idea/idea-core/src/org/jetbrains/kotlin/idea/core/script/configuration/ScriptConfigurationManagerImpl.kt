/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptCompositeCache
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrNull

internal class ScriptConfigurationManagerImpl(project: Project) : AbstractScriptConfigurationManager(
    project,
    ScriptCompositeCache(project)
) {
    private val loader = FromRefinedConfigurationLoader()

    private val backgroundExecutor = BackgroundExecutor(project, rootsManager)
    private val listener = ScriptsListener(project, this)

    override fun reloadConfigurationInTransaction(
        file: KtFile,
        isFirstLoad: Boolean,
        loadEvenWillNotBeApplied: Boolean,
        /* Test only */ forceSync: Boolean
    ): ScriptCompilationConfigurationResult? {
        val autoReloadEnabled = KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled
        val shouldLoad = isFirstLoad || loadEvenWillNotBeApplied || autoReloadEnabled
        if (!shouldLoad) return null

        val virtualFile = file.originalFile.virtualFile ?: return null

        // todo: who will initiate loading of scripts configuration when definition manager will be ready?
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return null
        val scriptDefinition = file.findScriptDefinition() ?: return null

        return if (loader.isAsync(scriptDefinition) && !forceSync) {
            backgroundExecutor.ensureScheduled(virtualFile) {
                doReloadConfiguration(virtualFile, file, scriptDefinition)
            }
            null
        } else doReloadConfiguration(virtualFile, file, scriptDefinition)
    }

    private fun doReloadConfiguration(
        virtualFile: VirtualFile,
        file: KtFile,
        scriptDefinition: ScriptDefinition
    ) = loader.loadDependencies(file, scriptDefinition)
        ?.also { saveConfiguration(virtualFile, it, false) }

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
     * Save [newResult] for [file] into caches and update highlight.
     * Should be called inside `rootsManager.transaction { ... }`.
     *
     * @param skipNotification forces loading new configuration even if auto reload is disabled.
     *
     * @sample ScriptConfigurationManager.getConfiguration
     */
    private fun saveConfiguration(
        file: VirtualFile,
        newResult: ScriptCompilationConfigurationResult,
        skipNotification: Boolean
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
                    saveChangedConfiguration(file, newConfiguration)
                } else {
                    debug(file) {
                        "configuration changed, notification is shown: old = $oldConfiguration, new = $newConfiguration"
                    }
                    file.addScriptDependenciesNotificationPanel(
                        newConfiguration, project,
                        onClick = {
                            file.removeScriptDependenciesNotificationPanel(project)
                            rootsManager.transaction {
                                saveChangedConfiguration(file, it)
                            }
                        }
                    )
                }
            }
        }
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
}