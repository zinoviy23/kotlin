/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.*

val KmFunction.klibExtensions: KlibFunctionExtension
    get() = visitExtensions(KlibFunctionExtensionVisitor.TYPE) as KlibFunctionExtension

val KmClass.klibExtensions: KlibClassExtension
    get() = visitExtensions(KlibClassExtensionVisitor.TYPE) as KlibClassExtension

val KmType.klibExtensions: KlibTypeExtension
    get() = visitExtensions(KlibTypeExtensionVisitor.TYPE) as KlibTypeExtension

val KmProperty.klibExtensions: KlibPropertyExtension
    get() = visitExtensions(KlibPropertyExtensionVisitor.TYPE) as KlibPropertyExtension

val KmConstructor.klibExtensions: KlibConstructorExtension
    get() = visitExtensions(KlibConstructorExtensionVisitor.TYPE) as KlibConstructorExtension

val KmTypeParameter.klibExtensions: KlibTypeParameterExtension
    get() = visitExtensions(KlibTypeParameterExtensionVisitor.TYPE) as KlibTypeParameterExtension

val KmPackage.klibExtensions: KlibPackageExtension
    get() = visitExtensions(KlibPackageExtensionVisitor.TYPE) as KlibPackageExtension

class KlibFunctionExtension : KlibFunctionExtensionVisitor(), KmFunctionExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun accept(visitor: KmFunctionExtensionVisitor) {
        require(visitor is KlibFunctionExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
    }
}

class KlibClassExtension : KlibClassExtensionVisitor(), KmClassExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun accept(visitor: KmClassExtensionVisitor) {
        require(visitor is KlibClassExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
    }
}

class KlibTypeExtension : KlibTypeExtensionVisitor(), KmTypeExtension {
    override fun accept(visitor: KmTypeExtensionVisitor) {
        require(visitor is KlibTypeExtensionVisitor)
    }
}

class KlibPropertyExtension : KlibPropertyExtensionVisitor(), KmPropertyExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()
    val getterAnnotations: MutableList<KmAnnotation> = mutableListOf()
    val setterAnnotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun visitGetterAnnotation(annotation: KmAnnotation) {
        getterAnnotations += annotation
    }

    override fun visitSetterAnnotation(annotation: KmAnnotation) {
        setterAnnotations += annotation
    }

    override fun accept(visitor: KmPropertyExtensionVisitor) {
        require(visitor is KlibPropertyExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
        getterAnnotations.forEach(visitor::visitGetterAnnotation)
        setterAnnotations.forEach(visitor::visitSetterAnnotation)
    }
}

class KlibConstructorExtension : KlibConstructorExtensionVisitor(), KmConstructorExtension {
    override fun accept(visitor: KmConstructorExtensionVisitor) {
        require(visitor is KlibConstructorExtensionVisitor)
    }
}

class KlibTypeParameterExtension : KlibTypeParameterExtensionVisitor(), KmTypeParameterExtension {
    override fun accept(visitor: KmTypeParameterExtensionVisitor) {
        require(visitor is KlibTypeParameterExtensionVisitor)
    }
}

class KlibPackageExtension : KlibPackageExtensionVisitor(), KmPackageExtension {

    val classes: MutableList<KmClass> = mutableListOf()

    override fun visitClass(klass: KmClass) {
        classes += klass
    }

    override fun accept(visitor: KmPackageExtensionVisitor) {
        require(visitor is KlibPackageExtensionVisitor)
        classes.forEach(visitor::visitClass)
    }
}