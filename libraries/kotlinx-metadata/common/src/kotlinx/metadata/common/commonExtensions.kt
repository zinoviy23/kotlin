/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.common

import kotlinx.metadata.*
import kotlinx.metadata.impl.ReadContext
import kotlinx.metadata.impl.WriteContext
import kotlinx.metadata.impl.extensions.*
import kotlinx.metadata.impl.writeAnnotation
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf

class CommonMetadataExtensions : MetadataExtensions {
    override fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, c: ReadContext) {
        TODO("not implemented")
    }

    override fun readPackageExtensions(v: KmPackageVisitor, proto: ProtoBuf.Package, c: ReadContext) {
        TODO("not implemented")
    }

    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext) {
        TODO("not implemented")
    }

    override fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, c: ReadContext) {
        TODO("not implemented")
    }

    override fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, c: ReadContext) {
        TODO("not implemented")
    }

    override fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, c: ReadContext) {
        TODO("not implemented")
    }

    override fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, c: ReadContext) {
        TODO("not implemented")
    }

    override fun writeClassExtensions(type: KmExtensionType, proto: ProtoBuf.Class.Builder, c: WriteContext): KmClassExtensionVisitor? {
        TODO("not implemented")
    }

    override fun writePackageExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Package.Builder,
        c: WriteContext
    ): KmPackageExtensionVisitor? {
        TODO("not implemented")
    }

    override fun writeFunctionExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Function.Builder,
        c: WriteContext
    ): KmFunctionExtensionVisitor? {
        if (type != CommonFunctionExtensionVisitor.TYPE) return null
        return object : CommonFunctionExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                val stringTable = TODO()
                proto.setExtension(KlibMetadataProtoBuf.functionAnnotation, annotation.writeAnnotation(stringTable).build())
            }
        }
    }

    override fun writePropertyExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Property.Builder,
        c: WriteContext
    ): KmPropertyExtensionVisitor? {
        TODO("not implemented")
    }

    override fun writeConstructorExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Constructor.Builder,
        c: WriteContext
    ): KmConstructorExtensionVisitor? {
        TODO("not implemented")
    }

    override fun writeTypeParameterExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.TypeParameter.Builder,
        c: WriteContext
    ): KmTypeParameterExtensionVisitor? {
        TODO("not implemented")
    }

    override fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, c: WriteContext): KmTypeExtensionVisitor? {
        TODO("not implemented")
    }

    override fun createClassExtension(): KmClassExtension {
        TODO("not implemented")
    }

    override fun createPackageExtension(): KmPackageExtension {
        TODO("not implemented")
    }

    override fun createFunctionExtension(): KmFunctionExtension {
        TODO("not implemented")
    }

    override fun createPropertyExtension(): KmPropertyExtension {
        TODO("not implemented")
    }

    override fun createConstructorExtension(): KmConstructorExtension {
        TODO("not implemented")
    }

    override fun createTypeParameterExtension(): KmTypeParameterExtension {
        TODO("not implemented")
    }

    override fun createTypeExtension(): KmTypeExtension {
        TODO("not implemented")
    }
}

abstract class CommonFunctionExtensionVisitor : KmFunctionExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(CommonFunctionExtensionVisitor::class)
    }

    abstract fun visitAnnotation(annotation: KmAnnotation)
}

class CommonFunctionExtension() : CommonFunctionExtensionVisitor(), KmFunctionExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun accept(visitor: KmFunctionExtensionVisitor) {
        require(visitor is CommonFunctionExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
    }
}

val KmFunction.commonExtensions: CommonFunctionExtension
    get() = visitExtensions(CommonFunctionExtensionVisitor.TYPE) as CommonFunctionExtension

val KmFunction.annotations: MutableList<KmAnnotation>
    get() = commonExtensions.annotations