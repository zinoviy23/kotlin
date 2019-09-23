/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.annotationCall
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.block
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.cfgReference
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.classKindType
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.declaration
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.declarationStatus
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.expression
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.label
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.modalityType
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.nameType
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.reference
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.symbolType
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.typeParameter
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.typeProjection
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.typeRef
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.valueParameter
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.visibilityType
import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder

object FieldSets {
    val calleeReference = fieldSet(
        field("calleeReference", reference)
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
        field("safe", AbstractFirTreeBuilder.boolean)
    ) with listOf(
        calleeReference,
        receivers
    )

    val declarations = fieldSet(
        fieldList(declaration)
    )

    val annotations = fieldSet(
        fieldList("annotations", annotationCall).apply {
            defaultValue = "mutableListOf()"
        }
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

    fun superTypeRefs(withReplace: Boolean = false) = fieldSet(
        fieldList("superTypeRefs", typeRef, withReplace)
    )

    val classKind = fieldSet(
        field(classKindType)
    )

    val status = fieldSet(
        field("status", declarationStatus)
    )

    val controlFlowGraphReference = fieldSet(
        field("controlFlowGraphReference", cfgReference, withReplace = true)
    )

    val visibility = fieldSet(
        field(visibilityType)
    )

    val modality = fieldSet(
        field(modalityType, nullable = true)
    )
}