/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.Element
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.Field
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSet
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.booleanField

abstract class AbstractFieldConfigurator {
    infix fun Element.has(fieldSet: FieldSet) {
        fields.addAll(fieldSet.fields)
    }

    infix fun Element.has(field: Field) {
        fields.add(field)
    }

    inner class ConfigureContext(val element: Element) {
        operator fun FieldSet.unaryPlus() {
            element has this
        }

        operator fun Field.unaryPlus() {
            element has this
        }

        fun generateBooleanFields(vararg names: String) {
            names.forEach {
                +booleanField("is${it.capitalize()}")
            }
        }
    }

    inline fun Element.configure(block: ConfigureContext.() -> Unit) {
        ConfigureContext(this).block()
    }
}