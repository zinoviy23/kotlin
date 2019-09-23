/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder

object FirTreeBuilder : AbstractFirTreeBuilder() {
    // ========================= Types =========================

    val jumpTargetType = type("FirTarget")
    val constKindType = type("IrConstKind")
    val operationType = type("FirOperation")
    val classKindType = type("ClassKind")
    val symbolType = type("Symbol")
    val invocationKindType = type("InvocationKind")
    val varianceType = type("Variance")
    val nameType = type("Name")
    val visibilityType = type("Visibility")
    val modalityType = type("Modality")
    val fqNameType = type("FqName")
    val classIdType = type("ClassId")

    // ========================= Elements =========================

    val typeRef = element("TypeRef")
    val reference = element("Reference")
    val cfgReference = element("ControlFlowGraphReference", reference)
    val label = element("Label")
    val import = element("Import")
    val resolvedImport = element("ResolvedImport", import)

    val anonymousInitializer = element("AnonymousInitializer")

    val typeParameter = element("TypeParameter")
    val declarationStatus = element("DeclarationStatus")
    val resolvedDeclarationStatus = element("ResolvedDeclarationStatus", declarationStatus)

    val statement = element("Statement")
    val expression = element("Expression", statement)
    val declaration = element("Declaration")

    val valueParameter = element("ValueParameter", declaration)
    val variable = element("Variable", declaration)
    val klass = element("Class", declaration)
    val enumEntry = element("EnumEntry", klass)
    val memberFunction = element("MemberFunction", declaration)
    val memberProperty = element("MemberProperty", declaration)
    val propertyAccessor = element("PropertyAccessor")
    val constructor = element("Constructor", declaration)
    val file = element("File", declaration)

    val anonymousFunction = element("AnonymousFunction", declaration, expression)
    val anonymousObject = element("AnonymousObject", declaration, expression)

    val doWhileLoop = element("DoWhileLoop", statement)
    val whileLoop = element("WhileLoop", statement)

    val block = element("Block", expression)
    val binaryLogicExpression = element("BinaryLogicExpression", expression)
    val loopJump = element("LoopJump", expression)
    val breakExpression = element("BreakExpression", loopJump)
    val continueExpression = element("ContinueExpression", loopJump)
    val catchClause = element("Catch")
    val tryExpression = element("TryExpression", expression)
    val constExpression = element("ConstExpression", expression)
    val typeProjection = element("TypeProjection")
    val functionCall = element("FunctionCall", expression)
    val annotationCall = element("AnnotationCall", functionCall)
    val operatorCall = element("OperatorCall", expression)
    val typeOperatorCall = element("TypeOperatorCall", expression)
    val whenExpression = element("WhenExpression", expression)
    val whenBranch = element("WhenBranch")
    val delegatedConstructorCall = element("DelegatedConstructorCall", expression)
}