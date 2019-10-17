/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.*
import kotlinx.metadata.impl.ReadContext
import kotlinx.metadata.impl.WriteContext
import kotlinx.metadata.impl.extensions.*
import kotlinx.metadata.impl.writeAnnotation
import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf

class KlibMetadataExtensions : MetadataExtensions {
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
        if (type != KlibClassExtensionVisitor.TYPE) return null
        return object : KlibClassExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.setExtension(
                    KlibMetadataProtoBuf.classAnnotation,
                    listOf(annotation.writeAnnotation(c.strings).build())
                )
            }
        }
    }

    override fun writePackageExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Package.Builder,
        c: WriteContext
    ): KmPackageExtensionVisitor? {
        if (type != KlibPackageExtensionVisitor.TYPE) return null
        return object : KlibPackageExtensionVisitor() {
            override fun visitClass(klass: KmClass) {
//                proto.setExtension(
//                    KlibMetadataProtoBuf.className,
//                    listOf(0)
//                )
            }
        }
    }

    override fun writeFunctionExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Function.Builder,
        c: WriteContext
    ): KmFunctionExtensionVisitor? {
        if (type != KlibFunctionExtensionVisitor.TYPE) return null
        return object : KlibFunctionExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.setExtension(
                    KlibMetadataProtoBuf.functionAnnotation,
                    mutableListOf(annotation.writeAnnotation(c.strings).build())
                )
            }
        }
    }

    override fun writePropertyExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Property.Builder,
        c: WriteContext
    ): KmPropertyExtensionVisitor? {
        if (type != KlibPropertyExtensionVisitor.TYPE) return null
        return object : KlibPropertyExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.setExtension(
                    KlibMetadataProtoBuf.propertyAnnotation,
                    mutableListOf(annotation.writeAnnotation(c.strings).build())
                )
            }

            override fun visitGetterAnnotation(annotation: KmAnnotation) {
                proto.setExtension(
                    KlibMetadataProtoBuf.propertyGetterAnnotation,
                    mutableListOf(annotation.writeAnnotation(c.strings).build())
                )
            }

            override fun visitSetterAnnotation(annotation: KmAnnotation) {
                proto.setExtension(
                    KlibMetadataProtoBuf.propertySetterAnnotation,
                    mutableListOf(annotation.writeAnnotation(c.strings).build())
                )
            }
        }
    }

    override fun writeConstructorExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Constructor.Builder,
        c: WriteContext
    ): KmConstructorExtensionVisitor? {
        if (type != KlibConstructorExtensionVisitor.TYPE) return null
        return object : KlibConstructorExtensionVisitor() {

        }
    }

    override fun writeTypeParameterExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.TypeParameter.Builder,
        c: WriteContext
    ): KmTypeParameterExtensionVisitor? {
        if (type != KlibTypeParameterExtensionVisitor.TYPE) return null
        return object : KlibTypeParameterExtensionVisitor() {

        }
    }

    override fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, c: WriteContext): KmTypeExtensionVisitor? {
        if (type != KlibTypeExtensionVisitor.TYPE) return null
        return object : KlibTypeExtensionVisitor() {

        }
    }

    override fun createClassExtension(): KmClassExtension =
        KlibClassExtension()

    override fun createPackageExtension(): KmPackageExtension =
        KlibPackageExtension()

    override fun createFunctionExtension(): KmFunctionExtension =
        KlibFunctionExtension()

    override fun createPropertyExtension(): KmPropertyExtension =
        KlibPropertyExtension()

    override fun createConstructorExtension(): KmConstructorExtension =
        KlibConstructorExtension()

    override fun createTypeParameterExtension(): KmTypeParameterExtension =
        KlibTypeParameterExtension()

    override fun createTypeExtension(): KmTypeExtension =
        KlibTypeExtension()
}