/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import java.io.File
import java.io.PrintWriter

private const val FIR_PATH =
    "/home/demiurg/Programming/kotlin/kotlin-pill/compiler/fir/tree/visitors-generator/src/org/jetbrains/kotlin/fir/tree/generator/Result.kt"
private const val VISITOR_PATH = "/home/demiurg/Programming/kotlin/kotlin-pill/compiler/fir/tree/visitors-generator/src/org/jetbrains/kotlin/fir/tree/generator/Visitor.kt"
private const val INDENT = "    "

fun printElements(builder: ElementsBuilder) {
    File(FIR_PATH).printWriter().use {
        builder.elements.forEach(it::printElement)
        builder.types.forEach(it::printType)
    }
    printVisitor(builder.elements)
}

fun printVisitor(elements: List<Element>) {
    File(VISITOR_PATH).printWriter().use { printer ->
        with(printer) {
            println("interface FirVisitor<out R, in D> {")
            elements.forEach { element ->
                with(element) {
                    indent()
                    println("fun visit$name(${safeDecapitalizedName}: $type, data: D): R")
                    println()
                }
            }
            println("}")
        }
    }
}

fun PrintWriter.indent(n: Int = 1) {
    print(INDENT.repeat(n))
}

fun PrintWriter.printType(type: Type) {
    println("interface ${type.type}")
    println()
}

val Element.safeDecapitalizedName: String get() = if (name == "Class") "klass" else name.decapitalize()

fun PrintWriter.printElement(element: Element) {
    fun Element.override() {
        indent()
        if (this != ElementsBuilder.baseFirElement) {
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
            indent()
            if (it !in fields) {
                print("override ")
            }
            println("val ${it.name}: ${it.type}")
        }
        println()
        override()
        println("fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visit$name(this, data)")
        println()
        override()
        println("fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {")
        allFields.forEach {
            if (it is FirField) {
                indent(2)
                val safeCall = if (it.nullable) "?." else "."
                println("${it.name}${safeCall}accept(visitor, data)")
            }
        }
        indent()
        println("}")
        println("}")
        println()
    }
}