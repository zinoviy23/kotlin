/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder

sealed class Field {
    abstract val name: String
    abstract val type: String
    abstract val nullable: Boolean
    abstract val withReplace: Boolean
}

// ----------- Simple field -----------

data class SimpleField(
    override val name: String,
    override val type: String,
    override val nullable: Boolean,
    override val withReplace: Boolean
) : Field()

fun field(name: String, type: String, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(name, type, nullable, withReplace)
}

fun field(name: String, type: Type, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(name, type.type, nullable, withReplace)
}

fun field(type: Type, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(type.type.decapitalize(), type.type, nullable, withReplace)
}

fun booleanField(name: String): Field {
    return field(name, AbstractFirTreeBuilder.boolean)
}

fun stringField(name: String): Field {
    return field(name, AbstractFirTreeBuilder.string)
}

// ----------- Fir field -----------

data class FirField(
    override val name: String,
    val element: Element,
    override val nullable: Boolean,
    override val withReplace: Boolean
) : Field() {
    override val type: String = element.type + if (nullable) "?" else ""
}

fun field(name: String, element: Element, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return FirField(name, element, nullable, withReplace)
}

fun field(element: Element, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return FirField(element.name.decapitalize(), element, nullable, withReplace)
}

// ----------- Field list -----------

data class FieldList(
    override val name: String,
    val baseType: String,
    override val withReplace: Boolean
) : Field() {
    override val type: String = "List<$baseType>"

    override val nullable: Boolean
        get() = false
}

fun fieldList(name: String, element: Element, withReplace: Boolean = false): Field {
    return FieldList(name, element.type, withReplace)
}

fun fieldList(element: Element, withReplace: Boolean = false): Field {
    return FieldList(element.name.decapitalize() + "s", element.type, withReplace)
}

// ----------- Element -----------

class Element(val name: String) {
    val fields = mutableSetOf<Field>()
    val type: String = "Fir$name"
    val parents = mutableListOf<Element>()
    var implementation: Implementation? = null

    val allFields: List<Field> by lazy {
        val result = LinkedHashSet<Field>()
        parents.forEach {
            result.addAll(it.allFields)
        }
        result.addAll(fields)
        result.toList()
    }

    val allFirFields: List<Field> by lazy {
        allFields.filter { it is FirField || it is FieldList}
    }

    val allSimpleFields: List<SimpleField> by lazy {
        allFields.filterIsInstance<SimpleField>()
    }
}

class Type(val type: String)

// ----------- Field set -----------

class FieldSet(val fields: MutableList<Field>)

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

// ----------- Implementation -----------

class Implementation(val element: Element, name: String?) {
    init {
        element.implementation = this
    }

    val type = name ?: element.type + "Impl"

    val separateTransformations = mutableListOf<Field>()
}