/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import java.io.File
import java.io.PrintWriter

private const val BASE_PATH = "compiler/fir/tree/visitors-generator/src/org/jetbrains/kotlin/fir/tree/generator/result/"
private const val FIR_PATH = "$BASE_PATH/Result.kt"
private const val IMPL_PATH = "$BASE_PATH/Impl.kt"
private const val VISITOR_PATH = "$BASE_PATH/Visitor.kt"
private const val TRANSFORMER_PATH = "$BASE_PATH/Transformer.kt"
private const val INDENT = "    "

fun printElements(builder: AbstractFirTreeBuilder) {
    File(FIR_PATH).printWriter().use {
        builder.elements.forEach(it::printElement)
        builder.types.forEach(it::printType)
    }

    File(IMPL_PATH).printWriter().use { printer ->
        builder.elements.flatMap { it.allImplementations }
            .forEach(printer::printImplementation)
    }
    printVisitor(builder.elements)
    printTransformer(builder.elements)
}

fun printVisitor(elements: List<Element>) {
    File(VISITOR_PATH).printWriter().use { printer ->
        with(printer) {
            println("abstract class FirVisitor<out R, in D> {")

            indent()
            println("abstract fun visitElement(element: FirElement, data: D): R")

            for (element in elements) {
                if (element == AbstractFirTreeBuilder.baseFirElement) continue
                with(element) {
                    indent()
                    val varName = safeDecapitalizedName
                    println("open fun visit$name($varName: $type, data: D): R = visitElement($varName, data)")
                    println()
                }
            }
            println("}")
        }
    }
}

fun PrintWriter.printFieldWithDefaultInImplementation(field: Field) {
    val defaultValue = field.defaultValue
    indent()
    print("override ")
    if (field.isLateinit) {
        print("lateinit ")
    }
    if (field.isVal || field is FieldList) {
        print("val")
    } else {
        print("var")
    }
    print(" ${field.name}: ${field.mutableType} ")
    if (field.isLateinit) {
        println()
        return
    }
    if (field.withGetter) {
        print("get() ")
    }
    requireNotNull(defaultValue) {
        "No default value for $field"
    }
    println("= $defaultValue")
}

fun PrintWriter.printImplementation(implementation: Implementation) {
    fun Field.transform() {
        when (this) {
            is FirField ->
                println("$name = ${name}${call()}transformSingle(transformer, data)")

            is FieldList -> {
                println("${name}.transformInplace(transformer, data)")
            }

            else -> throw IllegalStateException()
        }
    }

    with(implementation) {
        print("class $type")
        val fieldsWithoutDefault = element.allFields.filter { it.defaultValue == null && !it.isLateinit}
        val fieldsWithDefault = element.allFields.filter { it.defaultValue != null || it.isLateinit}

        if (fieldsWithoutDefault.isNotEmpty()) {
            println("(")
            fieldsWithoutDefault.forEachIndexed { i, field ->
                val end = if (i == fieldsWithoutDefault.size - 1) "" else ","
                printField(field, isVar = true, override = true, end = end)
            }
            print(")")
        }
        println(" : ${element.type} {")

        fieldsWithDefault.forEach { printFieldWithDefaultInImplementation(it) }
        if (fieldsWithDefault.isNotEmpty()) {
            println()
        }

        indent()
        println("override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): $type {")
        for (field in element.allFirFields) {
            if (field.isVal) continue
            if (field !in separateTransformations) {
                if (!needRestTransforms) {
                    indent(2)
                    field.transform()
                }
            } else {
                indent(2)
                println("transform${field.name.capitalize()}(transformer, data)")
            }
        }
        if (needRestTransforms) {
            indent(2)
            println("transformOtherChildren(transformer, data)")
        }
        indent(2)
        println("return this")
        indent()
        println("}")

        for (field in separateTransformations) {
            println()
            indent()
            println("override ${field.transformFunctionDeclaration(type)} {")
            indent(2)
            field.transform()
            indent(2)
            println("return this")
            indent()
            println("}")
        }

        if (needRestTransforms) {
            println()
            indent()
            println("override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): $type {")
            for (field in element.allFirFields) {
                if (field.isVal) continue
                if (field !in separateTransformations) {
                    indent(2)
                    field.transform()
                }
            }
            indent(2)
            println("return this")
            indent()
            println("}")
        }

        element.allFields.filter { it.withReplace }.forEach { field ->
            println()
            indent()
            println("override ${field.replaceFunctionDeclaration()} {")
            indent(2)
            val newValue = "new${field.name.capitalize()}"
            when {
                field.withGetter -> {
                    println("throw IllegalStateException()")
                }

                field is FieldList -> {
                    println("${field.name}.clear()")
                    indent(2)
                    println("${field.name}.addAll($newValue)")
                }

                else -> {
                    println("${field.name} = $newValue")
                }
            }
            indent()
            println("}")
        }

        println("}")
        println()
    }
}

fun Field.transformFunctionDeclaration(returnType: String): String {
    return transformFunctionDeclaration(name.capitalize(), returnType)
}

fun transformFunctionDeclaration(transformName: String, returnType: String): String {
    return "fun <D> transform$transformName(transformer: FirTransformer<D>, data: D): $returnType"
}

fun Field.replaceFunctionDeclaration(): String {
    val capName = name.capitalize()
    return "fun replace$capName(new$capName: $type)"
}

fun printTransformer(elements: List<Element>) {
    File(TRANSFORMER_PATH).printWriter().use { printer ->
        with(printer) {
            println("interface CompositeTransformResult<out T : Any>")
            println()

            println("fun <T : FirElement, D> T.transformSingle(transformer: FirTransformer<D>, data: D): T = TODO()")
            println("fun <T : FirElement, D> MutableList<T>.transformInplace(transformer: FirTransformer<D>, data: D) {}")
            println()

            println("abstract class FirTransformer<in D> : FirVisitor<CompositeTransformResult<FirElement>, D>() {")
            println()
            indent()
            println("abstract fun <E : FirElement> transformElement(element: E, data: D): CompositeTransformResult<E>")
            for (element in elements) {
                if (element == AbstractFirTreeBuilder.baseFirElement) continue
                indent()
                val varName = element.safeDecapitalizedName
                println("open fun transform${element.name}($varName: ${element.type}, data: D): CompositeTransformResult<${element.type}> {")
                indent(2)
                println("return transformElement($varName, data)")
                indent()
                println("}")
                println()
            }

            for (element in elements) {
                indent()
                val varName = element.safeDecapitalizedName
                println("final override fun visit${element.name}($varName: ${element.type}, data: D): CompositeTransformResult<${element.type}> {")
                indent(2)
                println("return transform${element.name}($varName, data)")
                indent()
                println("}")
                println()
            }
            println("}")
        }
    }
}

fun PrintWriter.printField(field: Field, isVar: Boolean, override: Boolean, end: String) {
    indent()
    if (override) {
        print("override ")
    }
    if (isVar && field !is FieldList) {
        print("var")
    } else {
        print("val")
    }
    val type = if (isVar) field.mutableType else field.type
    println(" ${field.name}: $type$end")
}

val Field.mutableType: String get() = if (this is FieldList) "Mutable$type" else type

fun Field.call(): String = if (nullable) "?." else "."

fun PrintWriter.printElement(element: Element) {
    fun Element.override() {
        indent()
        if (this != AbstractFirTreeBuilder.baseFirElement) {
            print("override ")
        }
    }

    with(element) {
        print("interface $type")
        if (parents.isNotEmpty()) {
            print(" : ")
            print(parents.joinToString(", ") { it.type })
        }
        println(" {")
        allFields.forEach {
            printField(it, isVar = false, override = it in parentFields, end = "")
        }
        if (allFields.isNotEmpty()) {
            println()
        }

        override()
        println("fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visit$name(this, data)")
        println()
        override()
        print("fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {")
        if (allFirFields.isNotEmpty()) {
            println()
            allFirFields.forEach {
                indent(2)
                when (it) {
                    is FirField -> {
                        println("${it.name}${it.call()}accept(visitor, data)")
                    }

                    is FieldList -> {
                        println("${it.name}.forEach { it.accept(visitor, data) }")
                    }

                    else -> throw IllegalStateException()
                }
            }
            indent()
        }
        println("}")

        fields.filter { it.withReplace }.forEach {
            indent()
            println(it.replaceFunctionDeclaration())
        }

        allImplementations.firstOrNull()?.let {
            for (field in it.separateTransformations) {
                println()
                indent()
                if (field in parentFields) {
                    print("override ")
                }
                println(field.transformFunctionDeclaration(type))
            }
            if (it.needRestTransforms) {
                println()
                indent()
                println(transformFunctionDeclaration("OtherChildren", type))
            }
        }

        if (element == AbstractFirTreeBuilder.baseFirElement) {
            println()
            indent()
            println("@Suppress(\"UNCHECKED_CAST\")")
            indent()
            println("fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E> =")
            indent(2)
            println("accept(visitor, data) as CompositeTransformResult<E>")
            println()
            indent()
            println("fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement = this")
        }
        println("}")
        println()
    }
}

// --------------------------------------- Helpers ---------------------------------------

fun PrintWriter.indent(n: Int = 1) {
    print(INDENT.repeat(n))
}

fun PrintWriter.printType(type: Type) {
    println("interface ${type.type}")
    println()
}

val Element.safeDecapitalizedName: String get() = if (name == "Class") "klass" else name.decapitalize()