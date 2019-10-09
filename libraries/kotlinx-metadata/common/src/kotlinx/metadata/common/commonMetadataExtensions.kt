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
        if (type != CommonClassExtensionVisitor.TYPE) return null
        return object : CommonClassExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.setExtension(
                    KlibMetadataProtoBuf.classAnnotation,
                    mutableListOf(annotation.writeAnnotation(c.strings).build())
                )
            }
        }
    }

    override fun writePackageExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Package.Builder,
        c: WriteContext
    ): KmPackageExtensionVisitor? {
        if (type != CommonPackageExtensionVisitor.TYPE) return null
        return object : CommonPackageExtensionVisitor() {

        }
    }

    override fun writeFunctionExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Function.Builder,
        c: WriteContext
    ): KmFunctionExtensionVisitor? {
        if (type != CommonFunctionExtensionVisitor.TYPE) return null
        return object : CommonFunctionExtensionVisitor() {
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
        if (type != CommonPropertyExtensionVisitor.TYPE) return null
        return object : CommonPropertyExtensionVisitor() {
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
        if (type != CommonConstructorExtensionVisitor.TYPE) return null
        return object : CommonConstructorExtensionVisitor() {

        }
    }

    override fun writeTypeParameterExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.TypeParameter.Builder,
        c: WriteContext
    ): KmTypeParameterExtensionVisitor? {
        if (type != CommonTypeParameterExtensionVisitor.TYPE) return null
        return object : CommonTypeParameterExtensionVisitor() {

        }
    }

    override fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, c: WriteContext): KmTypeExtensionVisitor? {
        if (type != CommonTypeExtensionVisitor.TYPE) return null
        return object : CommonTypeExtensionVisitor() {

        }
    }

    override fun createClassExtension(): KmClassExtension =
        CommonClassExtension()

    override fun createPackageExtension(): KmPackageExtension =
        CommonPackageExtension()

    override fun createFunctionExtension(): KmFunctionExtension =
        CommonFunctionExtension()

    override fun createPropertyExtension(): KmPropertyExtension =
        CommonPropertyExtension()

    override fun createConstructorExtension(): KmConstructorExtension =
        CommonConstructorExtension()

    override fun createTypeParameterExtension(): KmTypeParameterExtension =
        CommonTypeParameterExtension()

    override fun createTypeExtension(): KmTypeExtension =
        CommonTypeExtension()
}