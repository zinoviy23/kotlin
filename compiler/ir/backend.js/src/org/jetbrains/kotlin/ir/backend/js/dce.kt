/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isClassWithFqName
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*


fun removeUnreachables(
    entryFragments: List<IrPackageFragment>,
    allFragments: List<IrPackageFragment>,
    context: JsIrBackendContext,
    atStart: Boolean = false
) {
    val rootSet = getAllDeclarations(entryFragments, context) +
            (if (atStart) context.primitiveCompanionObjects.flatMap {
                val e = it.value.owner
                listOf(e) + e.declarations
            } else emptyList())
    val allReachebles = getAllReachebles(rootSet, context, atStart)
    removeUnreachables(allFragments, allReachebles, context, atStart)
}

private fun getAllDeclarations(files: List<IrPackageFragment>, context: JsIrBackendContext): Set<IrDeclaration> {
    return files.flatMap { it.declarations }.toSet()
//    return files.flatMap { it.declarations.filter { it is IrDeclarationWithName && it.isExported(context) } }.toSet()
//    return mutableSetOf<IrDeclaration>().apply {
//        files.forEach {
//            it.accept(object : IrElementVisitorVoid {
//                // TODO: probably getting only toplevel declarations should be enough
//                override fun visitElement(element: IrElement) {
//                    element.acceptChildrenVoid(this)
//                }
//
//                override fun visitDeclaration(declaration: IrDeclaration) {
//                    add(declaration)
//                    super.visitDeclaration(declaration)
//                }
//            }, null)
//        }
//    }
}

private fun getAllReachebles(
    root: Set<IrDeclaration>,
    context: JsIrBackendContext,
    atStart: Boolean
): Set<IrDeclaration> {
    val queue = LinkedList(root)
    val visited = mutableSetOf<IrDeclaration>()
    val mapDeclarationToOverrides = mutableMapOf<IrDeclaration, MutableSet<IrDeclaration>>()

    val u = mutableSetOf<String>()

    // TODO we need more precise solution
    // preserve all members of any and their inheritors
//    visited += context.irBuiltIns.anyClass.owner.declarations

    fun atStart(f: () -> Unit) {
        if (atStart) f()
    }

    queue@ while (queue.isNotEmpty()) {
        val d = queue.remove()

        if (d in visited) continue

        visited += d
        // inheriters

        fun q(declaration: IrDeclaration) {
            fun logDependency() {
                // don't print local accesses
//                if (declaration is IrValueDeclaration) return

                val from = (d as? IrDeclarationWithName)?.fqNameWhenAvailable ?: "<unknown>"
                val to = (declaration as? IrDeclarationWithName)?.fqNameWhenAvailable ?: "<unknown>"

                val v = "\"$from\" -> \"$to\""

                if (u.add(v)) println(v)
            }

//            fun IrDeclaration.isLongMember() = parent.let { it is IrClass && it.fqNameWhenAvailable?.asString() == "kotlin.Long" }
//            if (declaration.isLongMember() || d.isLongMember()) {
            logDependency()
//            }

            queue += declaration
        }

        fun qNN(declaration: IrDeclaration?) {
            if (declaration != null) q(declaration)
        }

        fun IrDeclaration.registerOverridens() {
            if (this is IrProperty) {
                getter?.registerOverridens()
                setter?.registerOverridens()
                backingField?.registerOverridens()
                return
            }
            if (this !is IrOverridableDeclaration<out IrSymbol>) return

            fun IrOverridableDeclaration<out IrSymbol>.addAll() {
                overriddenSymbols.forEach {
                    val od = it.owner as IrOverridableDeclaration<out IrSymbol>

                    if (od in visited) {
                        q(this@registerOverridens)
                    }

                    val l = mapDeclarationToOverrides.getOrPut(od) { mutableSetOf() }
                    l += this@registerOverridens
                    od.addAll()
                }
            }

            addAll()
        }

        // TODO
        when ((d as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString()) {
            "kotlin.js.equals" -> "equals"
            "kotlin.js.hashCode" -> "hashCode"
            "kotlin.js.toString" -> "toString"
            else -> null
        }?.let { name ->
            q(context.irBuiltIns.anyClass.owner.declarations.single { (it as? IrDeclarationWithName)?.name?.asString() == name })
        }

        when (d) {
            is IrConstructor -> {
                val klass = d.parent as IrClass
                q(klass)

                atStart {
                    qNN(klass.declarations.singleOrNull {
                        it is IrSimpleFunction && it.valueParameters.size == 0 && it.name.asString() == "toJSON"
                        // typeParameters.size? returnType?
                    })
                }
            }
            is IrClass -> {
                d.superTypes.forEach {
                    queue.addIfNotNull(it.classOrNull?.owner)
                }
//                qNN(d.constructors.filter(IrConstructor::isPrimary).singleOrNull())

                d.declarations.forEach(IrDeclaration::registerOverridens)

                continue@queue
            }
            is IrOverridableDeclaration<out IrSymbol> -> {
                mapDeclarationToOverrides.get(d)?.let {
                    it.filter { it !in visited }.forEach {
                        // mark overrides which treated as unreachable before if any of constructor of class is reachable
                        if (it.parentAsClass.declarations.any { it is IrConstructor && it in visited }) {
                            q(it)
                        }
                    }
                }

                atStart {
                    if (d is IrSimpleFunction) {
                        qNN(d.resolveFakeOverride())
                        qNN(d.correspondingPropertySymbol?.owner)
                    }
                }
            }
            // atStart
            is IrProperty -> {
                qNN(d.getter)
                qNN(d.setter)
                qNN(d.backingField)
            }
        }

        val visitor = if (atStart) {
            object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitDeclaration(declaration: IrDeclaration) {
                    // don't process nested declarations
                    // TODO doesn't it break local variables????
                    if (declaration !== d) return

                    super.visitDeclaration(declaration)
                }
                // TODO type references?

                override fun visitFunctionExpression(expression: IrFunctionExpression) {
                    q(expression.function)
                    super.visitFunctionExpression(expression)
                }

                override fun visitCallableReference(expression: IrCallableReference) {
                    val declaration = expression.symbol.owner as? IrDeclaration
                    qNN(declaration)
                    super.visitCallableReference(expression)
                }

                override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
                    val declaration = expression.symbol.owner
                    q(declaration)
                    // TODO w/o cast
//                    qNN((declaration as? IrSimpleFunction)?.resolveFakeOverride())

                    // TODO: generic solution?
                    if (expression.symbol == context.intrinsics.jsClass) {
                        qNN(expression.getTypeArgument(0)!!.classifierOrFail.owner as? IrDeclaration)
                    }

                    super.visitFunctionAccess(expression)
                }

                override fun visitDeclarationReference(expression: IrDeclarationReference) {
                    q(expression.symbol.owner as IrDeclaration) // TODO
                    super.visitDeclarationReference(expression)
                }

                override fun visitTypeOperator(expression: IrTypeOperatorCall) {
                    qNN(expression.typeOperand.classifierOrFail.owner as? IrDeclaration)
                    super.visitTypeOperator(expression)
                }

                override fun visitSingletonReference(expression: IrGetSingletonValue) {
                    val klass = expression.symbol.owner as? IrClass // TODO enum entry
                    qNN(klass)
                    ///???
                    qNN(klass?.constructors?.filter(IrConstructor::isPrimary)?.singleOrNull())
                    if (klass?.isCompanion == true) {
                        qNN(klass.parent as? IrDeclaration)
                    }
                    super.visitSingletonReference(expression)
                }
            }
        } else {
//            TODO()
            object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
                    val declaration = expression.symbol.owner
                    q(declaration)
                    // TODO w/o cast
                    qNN((declaration as? IrSimpleFunction)?.resolveFakeOverride())

                    // TODO: generic solution?
                    if (expression.symbol == context.intrinsics.jsClass) {
                        qNN(expression.getTypeArgument(0)!!.classifierOrFail.owner as? IrDeclaration)
                    }

                    super.visitFunctionAccess(expression)
                }

//            override fun visitFieldAccess(expression: IrFieldAccessExpression) {
//                q(expression.symbol.owner)
//                super.visitFieldAccess(expression)
//            }
//
//            override fun visitValueAccess(expression: IrValueAccessExpression, data: Nothing?) {
//                q(expression.symbol.owner)
//                super.visitValueAccess(expression, data)
//            }

                override fun visitDeclarationReference(expression: IrDeclarationReference) {
                    q(expression.symbol.owner as IrDeclaration) // TODO
                    super.visitDeclarationReference(expression)
                }

            }
        }

        d.accept(visitor, null)
    }
    return visited
}

private var removedCount = 0
private var keptCount = 0

private fun removeUnreachables(
    fragments: List<IrPackageFragment>,
    reachebles: Set<IrDeclaration>,
    context: JsIrBackendContext,
    atStart: Boolean
) {

    fun IrFunction.dummifyBody() {
        val statements = (body as? IrBlockBody)?.statements

        // TODO check that it is effectivly empty
        if (statements?.isNotEmpty() == true) {
            statements.apply {
                clear()
                add(
                    IrCallImpl(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        context.intrinsics.unreachable.returnType,
                        context.intrinsics.unreachable.symbol
                    )
                )
            }
        }
    }

    // TODO remove unused getters and setters
    // TODO lambdas: use contracts? link lambdas to use sites?
    // TODO member could be visited but any ctor not yet, such method could have overrides

    fun IrDeclarationContainer.process() {
        declarations.transformFlat {
            if (
                (it.parent as? IrClass)?.isInline == true ||
                // TODO other solution? force tracking usages inside?
                // TODO Does it still require special support?
                (it.parent as? IrClass)?.superTypes?.any { it.classOrNull?.isClassWithFqName(FqNameUnsafe("kotlin.Enum")) == true } == true
            // TODO keep Long.valueOf?
            ) {
                return@transformFlat null
            }

            fun keep(): List<IrDeclaration>? {
                keptCount++
                return null
            }

            fun remove(): List<IrDeclaration> {
                removedCount++
                return emptyList()
            }

            if (it in reachebles) {
                if (it is IrFunction && (it.parent as? IrClass)?.let { it !in reachebles} == true) {
                    it.dummifyBody()
                }

                if (it is IrDeclarationContainer) {
                    it.process()
                }

                keep()
            } else {
                if (it is IrConstructor && it.isPrimary && it.parentAsClass in reachebles) {
                    it.dummifyBody()
                    keep()
                } else {
                    remove()
                }
            }
        }
    }

    fragments.forEach(IrPackageFragment::process)
}
