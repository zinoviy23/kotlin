/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.Element
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.Type

abstract class AbstractFirTreeBuilder {
    companion object {
        val baseFirElement = Element("Element")

        const val string = "String"
        const val boolean = "Boolean"
    }

    val elements = mutableListOf(baseFirElement)
    val types = mutableListOf<Type>()

    fun type(type: String): Type = Type(
        type
    ).also { types += it }

    fun element(name: String, vararg dependencies: Element, init: Element.() -> Unit = {}): Element =
        Element(name).apply(init).also {
            if (dependencies.isEmpty()) {
                it.parents.add(baseFirElement)
            }
            it.parents.addAll(dependencies)
            elements += it
        }
}