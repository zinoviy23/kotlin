/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager.Companion.toVfsRoots
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

abstract class AbstractScriptConfigurationManager : ScriptConfigurationManager {
    protected abstract val project: Project
    protected abstract val cache: ScriptConfigurationCache
    protected val allScripts = AllScriptsConfigurationImpl(object : InternalScriptConfigurationsProvider {
        override val project: Project
            get() = this@AbstractScriptConfigurationManager.project

        override fun getConfiguration(virtualFile: VirtualFile): ScriptCompilationConfigurationWrapper? {
            return this@AbstractScriptConfigurationManager.getCachedConfiguration(virtualFile)
        }

        override val allConfigurations: Collection<CachedConfiguration>
            get() = cache.all()
    })

    @Deprecated("Use getScriptClasspath(KtFile) instead")
    override fun getScriptClasspath(file: VirtualFile): List<VirtualFile> {
        val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return emptyList()
        return getScriptClasspath(ktFile)
    }

    override fun getScriptClasspath(file: KtFile): List<VirtualFile> =
        toVfsRoots(getConfiguration(file)?.dependenciesClassPath.orEmpty())

    fun getCachedConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? =
        cache[file]?.result

    fun isConfigurationUpToDate(file: VirtualFile): Boolean {
        return cache[file]?.isUpToDate == true
    }

    /**
     * Check if configuration is already cached for [file] (in cache or FileAttributes).
     * Don't check if file was changed after the last update.
     * Supposed to be used to switch highlighting off for scripts without configuration.
     * to avoid all file being highlighted in red.
     */
    override fun isConfigurationCached(file: KtFile): Boolean {
        return getCachedConfiguration(file.originalFile.virtualFile) != null
    }

    override fun getScriptSdk(file: VirtualFile): Sdk? = allScripts.getScriptSdk(file)
    override fun getFirstScriptsSdk(): Sdk? = allScripts.getFirstScriptsSdk()
    override fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        allScripts.getScriptDependenciesClassFilesScope(file)

    override fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope =
        allScripts.getAllScriptsDependenciesClassFilesScope()

    override fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope =
        allScripts.getAllScriptDependenciesSourcesScope()

    override fun getAllScriptsDependenciesClassFiles(): List<VirtualFile> =
        allScripts.getAllScriptsDependenciesClassFiles()

    override fun getAllScriptDependenciesSources(): List<VirtualFile> =
        allScripts.getAllScriptDependenciesSources()

    protected abstract fun getConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile? = null
    ): ScriptCompilationConfigurationWrapper?

    override fun getConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper? {
        return getConfiguration(file.virtualFile, file)
    }
}