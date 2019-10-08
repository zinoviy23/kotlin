/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ModuleRootModificationUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.junit.Ignore

class ScriptSdkTest : AbstractScriptConfigurationTest() {

    @Ignore("KT-34233")
    fun testScriptConflictingSdk() {
        configureScriptFile("$testDataPath/conflictingSdk")

        runWriteAction {
            // The first sdk from ProjectJdkTable will be used for script
            val scriptSdk = PluginTestCaseBase.addJdk(testRootDisposable) { PluginTestCaseBase.fullJdk() }
            ProjectJdkTable.getInstance().addJdk(scriptSdk, testRootDisposable)

            val moduleSdk = JavaSdk.getInstance().createJdk("Module SDK", scriptSdk.homePath!!, true)
            ProjectJdkTable.getInstance().addJdk(moduleSdk, testRootDisposable)

            myModule = createTestModuleByName("myModule")
            ModuleRootModificationUtil.updateModel(myModule) { model ->
                model.sdk = moduleSdk
            }
        }

        checkHighlighting(editor, true, true)
    }

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/script/sdk"
    }
}