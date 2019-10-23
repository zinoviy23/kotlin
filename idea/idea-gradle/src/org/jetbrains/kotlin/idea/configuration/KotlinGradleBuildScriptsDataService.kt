/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.NotificationSource
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.dependencies.ScriptDependencies

class KotlinGradleBuildScriptsDataService : AbstractProjectDataService<GradleSourceSetData, Void>() {
    override fun getTargetDataKey(): Key<GradleSourceSetData> = GradleSourceSetData.KEY

    override fun onSuccessImport(
        imported: MutableCollection<DataNode<GradleSourceSetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        super.onSuccessImport(imported, projectData, project, modelsProvider)

        val projectDataNode = imported.firstNotNullResult { ExternalSystemApiUtil.findParent(it, ProjectKeys.PROJECT) } ?: return

        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        val projectSettings = gradleSettings.getLinkedProjectSettings(projectData?.linkedExternalProjectPath ?: return) ?: return
        val gradleExeSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
            project,
            projectSettings.externalProjectPath,
            GradleConstants.SYSTEM_ID
        )
        val javaHome = File(gradleExeSettings.javaHome ?: return)

        val files = mutableListOf<Pair<VirtualFile, ScriptCompilationConfigurationResult>>()

        val home = "/Users/sergey.rostov/.gradle/wrapper/dists/gradle-6.0-20191001230020+0000-bin/6flzaqm7aakvjqwzpzw1mpxsw/gradle-6.0-20191001230020+0000/lib"
        projectDataNode.gradleKotlinBuildScripts?.forEach { buildScript ->
            val scriptFile = File(buildScript.file)
            val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

            files.add(
                Pair(
                    virtualFile,
                    ResultWithDiagnostics.Success(
                        ScriptCompilationConfigurationWrapper.FromLegacy(
                            VirtualFileScriptSource(virtualFile),
                            ScriptDependencies(
                                javaHome = javaHome,
                                classpath = (listOf(
                                    "$home/gradle-core-6.0.jar",
                                    "$home/gradle-kotlin-dsl-tooling-models-6.0.jar",
                                    "$home/gradle-kotlin-dsl-6.0.jar",
                                    "$home/gradle-core-api-6.0.jar"
                                ) + buildScript.classPath).map { File(it) },
                                sources = buildScript.sourcePath.map { File(it) },
                                imports = buildScript.imports
                            ),
                            virtualFile.findScriptDefinition(project)
                        )
                    )
                )
            )

            buildScript.messages.forEach {
                addBuildScriptDiagnosticMessage(it, virtualFile, project)
            }
        }

        project.service<ScriptConfigurationManager>().saveCompilationConfigurationAfterImport(files)
    }

    private fun addBuildScriptDiagnosticMessage(
        it: GradleKotlinBuildScriptData.Message,
        virtualFile: VirtualFile,
        project: Project
    ) {
        val notification =
            NotificationData(
                "Kotlin Build Script",
                it.text,
                when (it.severity) {
                    GradleKotlinBuildScriptData.Severity.WARNING -> NotificationCategory.WARNING
                    GradleKotlinBuildScriptData.Severity.ERROR -> NotificationCategory.ERROR
                },
                NotificationSource.PROJECT_SYNC
            )

        notification.navigatable = LazyNavigatable(
            virtualFile,
            project,
            it.position
        )

        ExternalSystemNotificationManager.getInstance(project).showNotification(
            GradleConstants.SYSTEM_ID,
            notification
        )
    }

    class LazyNavigatable internal constructor(
        private val virtualFile: VirtualFile,
        private val project: Project,
        val position: GradleKotlinBuildScriptData.Position?
    ) : Navigatable {
        private val openFileDescriptor: Navigatable by lazy {
            if (position != null) OpenFileDescriptor(project, virtualFile, position.line, position.column)
            else OpenFileDescriptor(project, virtualFile, -1)
        }

        override fun navigate(requestFocus: Boolean) {
            if (openFileDescriptor.canNavigate()) openFileDescriptor.navigate(requestFocus)
        }

        override fun canNavigate(): Boolean = virtualFile.exists()

        override fun canNavigateToSource(): Boolean = canNavigate()
    }
}