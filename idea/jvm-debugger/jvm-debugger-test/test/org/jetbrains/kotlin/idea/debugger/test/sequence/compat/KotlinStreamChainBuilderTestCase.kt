/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.sequence.compat

import com.intellij.debugger.streams.test.StreamChainBuilderTestCase
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

// BUNCH: 192
@Suppress("IncompatibleAPI")
abstract class KotlinStreamChainBuilderTestCase : StreamChainBuilderTestCase() {
    val project_: Project get() = project
    val module_: Module get() = module
}