/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeImplementationConfigurator

object ImplementationConfigurator : AbstractFirTreeImplementationConfigurator(FirTreeBuilder) {
    fun configureImplementations() {
        configure()
        generateDefaultImplementations(FirTreeBuilder)
    }

    private fun configure() = with(FirTreeBuilder) {
        impl(whenExpression) {
            sep("branches", "subject")
        }
    }
}