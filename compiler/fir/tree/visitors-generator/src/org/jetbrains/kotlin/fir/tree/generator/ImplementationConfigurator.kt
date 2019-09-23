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
    }

    private fun configure() = with(FirTreeBuilder) {
        impl(whenExpression) {
            sep("branches", "subject")
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

        elements.forEach { element ->
            element.allFields.firstOrNull { it.name == "controlFlowGraphReference" }?.let {
                impl(element) {
                    sep(it)
                    default("controlFlowGraphReference", "FirEmptyControlFlowGraphReference()")
                }
            }
        }
    }
}