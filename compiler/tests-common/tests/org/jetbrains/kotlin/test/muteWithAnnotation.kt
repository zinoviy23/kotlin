/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import junit.framework.TestCase

enum class Platform {
    IJ193
}

annotation class Mute(vararg val platform: Platform)

@Throws(Exception::class)
fun testWithMuteAnnotation(test: () -> Unit, testCase: TestCase): () -> Unit {
    val muteAnnotation = testCase.javaClass.getMethod(testCase.name).getAnnotation(Mute::class.java) ?: return test

    return {
        System.err.println("IGNORED TEST: ${testCase::class.java.name}.${testCase.name}")
    }
}