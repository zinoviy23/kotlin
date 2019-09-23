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

    var defaultValue: String? = null
    var isVal: Boolean = false
    var withGetter: Boolean = false

    fun copy(): Field = internalCopy().also {
        it.defaultValue = defaultValue
        it.isVal = isVal
        it.withGetter = withGetter
    }

    protected abstract fun internalCopy(): Field

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        other as Field
        return name != other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

// ----------- Simple field -----------

class SimpleField(
    override val name: String,
    type: String,
    override val nullable: Boolean,
    override val withReplace: Boolean
) : Field() {
    override val type: String = type + (if (nullable) "?" else "")

    override fun internalCopy(): Field {
        val type = if (nullable) type.dropLast(1) else type
        return SimpleField(name, type, nullable, withReplace)
    }
}

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

    override fun internalCopy(): Field {
        return FirField(name, element, nullable, withReplace)
    }
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
    init {
        defaultValue = "mutableListOf()"
    }

    override val type: String = "List<$baseType>"

    override val nullable: Boolean
        get() = false

    override fun internalCopy(): Field {
        return FieldList(name, baseType, withReplace)
    }
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
    var defaultImplementation: Implementation? = null
    val customImplementations = mutableListOf<Implementation>()

    val allImplementations: List<Implementation> by lazy {
        val implementations = customImplementations.toMutableList()
        defaultImplementation?.let { implementations += it }
        implementations
    }

    val allFields: List<Field> by lazy {
        val result = LinkedHashSet<Field>()
        result.addAll(fields.toList().asReversed())
        parents.forEach {
            result.addAll(it.allFields.map { it.copy() }.asReversed())
        }
        result.toList().asReversed()
    }

    val allFirFields: List<Field> by lazy {
        allFields.filter { it is FirField || it is FieldList}
    }

    val allSimpleFields: List<SimpleField> by lazy {
        allFields.filterIsInstance<SimpleField>()
    }

    override fun toString(): String {
        return type
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

class Implementation(val element: Element, val name: String?) {
    val isDefault = name == null
    val type = name ?: element.type + "Impl"

    init {
        if (isDefault) {
            element.defaultImplementation = this
        } else {
            element.customImplementations += this
        }
    }

    val separateTransformations = mutableListOf<Field>()
}