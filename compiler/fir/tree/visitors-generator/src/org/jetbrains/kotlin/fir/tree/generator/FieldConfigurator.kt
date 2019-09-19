/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.annotations
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.arguments
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.body
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.calleeReference
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.classKind
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.declarations
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.initializer
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.loopFields
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.name
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.qualifiedAccess
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.receiverTypeRef
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.returnTypeRef
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.status
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.superTypeRefs
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.symbol
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeArguments
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeParameters
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.valueParameters
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFieldConfigurator
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder

object FieldConfigurator : AbstractFieldConfigurator() {
    fun configureFields() = with(FirTreeBuilder) {
        expression.configure {
            +field(typeRef, withReplace = true)
            +annotations
        }

        block has fieldList(statement)

        binaryLogicExpression.configure {
            +field("leftOperand", expression)
            +field("rightOperand", expression)
        }

        loopJump has field("target", jumpTargetType)

        label has field("name", AbstractFirTreeBuilder.string)

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
            +field("isReified", AbstractFirTreeBuilder.boolean)
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
}