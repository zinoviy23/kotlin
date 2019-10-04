/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.protoMimic

sealed class Proto(val name: String, val directives: List<String>, val nested: List<Proto>) {
    class Message(name: String, val fields: List<MessageEntry>, directives: List<String>, nested: List<Proto>) : Proto(name, directives, nested)

    class Enum(name: String, val entries: List<EnumEntry>, directives: List<String>, nested: List<Proto>) : Proto(name, directives, nested)
}

class EnumEntry(
    val index: Int,
    val name: String
)

enum class FieldKind {
    REQUIRED,
    OPTIONAL,
    REPEATED,
    ONE_OF
}

sealed class MessageEntry(val name: String) {
    class Field(
        val kind: FieldKind,
        name: String,
        val index: Int,
        val type: String,
        val directives: List<String> = emptyList(),
        val defaultValue: String? = null
    ): MessageEntry(name)

    class OneOf(
        name: String,
        val fields: List<Field>
    ): MessageEntry(name)
}

fun printProto(p: Proto, indent: String = "") {
    when (p) {
        is Proto.Message -> printMessage(p, indent)
        is Proto.Enum -> printEnum(p, indent)
    }
    println()
}

fun printMessage(message: Proto.Message, indent: String) {
    println("${indent}message ${message.name} {")

    for (p in message.nested) {
        printProto(p, indent + "    ")
        println()
    }

    for (f in message.fields) {
        when (f) {
            is MessageEntry.OneOf -> {
                println("${indent}  oneof ${f.name} {")
                for (f2 in f.fields) {
                    println("    ${f2.type} ${f2.name} = ${f2.index};")
                }
                println("${indent}  }")
            }
            is MessageEntry.Field -> {
                print("${indent}  ${f.kind.toString().toLowerCase()} ${f.type} ${f.name} = ${f.index}")
                if (f.defaultValue != null) {
                    print(" [default = ${f.defaultValue}]")
                }
                if (f.directives.isNotEmpty()) {
                    print(" /*${f.directives.fold("") { acc, c -> acc + " " + c}} */")
                }
                println(";")
            }
        }
    }
    println("${indent}}")
}

fun printEnum(enum: Proto.Enum, indent: String) {
    println("${indent}enum ${enum.name} {")

    for (p in enum.nested) {
        printProto(p, indent + "    ")
        println()
    }

    for (e in enum.entries) {
        println("${indent}  ${e.name} = ${e.index};")
    }
    println("${indent}}")
}