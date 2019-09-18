/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

sealed class Field {
    abstract val name: String
    abstract val type: String
    abstract val nullable: Boolean
}

data class SimpleField(override val name: String, override val type: String, override val nullable: Boolean) : Field() {
    constructor(name: String, type: Type, nullable: Boolean) : this(name, type.type, nullable)
    constructor(type: Type, nullable: Boolean) : this(type.type.decapitalize(), type.type, nullable)
}

data class FieldList(override val name: String, val baseType: String) : Field() {
    constructor(name: String, element: Element) : this(name, element.type)
    constructor(baseType: String) : this(baseType.decapitalize() + "s", baseType)
    constructor(base: Element) : this(base.name.decapitalize() + "s", base.type)

    override val type: String
        get() = "List<$baseType>"

    override val nullable: Boolean
        get() = false
}

data class FirField(override val name: String, val element: Element, override val nullable: Boolean = false) : Field() {
    constructor(element: Element, nullable: Boolean) : this(element.name.decapitalize(), element, nullable)

    override val type: String = element.type + if (nullable) "?" else ""
}

class Element(val name: String) {
    val fields = mutableSetOf<Field>()
    val type: String = "Fir$name"
    val parents = mutableListOf<Element>()

    val allFields: List<Field> by lazy {
        val result = LinkedHashSet<Field>()
        parents.forEach {
            result.addAll(it.allFields)
        }
        result.addAll(fields)
        result.toList()
    }
}

class Type(val type: String)

class FieldSet(val fields: MutableList<Field>)

