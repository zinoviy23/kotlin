/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.Mute
import org.jetbrains.kotlin.test.Platform
import org.jetbrains.kotlin.test.Platform.IJ193
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class NavigateFromLibrarySourcesTest: AbstractNavigateFromLibrarySourcesTest() {
    @Mute(IJ193)
    fun testJdkClass() {
        checkNavigationFromLibrarySource("Thread", "java.lang.Thread")
    }

    @Mute(IJ193)
    fun testOurKotlinClass() {
        checkNavigationFromLibrarySource("Foo", "a.Foo")
    }

    @Mute(IJ193)
    fun testBuiltinClass() {
        checkNavigationFromLibrarySource("String", "kotlin.String")
    }

    // This test is not exactly for navigation, but separating it to another class doesn't worth it.
    @Mute(IJ193)
    fun testLightClassForLibrarySource() {
        KotlinTestUtils.runTest(this) {
            val navigationElement = navigationElementForReferenceInLibrarySource("usage.kt", "Foo")
            assertTrue(navigationElement is KtClassOrObject, "Foo should navigate to JetClassOrObject")
            val lightClass = navigationElement.toLightClass()
            assertTrue(
                lightClass is KtLightClassForDecompiledDeclaration,
                "Light classes for decompiled declaration should be provided for library source"
            )
            assertEquals("Foo", lightClass.name)
        }
    }

    private fun checkNavigationFromLibrarySource(referenceText: String, targetFqName: String) {
        KotlinTestUtils.runTest(this) {
            checkNavigationElement(navigationElementForReferenceInLibrarySource("usage.kt", referenceText), targetFqName)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return SdkAndMockLibraryProjectDescriptor(
            PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/fromLibSource",
            true,
            true,
            false,
            false
        )
    }

    private fun navigationElementForReferenceInLibrarySource(referenceText: String) =
        navigationElementForReferenceInLibrarySource("usage.kt", referenceText)

    private fun checkNavigationElement(element: PsiElement, expectedFqName: String) {
        when (element) {
            is PsiClass -> {
                assertEquals(expectedFqName, element.qualifiedName)
            }
            is KtClass -> {
                assertEquals(expectedFqName, element.fqName!!.asString())
            }
            else -> {
                fail("Navigation element should be JetClass or PsiClass: " + element::class.java + ", " + element.text)
            }
        }
    }
}
