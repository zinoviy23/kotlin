/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.ElementsBuilder.Companion.boolean
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.ElementsBuilder.Companion.string

// ----------- Simple field -----------

fun Element.field(name: String, type: String, nullable: Boolean = false) {
    fields += SimpleField(name, type, nullable)
}

fun Element.field(name: String, type: Type, nullable: Boolean = false) {
    fields += SimpleField(name, type, nullable)
}

fun Element.field(type: Type, nullable: Boolean = false) {
    fields += SimpleField(type, nullable)
}

fun field(name: String, type: String, nullable: Boolean = false): Field {
    return SimpleField(name, type, nullable)
}

fun field(name: String, type: Type, nullable: Boolean = false): Field {
    return SimpleField(name, type, nullable)
}

fun field(type: Type, nullable: Boolean = false): Field {
    return SimpleField(type, nullable)
}

// ----------- Fir field -----------

fun booleanField(name: String): Field {
    return field(name, boolean)
}

fun stringField(name: String): Field {
    return field(name, string)
}

fun Element.field(name: String, element: Element, nullable: Boolean = false) {
    fields += FirField(name, element, nullable)
}

fun Element.field(element: Element, nullable: Boolean = false) {
    fields += FirField(element, nullable)
}

fun field(name: String, element: Element, nullable: Boolean = false): Field {
    return FirField(name, element, nullable)
}

fun field(element: Element, nullable: Boolean = false): Field {
    return FirField(element, nullable)
}

// ----------- Field list -----------

fun Element.fieldList(name: String, element: Element) {
    fields += FieldList(name, element)
}

fun Element.fieldList(element: Element) {
    fields += FieldList(element)
}

fun fieldList(name: String, element: Element): Field {
    return FieldList(name, element)
}

fun fieldList(element: Element): Field {
    return FieldList(element)
}

// --------------------------------

class ElementsBuilder {
    companion object {
        val baseFirElement = Element("Element")

        const val string = "String"
        const val boolean = "Boolean"
    }

    private val _elements = mutableListOf(baseFirElement)
    private val _types = mutableListOf<Type>()

    val elements: List<Element> get() = _elements
    val types: List<Type> get() = _types

    fun type(type: String): Type = Type(type).also { _types += it }

    fun element(name: String, vararg dependencies: Element, init: Element.() -> Unit = {}): Element =
        Element(name).apply(init).also {
            it.parents.add(baseFirElement)
            it.parents.addAll(dependencies)
            _elements += it
        }

    fun fieldSet(vararg fields: Field): FieldSet {
        return FieldSet(fields.toMutableList())
    }

    infix fun FieldSet.with(sets: List<FieldSet>): FieldSet {
        sets.forEach {
            fields += it.fields
        }
        return this
    }

    infix fun FieldSet.with(set: FieldSet): FieldSet {
        fields += set.fields
        return this
    }

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

fun buildElements(init: ElementsBuilder.() -> Unit): ElementsBuilder = ElementsBuilder().apply(init)