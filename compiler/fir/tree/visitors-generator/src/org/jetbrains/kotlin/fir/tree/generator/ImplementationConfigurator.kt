/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeImplementationConfigurator

object ImplementationConfigurator : AbstractFirTreeImplementationConfigurator(FirTreeBuilder) {
    fun configureImplementations() {
        configure()
        generateDefaultImplementations(FirTreeBuilder)
        configureControlFlowGraphReference()
    }

    private fun configureControlFlowGraphReference() {
        FirTreeBuilder.elements.forEach { element ->
            element.allFields.firstOrNull { it.name == "controlFlowGraphReference" }?.let {
                impl(element) {
                    sep(it)
                    default("controlFlowGraphReference", "FirEmptyControlFlowGraphReference()")
                }
            }
        }

    }

    private fun configure() = with(FirTreeBuilder) {
        impl(whenExpression) {
            sep("branches", "subject", needTransformOthers = true)
        }

        impl(anonymousFunction) {
            sep("valueParameters", "returnTypeRef")
        }

        impl(anonymousObject) {
            default("classKind") {
                value = "ClassKind.OBJECT"
                withGetter = true
            }
        }

        val constructorImplConfiguration: ImplementationContext.() -> Unit = {
            sep("valueParameters", "returnTypeRef")

            defaultNull("delegatedConstructor")
            defaultNull("body")
            default("name", "Name.special(\"<init>\")")
        }

        impl(constructor, config = constructorImplConfiguration)

        impl(constructor, "FirPrimaryConstructorImpl") {
            constructorImplConfiguration()

            default("isPrimary") {
                value = "true"
                withGetter = true
            }
        }

        // TODO: complex field implementation
        impl(declarationStatus)

        impl(klass) {
            defaultNull("companionObject")

            default("modality") {
                delegate = "status"
            }

            default("visibility") {
                delegate = "status"
            }
        }

        impl(enumEntry) {
            default("status", "FirDeclarationStatusImpl(Visibilities.UNKNOWN, Modality.FINAL)")
            default("classKind") {
                value = "ClassKind.ENUM_ENTRY"
                withGetter = true
            }
            default("companionObject") {
                value = "null"
                withGetter = true
            }
            default("typeRef", "session.builtinTypes.enumType")

            sep("arguments")
        }

        impl(import)

        impl(resolvedImport) {
            delegateFields(listOf("aliasName", "importedFqName", "isAllUnder"), "delegate")

            default("resolvedClassId") {
                delegate = "relativeClassName"
                delegateCall = "let { ClassId(packageFqName, it, false) }"
                withGetter = true
            }

            default("importedName") {
                delegate = "importedFqName"
                delegateCall = "shortName()"
                withGetter = true
            }
        }

        impl(valueParameter) {
            sep("returnTypeRef")
        }

        impl(annotationCall) {
            default("typeRef") {
                value = "annotationTypeRef"
                withGetter = true
            }
        }

        impl(arrayOfCall) {
            defaultImplicitTypeRef()
        }

        impl(arraySetCall) {
            lateinit("calleeReference")
            defaultImplicitTypeRef()
            sep("rValue", "arguments")
        }

        impl(binaryLogicExpression) {
            sep("leftOperand", "rightOperand", needTransformOthers = true)
        }

        impl(callableReferenceAccess) {
            defaultNull("explicitReceiver")
            lateinit("calleeReference")
        }

        impl(catchClause) {
            sep("parameter", "block")
        }

        impl(componentCall) {
            sep("calleeReference", "explicitReceiver")
            default("calleeReference", "FirSimpleNamedReference(psi, Name.identifier(\"component\$componentIndex\"))")
        }

        impl(whileLoop) {
            sep("condition", "block", needTransformOthers = true)
            defaultNull("label")
            lateinit("block")
        }

        impl(doWhileLoop) {
            sep("condition", "block", needTransformOthers = true)
            defaultNull("label")
            lateinit("block")
        }

        impl(delegatedConstructorCall) {
            sep("calleeReference")
            default(
                "calleeReference",
                "if (isThis) FirExplicitThisReference(psi, null) else FirExplicitSuperReference(psi, constructedTypeRef)"
            )
            defaultImplicitTypeRef()
        }

        impl(expression, "FirElseIfTrueCondition") {
            defaultImplicitTypeRef()
        }

        impl(block)
        impl(block, "FirEmptyExpressionBlock") {
            // TODO: make statements immutable
        }

        impl(singleExpressionBlock) {
            default("statements") {
                value = "mutableListOf(statement)"
                withGetter = true
            }
        }

        // TODO
        impl(statement, "FirErrorLoop")

        impl(expression, "FirExpressionStub")

        impl(functionCall) {
            lateinit("calleeReference")
            defaultReceivers()
            sep("explicitReceiver", "calleeReference")
        }

        impl(qualifiedAccessExpression) {
            lateinit("calleeReference")
            defaultReceivers()
        }

        impl(expressionWithSmartcast) {
            listOf("safe", "explicitReceiver", "extensionReceiver", "dispatchReceiver", "calleeReference").forEach {
                default(it) {
                    delegate = "originalExpression"
                }
            }
            default("originalType") {
                delegate = "originalExpression"
                delegateCall = "typeRef"
            }
            // TODO: add configuring acceptChildren
        }

        impl(expression, "FirNoReceiverExpression") {
            // TODO: add object support
            defaultImplicitTypeRef()
        }

        impl(getClassCall) {
            default("argument") {
                value = "arguments.first()"
                withGetter = true
            }
        }

        impl(lambdaArgumentExpression) {
            default("isSpread") {
                value = "false"
                withGetter = true
            }
        }

        impl(spreadArgumentExpression) {
            default("isSpread") {
                value = "true"
                withGetter = true
            }
        }

        impl(operatorCall) {
            default("typeRef", """
                |if (operation in FirOperation.BOOLEANS) {
                |    FirImplicitBooleanTypeRef(null)
                |} else {
                |    FirImplicitTypeRefImpl(null)
                |}
                """.trimMargin())
        }

        impl(resolvedQualifier) {
            isVal("packageFqName", "relativeClassFqName")
        }

        impl(returnExpression) {
            lateinit("target")
            default("typeRef", "FirImplicitNothingTypeRef(psi)")
        }

        impl(stringConcatenationCall) {
            default("typeRef", "FirImplicitStringTypeRef(psi)")
        }

        impl(throwExpression) {
            default("typeRef", "FirImplicitNothingTypeRef(psi)")
        }

        impl(qualifiedAccessExpression, "FirThisReceiverExpressionImpl")

        impl(tryExpression) {
            sep("tryBlock", "catches", "finallyBlock", needTransformOthers = true)
            defaultImplicitTypeRef()
        }

        impl(expression, "FirUnitExpression") {
            default("typeRef", "FirImplicitUnitTypeRef(psi)")
        }

        impl(variableAssignment) {
            isVal("operation")
            lateinit("calleeReference")
            defaultReceivers()
            // TODO: lValue
        }

        impl(variable) {
            isVal("isVar")
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }
            default("delegateFieldSymbol") {
                value = "delegate?.let { FirDelegateFieldSymbol(symbol.callableId) }"
                isVal = true
            }
        }

        impl(whenBranch) {
            sep("condition", "result", needTransformOthers = true)
        }

        impl(whenSubjectExpression) {
            defaultImplicitTypeRef()
        }

        impl(wrappedDelegateExpression) {
            lateinit("delegateProvider")
            default("typeRef") {
                delegate = "expression"
            }
        }
    }
}