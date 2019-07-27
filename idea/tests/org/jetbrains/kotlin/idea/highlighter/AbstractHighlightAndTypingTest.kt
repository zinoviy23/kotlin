/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.codeInsight.daemon.ProblemHighlightFilter
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPassFactory
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.lang.ExternalAnnotatorsFilter
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.StdLanguages
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.xml.XmlFileNSInfoProvider
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.UIUtil
import com.intellij.xml.XmlSchemaProvider
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.completion.test.ExpectedCompletionUtils
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionHandlerTest.Companion.COMPLETION_CHARS_PREFIX
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionHandlerTest.Companion.COMPLETION_CHAR_PREFIX
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionHandlerTest.Companion.ELEMENT_TEXT_PREFIX
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionHandlerTest.Companion.INVOCATION_COUNT_PREFIX
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionHandlerTest.Companion.LOOKUP_STRING_PREFIX
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionHandlerTest.Companion.TAIL_TEXT_PREFIX
import org.jetbrains.kotlin.idea.completion.test.handlers.CompletionHandlerTestBase.Companion.completionChars
import org.jetbrains.kotlin.idea.completion.test.handlers.CompletionHandlerTestBase.Companion.doTestWithTextLoaded
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.highlighter.AbstractHighlightingTest.*
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TagsTestDataUtil

import java.io.File

abstract class AbstractHighlightAndTypingTest : KotlinLightCodeInsightFixtureTestCase() {

    private val defaultCompletionType: CompletionType = CompletionType.BASIC

    protected fun doTest(filePath: String) {
        val fileText = FileUtil.loadFile(File(filePath), true)
        val checkInfos = !InTextDirectivesUtils.isDirectiveDefined(fileText, NO_CHECK_INFOS_PREFIX)
        val checkWeakWarnings = !InTextDirectivesUtils.isDirectiveDefined(fileText, NO_CHECK_WEAK_WARNINGS_PREFIX)
        val checkWarnings = !InTextDirectivesUtils.isDirectiveDefined(fileText, NO_CHECK_WARNINGS_PREFIX)
        val expectedDuplicatedHighlighting = InTextDirectivesUtils.isDirectiveDefined(fileText, EXPECTED_DUPLICATED_HIGHLIGHTING_PREFIX)

        val invocationCount = InTextDirectivesUtils.getPrefixedInt(fileText, INVOCATION_COUNT_PREFIX) ?: 1
        val lookupString = InTextDirectivesUtils.findStringWithPrefixes(fileText, LOOKUP_STRING_PREFIX)
        val itemText = InTextDirectivesUtils.findStringWithPrefixes(fileText, ELEMENT_TEXT_PREFIX)
        val tailText = InTextDirectivesUtils.findStringWithPrefixes(fileText, TAIL_TEXT_PREFIX)
        val actions = InTextDirectivesUtils.findListWithPrefixes(fileText, ACTIONS_PREFIX) ?: emptyList()
        val completionType = ExpectedCompletionUtils.getCompletionType(fileText) ?: defaultCompletionType
        val inspections = InTextDirectivesUtils.findListWithPrefixes(fileText, INSPECTIONS_PREFIX) ?: emptyList()
        val completionChars = completionChars(fileText)

        val file = myFixture.configureByFile(filePath)
        myFixture.allowTreeAccessForAllFiles()

        // check initial highlight
        checkHighlighting(file, inspections, expectedDuplicatedHighlighting, checkWarnings, checkInfos, checkWeakWarnings)

        val highlightStrings = mutableListOf<String>()
        // type and check plain result
        doTestWithTextLoaded(
            myFixture,
            completionType,
            invocationCount,
            lookupString,
            itemText,
            tailText,
            completionChars,
            File(filePath).name + ".after",
            actions
        ) {
            highlightStrings.add(highlightString(file, inspections))
        }

        // form a highlight string (as it is quite inconvenient to reload file and track actual highlight positions)
        val highlightString = highlightStrings.last()

        val expectedHighlightString = FileUtil.loadFile(File("$filePath.expected"), true)
        assertEquals(expectedHighlightString, highlightString)
    }

    private fun checkHighlighting(
        file: PsiFile,
        inspections: List<String>,
        expectedDuplicatedHighlighting: Boolean,
        checkWarnings: Boolean,
        checkInfos: Boolean,
        checkWeakWarnings: Boolean
    ) {
        withExpectedDuplicatedHighlighting(expectedDuplicatedHighlighting, Runnable {
            try {
                doWithAnnotations(inspections) {
                    myFixture.checkHighlighting(checkWarnings, checkInfos, checkWeakWarnings)
                }
            } catch (e: Throwable) {
                reportActualHighlighting(file, inspections, e)
            }
        })
    }

    private fun reportActualHighlighting(file: PsiFile, inspections: List<String>, e: Throwable) {
        println(highlightString(file, inspections))
        throw e
    }

    private fun doWithAnnotations(inspections: List<String> = emptyList(), block: () -> Unit) {
        val disposable = Disposer.newDisposable("highlightString")

        if (inspections.isNotEmpty()) {
            commitAllDocuments()

            enableInspections(
                project,
                disposable,
                enabledInspections = inspections
            )
        }

        try {
            IdentifierHighlighterPassFactory.doWithHighlightingEnabled {
                block()
            }
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun highlightString(file: PsiFile, inspections: List<String>): String {
        commitAllDocuments()

        val hardRefToFileElement = (file as PsiFileImpl).calcTreeElement()
        assertNotNull("to load text", hardRefToFileElement)

        var highlights: List<HighlightInfo>? = null
        doWithAnnotations(inspections) {
            highlights = highlights()
        }

        return TagsTestDataUtil.insertInfoTagsWithCaretAndSelection(highlights, myFixture.editor)
    }

    private fun commitAllDocuments() =
        runInEdtAndWait { PsiDocumentManager.getInstance(project).commitAllDocuments() }

    private fun highlights(): List<HighlightInfo> =
        myFixture.doHighlighting()

    private fun withExpectedDuplicatedHighlighting(expectedDuplicatedHighlighting: Boolean, runnable: Runnable) {
        if (!expectedDuplicatedHighlighting) {
            runnable.run()
            return
        }

        expectedDuplicatedHighlighting(runnable)
    }

    companion object {
        const val INSPECTIONS_PREFIX = "INSPECTIONS:"
        const val ACTIONS_PREFIX = "ACTIONS:"

        fun disableInspection(project: Project, profile: InspectionProfileImpl, shortId: String) {
            val tools = profile.getToolsOrNull(shortId, project)
            if (tools != null) {
                tools.isEnabled = false
            }
        }

        fun enableInspections(
            project: Project,
            disposable: Disposable,
            enabledInspections: List<String>,
            disableInspections: List<String> = emptyList()
        ) {
            // based MadTestingUtil.enableAllInspections(project, disposable)

            // TODO: add support of ALL and NONE inspections

            val manager = InspectionProjectProfileManager.getInstance(project) as ProjectInspectionProfileManager
            InspectionProfileImpl.INIT_INSPECTIONS = true
            val profile = InspectionProfileImpl("Default Highlight")
            for (enabledInspection in enabledInspections) {
                profile.enableTool(enabledInspection, project)
            }

            disableInspection(project, profile, "HighlightVisitorInternal")
            for (disableInspection in disableInspections) {
                disableInspection(project, profile, disableInspection)
            }

            manager.addProfile(profile)
            val prev = manager.currentProfile
            manager.setCurrentProfile(profile)
            Disposer.register(disposable, Disposable {
                InspectionProfileImpl.INIT_INSPECTIONS = false
                manager.setCurrentProfile(prev)
                manager.deleteProfile(profile)
            })
        }

        fun enableAnnotatorsAndLoadDefinitions(project: Project) {
            InjectedLanguageManager.getInstance(project) // zillion of Dom Sem classes
            LanguageAnnotators.INSTANCE.allForLanguage(JavaLanguage.INSTANCE) // pile of annotator classes loads
            LanguageAnnotators.INSTANCE.allForLanguage(StdLanguages.XML)
            LanguageAnnotators.INSTANCE.allForLanguage(KotlinLanguage.INSTANCE)
            DaemonAnalyzerTestCase.assertTrue(
                "side effect: to load extensions",
                ProblemHighlightFilter.EP_NAME.extensions.toMutableList()
                    .plus(ImplicitUsageProvider.EP_NAME.extensions)
                    .plus(XmlSchemaProvider.EP_NAME.extensions)
                    .plus(XmlFileNSInfoProvider.EP_NAME.extensions)
                    .plus(ExternalAnnotatorsFilter.EXTENSION_POINT_NAME.extensions)
                    .plus(IndexPatternBuilder.EP_NAME.extensions).isNotEmpty()
            )
            // side effect: to load script definitions"
            val scriptDefinitionsManager = ScriptDefinitionsManager.getInstance(project)
            scriptDefinitionsManager.getAllDefinitions()
            runInEdtAndWait {
                UIUtil.dispatchAllInvocationEvents()
            }

            assertTrue(scriptDefinitionsManager.isReady())
            assertFalse(KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled)
        }

    }

}
