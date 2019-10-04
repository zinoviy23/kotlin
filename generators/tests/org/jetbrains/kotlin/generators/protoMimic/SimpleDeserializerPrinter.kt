/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.protoMimic

import java.lang.StringBuilder

fun List<Proto>.createSimpleDeserializer(className: String): String {

    val sb = StringBuilder()

    forEach {
        sb.printModel(it)
        sb.appendln()
    }

    sb.appendln("class $className(private val source: ByteArray) {")
    sb.appendln(GUTS)

    val typeMap = this.all().associate { it.name to it }

    val fqnMap = this.buildFqn()

    filterIsInstance<Proto.Message>().forEach { sb.addMessage(it, typeMap, fqnMap) }

    sb.appendln("}")

    return sb.toString()
}

private fun List<Proto>.buildFqn(
    prefix: String = "",
    acc: MutableMap<String, String> = mutableMapOf()
): Map<String, String> {
    forEach {
        acc[it.name] = prefix + it.name
        it.nested.buildFqn(prefix + it.name + ".", acc)
    }

    return acc
}

private fun List<Proto>.all(): List<Proto> = this.flatMap { listOf(it) + it.nested.all() }

private fun StringBuilder.printModel(p: Proto, indent: String = "") {
    when (p) {
        is Proto.Enum -> printModel(p, indent)
        is Proto.Message -> printModel(p, indent)
    }
}

private fun StringBuilder.printModel(e: Proto.Enum, indent: String = "") {
    appendln("${indent}enum class ${e.name} {")
    e.entries.forEachIndexed { index, value ->
        if (index != 0) appendln(",")
        append("${indent}    ${value.name}")
    }
    appendln("${indent};")
    appendln()

    e.nested.forEach {
        printModel(it, indent + "    ")
        appendln()
    }

    appendln("${indent}    companion object {")
    appendln("${indent}        fun fromIndex(index: Int): ${e.name} {")
    appendln("${indent}            return when (index) {")
    e.entries.forEach {
        appendln("${indent}                ${it.index} -> ${it.name}")
    }
    appendln("${indent}                else -> error(\"Unexpected enum value '\$index' for enum '${e.name}'\")")
    appendln("${indent}            }")
    appendln("${indent}        }")
    appendln("${indent}    }")
    appendln("${indent}}")
}

private fun StringBuilder.printModel(m: Proto.Message, indent: String = "") {
    appendln("${indent}class ${m.name}(")

    var firstParameter = true

    fun appendComma() {
        if (!firstParameter) {
            appendln(",")
        }
        firstParameter = false
    }

    val nullableFields = mutableListOf<MessageEntry.Field>()

    fun String.tildify() = if (endsWith('_')) "`${substring(0, length - 1)}`" else this

    fun MessageEntry.Field.appendConstructorParameter() {

        val (name, type) = if (kind == FieldKind.REPEATED) {
            "${name.escape()}List" to "List<${type.ktType()}>"
        } else {
            if (kind == FieldKind.OPTIONAL && defaultValue != null || kind == FieldKind.REQUIRED) {
                name.escape() to type.ktType()
            } else {
                nullableFields += this
                ("field_" + name).escape() to "${type.ktType()}?"
            }
        }

        appendComma()

        append("${indent}    ")
        if (this in nullableFields) append("private ")
        append("val ${name.tildify()}: ${type}")
    }

    m.fields.forEach {
        when (it) {
            is MessageEntry.Field -> it.appendConstructorParameter()
            is MessageEntry.OneOf -> {
                appendComma()
                append("${indent}    val ${it.name.escape()}Case: ${it.name.escape().firstUpper()}Case")

                it.fields.forEach {
                    it.appendConstructorParameter()
                }
            }
        }
    }

    appendln()
    appendln("${indent}) {")

    m.nested.forEach {
        printModel(it, indent + "    ")
        appendln()
    }

    m.fields.filterIsInstance<MessageEntry.OneOf>().forEach {
        appendln("${indent}    enum class ${it.name.escape().firstUpper()}Case {")
        it.fields.forEachIndexed { index, field ->
            if (index != 0) appendln(",")
            append("${indent}        ${field.name.toUpperCase()}")
        }
        appendln(",")
        append("${indent}        ${it.name.toUpperCase()}_NOT_SET")
        appendln()
        appendln("${indent}    }")
    }

    nullableFields.forEach {
        appendln()
        appendln("${indent}    val ${it.name.escape().tildify()}: ${it.type.ktType()}")
        appendln("${indent}        get() = ${("field_" + it.name).escape()}!!")
    }

    m.fields.flatMap {
        when (it) {
            is MessageEntry.Field -> listOf(it)
            is MessageEntry.OneOf -> it.fields
        }
    }.forEach {
        appendln()
        append("${indent}    fun ${("has_" + it.name).escape()}(): Boolean = ")
        if (it !in nullableFields) {
            appendln("true")
        } else {
            appendln("${("field_" + it.name).escape()} != null")
        }

        if (it.kind == FieldKind.REPEATED) {
            appendln()
            appendln("${indent}    val ${it.name.escape()}Count: Int")
            appendln("${indent}        get() = ${it.name.escape()}List.size")
        }
    }


    appendln("${indent}}")
}


private fun String.toReaderInvocation(typeMap: Map<String, Proto>, fqnMap: Map<String, String>): String {
    return typeMap.get(this)?.let {
        when (it) {
            is Proto.Enum -> "${fqnMap[it.name]}.fromIndex(readInt32())"
            is Proto.Message -> "readWithLength { read${it.name}() }"
        }
    } ?: run {
        when (this) {
            "int32" -> "readInt32()"
            "int64", "uint64" -> "readInt64()"
            "string" -> "readString()"
            "bool" -> "readBool()"
            "float" -> "readFloat()"
            "double" -> "readDouble()"
            else -> error("Unknown type: ${this}")
        }
    }
}

private val String.zeroValue: String?
    get() = when (this) {
        "int32" -> "0"
        "int64" -> "0L"
        "uint64" -> "0L"
        "string" -> "\"\""
        "bool" -> "false"
        "float" -> "0.0f"
        "double" -> "0.0"
        else -> null
    }

private fun String.ktType(fqnMap: Map<String, String> = emptyMap()): String = when (this) {
    "int32" -> "Int"
    "int64", "uint64" -> "Long"
    "string" -> "String"
    "bool" -> "Boolean"
    "float" -> "Float"
    "double" -> "Double"
    else -> fqnMap[this] ?: this
}

private fun String.firstLower(): String = this[0].toLowerCase() + substring(1)
private fun String.firstUpper(): String = this[0].toUpperCase() + substring(1)

private fun String.escape(): String {
    val result = this.split('_').fold("") { acc, c ->
        if (c.isEmpty()) acc else {
            if (acc.isEmpty()) {
                c.firstLower()
            } else {
                acc + c.firstUpper()
            }
        }
    }

    return result + (if (result in setOf(
            "super",
            "this",
            "null",
            "break",
            "continue",
            "while",
            "for",
            "return",
            "throw",
            "try",
            "when",
            "hasData",
            "fieldNumber"
        )
    ) "_" else "")
}

fun String.wireType(typeMap: Map<String, Proto>): Int {
    return when (this) {
        "int32", "int64", "uint64", "bool" -> 0
        "float" -> 5
        "double" -> 1
        "string" -> 2
        else -> {
            val p = typeMap[this]!!
            if (p is Proto.Enum) 0 else 2
        }
    }
}

private fun StringBuilder.addMessage(m: Proto.Message, typeMap: Map<String, Proto>, fqnMap: Map<String, String>) {
    val allFields = m.fields.flatMap {
        when (it) {
            is MessageEntry.OneOf -> it.fields
            is MessageEntry.Field -> listOf(it)
        }
    }

    appendln("    fun read${m.name}(): ${m.name.ktType(fqnMap)} {")

    val nullableFields = mutableSetOf<MessageEntry.Field>()

    allFields.forEach { f ->
        val (type, initExpression) = if (f.kind == FieldKind.REPEATED) {
            "MutableList<${f.type.ktType(fqnMap)}>" to "mutableListOf()"
        } else {
            val zero = f.type.zeroValue
            if (f.kind == FieldKind.OPTIONAL && f.defaultValue != null) {
                f.type.ktType(fqnMap) to f.defaultValue
            } else if (f.kind == FieldKind.REQUIRED && zero != null) {
                f.type.ktType(fqnMap) to zero
            } else {
                nullableFields.add(f)
                "${f.type.ktType(fqnMap)}?" to "null"
            }
        }
        appendln("        var ${f.name.escape()}: $type = $initExpression")
    }

    val of = m.fields.filterIsInstance<MessageEntry.OneOf>().singleOrNull()?.let { of ->
        appendln("        var oneOfCase: ${m.name}.${of.name.escape().firstUpper()}Case = ${m.name}.${of.name.escape().firstUpper()}Case.${of.name.toUpperCase()}_NOT_SET")
        of
    }

    appendln("        while (hasData) {")
    appendln("            when (val fieldHeader = readInt32()) {")

    val indent = "                "
    allFields.forEach { f ->
        val readExpression = f.type.toReaderInvocation(typeMap, fqnMap)
        val header = (f.index shl 3) or f.type.wireType(typeMap)
        if (f.kind == FieldKind.REPEATED) {
            appendln("${indent}${header} -> ${f.name.escape()}.add($readExpression)")
        } else if (f.kind == FieldKind.ONE_OF) {
            appendln("${indent}${header} -> {")
            appendln("${indent}    ${f.name.escape()} = $readExpression")
            appendln("${indent}    oneOfCase = ${m.name}.${of!!.name.escape().firstUpper()}Case.${f.name.toUpperCase()}")
            appendln("${indent}}")
        } else {
            appendln("${indent}${header} -> ${f.name.escape()} = $readExpression")
        }
    }

    appendln("                else -> skip(fieldHeader and 7)")
    appendln("            }")
    appendln("        }")

    var firstOneOf = true

    appendln("        return ${m.name}(${allFields.fold("") { acc, c ->
        var result = if (acc.isEmpty()) "" else "$acc, "
        if (c.kind == FieldKind.ONE_OF && firstOneOf) {
            firstOneOf = false
            result += "oneOfCase!!, "
        }
        result += c.name.escape()
        if (c.kind == FieldKind.REQUIRED && c in nullableFields) {
            result += "!!"
        }
        result
    }})")

    appendln("    }")
    appendln()
}

private val GUTS = """

    private var offset = 0

    private var currentEnd = source.size

    private val hasData: Boolean
        get() = offset < currentEnd

    private inline fun <T> readWithLength(block: () -> T): T {
        val length = readInt32()
        val oldEnd = currentEnd
        currentEnd = offset + length
        try {
            return block()
        } finally {
            currentEnd = oldEnd
        }
    }

    private fun nextByte(): Byte {
        if (!hasData) error("Oops")
        return source[offset++]
    }

    private fun readVarint64(): Long {
        var result = 0L

        var shift = 0
        while (true) {
            val b = nextByte().toInt()

            result = result or ((b and 0x7F).toLong() shl shift)
            shift += 7

            if ((b and 0x80) == 0) break
        }

        if (shift > 70) {
            error("int64 overflow ${'$'}shift")
        }

        return result
    }

    fun readInt32(): Int = readVarint64().toInt()

    fun readInt64(): Long = readVarint64()

    fun readBool(): Boolean = readVarint64() != 0L

    fun readFloat(): Float {
        var bits = nextByte().toInt() and 0xFF
        bits = bits or ((nextByte().toInt() and 0xFF) shl 8)
        bits = bits or ((nextByte().toInt() and 0xFF) shl 16)
        bits = bits or ((nextByte().toInt() and 0xFF) shl 24)

        return Float.fromBits(bits)
    }

    fun readDouble(): Double {
        var bits = nextByte().toLong() and 0xFF
        bits = bits or ((nextByte().toLong() and 0xFF) shl 8)
        bits = bits or ((nextByte().toLong() and 0xFF) shl 16)
        bits = bits or ((nextByte().toLong() and 0xFF) shl 24)
        bits = bits or ((nextByte().toLong() and 0xFF) shl 32)
        bits = bits or ((nextByte().toLong() and 0xFF) shl 40)
        bits = bits or ((nextByte().toLong() and 0xFF) shl 48)
        bits = bits or ((nextByte().toLong() and 0xFF) shl 56)

        return Double.fromBits(bits)
    }

    private fun readString(): String {
        val length = readInt32()
        val result = String(source, offset, length)
        offset += length
        return result
    }

    private fun skip(type: Int) {
        when (type) {
            0 -> readInt64()
            1 -> offset += 8
            2 -> {
                val len = readInt32()
                offset += len
            }
            3, 4 -> error("groups")
            5 -> offset += 4
        }
    }

"""