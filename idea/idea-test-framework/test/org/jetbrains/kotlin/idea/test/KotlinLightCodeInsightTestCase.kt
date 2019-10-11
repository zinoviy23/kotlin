/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightCodeInsightTestCase

@Suppress("IncompatibleAPI")
@Deprecated("Use KotlinLightCodeInsightFixtureTestCase instead")
abstract class KotlinLightCodeInsightTestCase : LightCodeInsightTestCase() {
    val project_: Project get() = project
    val module_: Module get() = module
}