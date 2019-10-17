/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loaders

import org.jetbrains.kotlin.idea.core.script.configuration.ScriptConfigurationManagerImpl
import org.jetbrains.kotlin.idea.highlighter.OutsidersPsiFileSupportUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import kotlin.script.experimental.api.asSuccess

class OutsiderFileDependenciesLoader(private val manager: ScriptConfigurationManagerImpl) :
    ScriptDependenciesLoader {
    override val cache: Boolean
        get() = false

    override val skipNotification: Boolean
        get() = true

    override fun loadDependencies(
        firstLoad: Boolean,
        file: KtFile,
        scriptDefinition: ScriptDefinition
    ): ScriptCompilationConfigurationResult? {
        val project = file.project
        val virtualFile = file.virtualFile ?: return null
        val fileOrigin = OutsidersPsiFileSupportUtils.getOutsiderFileOrigin(project, virtualFile) ?: return null

        return manager.getCachedConfiguration(fileOrigin)?.asSuccess()
    }
}