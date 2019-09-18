/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.ElementsBuilder.Companion.boolean
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.ElementsBuilder.Companion.string

fun main() {
    val builder = buildElements {
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

        // ========================= Elements =========================

        val typeRef = element("TypeRef")
        val reference = element("Reference")
        val label = element("Label")

        val typeParameter = element("TypeParameter")
        val declarationStatus = element("DeclarationStatus")

        val statement = element("Statement")
        val expression = element("Expression", statement)
        val declaration = element("Declaration")

        val valueParameter = element("ValueParameter", declaration)
        val variable = element("Variable", declaration)
        val klass = element("Class", declaration)
        val memberFunction = element("MemberFunction", declaration)
        val memberProperty = element("MemberProperty", declaration)
        val propertyAccessor = element("PropertyAccessor")
        val constructor = element("Constructor", declaration)

        val anonymousFunction = element("AnonymousFunction", declaration, expression)
        val anonymousObject = element("AnonymousObject", declaration, expression)

        val doWhileLoop = element("DoWhileLoop", statement)
        val whileLoop = element("WhileLoop", statement)

        val block = element("Block", expression)
        val binaryLogicExpression = element("BinaryLogicExpression", expression)
        val loopJump = element("LoopJump", expression)
        element("BreakExpression", loopJump)
        element("ContinueExpression", loopJump)
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

        // ========================= Fields =========================

        val calleeReference = fieldSet(
            field("calleeReference", reference)
        )

        val loopFields = fieldSet(
            field("condition", expression),
            field(block),
            field(label, nullable = true)
        )

        val receivers = fieldSet(
            field("explicitReceiver", expression, nullable = true),
            field("dispatchReceiver", expression),
            field("extensionReceiver", expression)
        )

        val typeArguments = fieldSet(
            fieldList("typeArguments", typeProjection)
        )

        val arguments = fieldSet(
            fieldList("arguments", expression)
        )

        val qualifiedAccess = fieldSet(
            field("safe", boolean)
        ) with listOf(
            calleeReference,
            receivers
        )

        val declarations = fieldSet(
            fieldList(declaration)
        )

        val annotations = fieldSet(
            fieldList("annotations", annotationCall)
        )

        val symbol = fieldSet(
            field(symbolType)
        )

        fun body(nullable: Boolean = false) = fieldSet(
            field("body", block, nullable)
        )

        val returnTypeRef = fieldSet(
            field("returnTypeRef", typeRef)
        )

        fun receiverTypeRef(nullable: Boolean = false) = fieldSet(
            field("receiverTypeRef", typeRef, nullable)
        )

        val valueParameters = fieldSet(
            fieldList(valueParameter)
        )

        val typeParameters = fieldSet(
            fieldList(typeParameter)
        )

        val name = fieldSet(
            field(nameType)
        )

        val initializer = fieldSet(
            field("initializer", expression, nullable = true)
        )

        val superTypeRefs = fieldSet(
            fieldList("superTypeRefs", typeRef)
        )

        val classKind = fieldSet(
            field(classKindType)
        )
        
        val status = fieldSet(
            field("status", declarationStatus)
        )

        // ========================= Configuration =========================

        expression.configure {
            +field(typeRef)
            +annotations
        }

        block has fieldList(statement)

        binaryLogicExpression.configure {
            +field("leftOperand", expression)
            +field("rightOperand", expression)
        }

        loopJump has field("target", jumpTargetType)

        label has field("name", string)

        doWhileLoop has loopFields

        whileLoop has loopFields

        catchClause.configure {
            +field("parameter", valueParameter)
            +field(block)
        }

        tryExpression.configure {
            +calleeReference
            +field("tryBlock", block)
            +fieldList("catches", catchClause)
        }

        constExpression has field("kind", constKindType)

        functionCall.configure {
            +qualifiedAccess
            +typeArguments
            +arguments
        }

        operatorCall.configure {
            +field(operationType)
            +arguments
        }

        typeOperatorCall.configure {
            +field(operationType)
            +arguments
            +field("conversionTypeRef", typeRef)
        }

        whenExpression.configure {
            +calleeReference
            +field("subject", expression, nullable = true)
            +field("subjectVariable", variable, nullable = true)
            +fieldList("branches", whenBranch)
        }

        whenBranch.configure {
            +field("condition", expression)
            +field("result", block)
        }

        klass.configure {
            +superTypeRefs
            +classKind
            +declarations
            +annotations
            +symbol
            +field("companionObject", klass)
        }

        anonymousFunction.configure {
            +returnTypeRef
            +receiverTypeRef(nullable = true)
            +symbol
            +field(label, nullable = true)
            +valueParameters
            +body(nullable = true)
            +field(invocationKindType, nullable = true)
        }

        typeParameter.configure {
            +symbol
            +field(varianceType)
            +field("isReified", boolean)
            +fieldList("bounds", typeRef)
            +name
            +annotations
        }

        memberFunction.configure {
            +symbol
            +valueParameters
            +body(nullable = true)
            +receiverTypeRef(nullable = true)
            +returnTypeRef
            +typeParameters
            +status
            +name
            +annotations
        }

        memberProperty.configure {
            +symbol
            +booleanField("isVar")
            +initializer
            +field("delegate", expression, nullable = true)
            +field("backingFieldSymbol", symbolType)
            +field("delegateFieldSymbol", symbolType, nullable = true)
            +field("getter", propertyAccessor, nullable = true)
            +field("setter", propertyAccessor, nullable = true)
            +receiverTypeRef(nullable = true)
            +returnTypeRef
            +typeParameters
            +status
        }

        propertyAccessor.configure {
            +booleanField("isGetter")
            +returnTypeRef
            +status
            +body(nullable = true)
            +valueParameters
            +receiverTypeRef(nullable = true)
            +symbol
        }

        declarationStatus.configure {
            +field(visibilityType)
            +field(modalityType, nullable = true)
            generateBooleanFields(
                "expect", "actual", "override", "operator", "infix", "tailRec",
                "external", "const", "companion", "data", "suspend", "static"
            )
        }

        anonymousObject.configure {
            +superTypeRefs
            +declarations
            +classKind
        }

        constructor.configure {
            +symbol
            +field(delegatedConstructorCall, nullable = true)
            +valueParameters
            +body(nullable = true)
            +receiverTypeRef(nullable = true)
            +returnTypeRef
            +typeParameters
            +status
            +name
            +annotations
        }

        delegatedConstructorCall.configure {
            +field("constructedTypeRef", typeRef)
            generateBooleanFields("this")
            +calleeReference
        }

        valueParameter.configure {
            +returnTypeRef
            +field("defaultValue", expression, nullable = true)
            +symbol
            generateBooleanFields("crossinline", "noinline", "vararg")
        }

        variable.configure {
            +returnTypeRef
            +initializer
            +symbol
            +field("delegate", expression, nullable = true)
            +field("delegateFieldSymbol", symbolType, nullable = true)
        }
    }
    printElements(builder)
}