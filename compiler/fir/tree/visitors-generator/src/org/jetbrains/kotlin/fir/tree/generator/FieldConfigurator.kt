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
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.controlFlowGraphReference
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.declarations
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.initializer
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.modality
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.name
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.qualifiedAccess
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.receiverTypeRef
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.receivers
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.returnTypeRef
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.status
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.superTypeRefs
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.symbol
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeArguments
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeParameters
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.valueParameters
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FieldSets.visibility
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFieldConfigurator
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder

object FieldConfigurator : AbstractFieldConfigurator() {
    fun configureFields() = with(FirTreeBuilder) {
        expression.configure {
            +field(typeRef, withReplace = true)
            +annotations
        }

        block.configure {
            +fieldList(statement)
        }

        singleExpressionBlock.configure {
            +field(statement)
        }

        binaryLogicExpression.configure {
            +field("leftOperand", expression)
            +field("rightOperand", expression)
            +field("kind", operationKindType)
        }

        jump.configure {
            +field("target", jumpTargetType)
        }

        returnExpression.configure {
            +field("result", expression)
        }

        label.configure {
            +field("name", AbstractFirTreeBuilder.string)
        }

        doWhileLoop.configure {
            +field(block)
            +field("condition", expression)
            +field(label, nullable = true)
        }

        whileLoop.configure {
            +field("condition", expression)
            +field(block)
            +field(label, nullable = true)
        }

        catchClause.configure {
            +field("parameter", valueParameter)
            +field(block)
        }

        tryExpression.configure {
            +calleeReference
            +field("tryBlock", block)
            +fieldList("catches", catchClause)
            +field("finallyBlock", block, nullable = true)
        }

        constExpression.configure {
            +field("kind", constKindType)
            +field("value", "Any")
        }

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
            +symbol
            +name
            +visibility
            +modality
            +classKind
            +superTypeRefs(withReplace = true)
            +declarations
            +field("companionObject", klass, nullable = true)
            +annotations
            +status
            +typeParameters
        }

        typeAlias.configure {
            +annotations
            generateBooleanFields("actual", "expect")
            +modality
            +visibility
            +name
            +status
            +symbol
            +typeParameters
            +field("expandedTypeRef", typeRef, withReplace = true)
        }

        enumEntry.configure {
            +arguments
            +field(typeRef)
        }

        anonymousFunction.configure {
            +returnTypeRef
            +receiverTypeRef(nullable = true)
            +symbol
            +field(label, nullable = true)
            +valueParameters
            +body(nullable = true)
            +field(invocationKindType, nullable = true, withReplace = true)
            +controlFlowGraphReference
        }

        typeParameter.configure {
            +symbol
            +field(varianceType)
            +booleanField("isReified")
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
            +annotations
        }

        declarationStatus.configure {
            +visibility
            +modality
            generateBooleanFields(
                "expect", "actual", "override", "operator", "infix", "tailRec",
                "external", "const", "companion", "data", "suspend", "static"
            )
        }

        anonymousObject.configure {
            +superTypeRefs(withReplace = true)
            +declarations
            +classKind
        }

        constructor.configure {
            +symbol
            +field("delegatedConstructor", delegatedConstructorCall, nullable = true)
            +valueParameters
            +body(nullable = true)
            +controlFlowGraphReference
            +returnTypeRef
            +status
            +booleanField("isPrimary")
            +name
            +annotations
        }

        delegatedConstructorCall.configure {
            +field("constructedTypeRef", typeRef)
            generateBooleanFields("this")
            +calleeReference
        }

        valueParameter.configure {
            +name
            +returnTypeRef
            +field("defaultValue", expression, nullable = true)
            +symbol
            +annotations
            generateBooleanFields("crossinline", "noinline", "vararg")
        }

        variable.configure {
            +returnTypeRef
            +initializer
            +symbol
            +name
            +field("delegate", expression, nullable = true)
            +field("delegateFieldSymbol", symbolType, nullable = true)
            generateBooleanFields("var", "val")
        }

        anonymousInitializer.configure {
            +body(nullable = true)
        }

        file.configure {
            +fieldList(import)
            +declarations
            +name
            +annotations
            +field("packageFqName", fqNameType)
        }

        import.configure {
            +field("importedFqName", fqNameType, nullable = true)
            +field("aliasName", nameType, nullable = true)
            generateBooleanFields("allUnder")
        }

        resolvedImport.configure {
            +field("delegate", import)
            +field("packageFqName", fqNameType)
            +field("relativeClassName", fqNameType, nullable = true)
            +field("resolvedClassId", classIdType, nullable = true)
            +field("importedName", nameType, nullable = true)
        }

        annotationCall.configure {
            +field("useSiteTarget", annotationUseSiteTargetType, nullable = true)
            +field("annotationTypeRef", typeRef)
        }

        arrayOfCall.configure {
            +arguments
        }

        arraySetCall.configure {
            +field("rValue", expression)
            +field("operation", operationType)
            +calleeReference
            // +field("lValue", reference) // TODO
            +fieldList("indexes", expression)
            +arguments
        }

        callableReferenceAccess.configure {
            +calleeReference
            +field("explicitReceiver", expression ,nullable = true)
        }

        classReferenceExpression.configure {
            +field("classTypeRef", typeRef)
        }

        componentCall.configure {
            +intField("componentIndex")
            +field("explicitReceiver", expression)
        }

        errorExpression.configure {
            +stringField("reason")
        }

        qualifiedAccessExpression.configure {
            +qualifiedAccess
        }

        expressionWithSmartcast.configure {
            +field("originalExpression", qualifiedAccessExpression)
            +field("typesFromSmartcast", "Collection<ConeKotlinType>")
            +field("originalType", typeRef)
        }

        getClassCall.configure {
            +arguments
            +field("argument", expression)
        }

        wrappedArgumentExpression.configure {
            +field(expression)
            +booleanField("isSpread")
        }

        namedArgumentExpression.configure {
            +name
        }

        resolvedQualifier.configure {
            +field("packageFqName", fqNameType)
            +field("relativeClassFqName", fqNameType, nullable = true)
        }

        stringConcatenationCall.configure {
            +arguments
        }

        throwExpression.configure {
            +field("exception", expression)
        }

        variableAssignment.configure {
            +annotations
            +calleeReference
            +receivers
            +field("lValue", reference)
            +field("rValue", expression)
            +field("operation", operationType)
            +booleanField("safe")
        }

        whenSubjectExpression.configure {
            +field("whenSubject", whenSubjectType)
        }

        wrappedDelegateExpression.configure {
            +field(expression)
            +field("delegateProvider", expression)
        }
    }
}