/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.*

abstract class AbstractFirTreeImplementationConfigurator(private val treeBuilder: AbstractFirTreeBuilder) {
    private val elementsWithImpl = mutableSetOf<Element>()

    fun impl(element: Element, name: String? = null, init: ImplementationContext.() -> Unit = {}) {
        val implementation = if (name == null) {
            element.defaultImplementation
        } else {
            element.customImplementations.firstOrNull { it.name == name }
        } ?: Implementation(element, name)
        val context = ImplementationContext(implementation)
        context.apply(init)
        elementsWithImpl += element
    }

    fun generateDefaultImplementations(builder: AbstractFirTreeBuilder) {
        collectLeafsWithoutImplementation(builder).forEach {
            impl(it)
        }
    }

    private fun collectLeafsWithoutImplementation(builder: AbstractFirTreeBuilder): Set<Element> {
        val elements = builder.elements.toMutableSet()
        builder.elements.forEach {
            elements.removeAll(it.parents)
        }
        elements.removeAll(elementsWithImpl)
        return elements
    }

    inner class ImplementationContext(private val implementation: Implementation) {
        fun sep(vararg names: String) {
            val fields = names.map { name ->
                getField(name).also { require(it is FirField || it is FieldList) }
            }.toTypedArray()
            sep(*fields)
        }

        fun sep(vararg fields: Field) {
            implementation.separateTransformations += fields
        }


        private fun getField(name: String): Field {
            return implementation.element.allFields.first { it.name == name }
        }
    }
}