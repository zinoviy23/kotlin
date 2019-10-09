/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.common

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.*

val KmFunction.commonExtensions: CommonFunctionExtension
    get() = visitExtensions(CommonFunctionExtensionVisitor.TYPE) as CommonFunctionExtension

val KmClass.commonExtensions: CommonClassExtension
    get() = visitExtensions(CommonClassExtensionVisitor.TYPE) as CommonClassExtension

val KmType.commonExtensions: CommonTypeExtension
    get() = visitExtensions(CommonTypeExtensionVisitor.TYPE) as CommonTypeExtension

val KmProperty.commonExtensions: CommonPropertyExtension
    get() = visitExtensions(CommonPropertyExtensionVisitor.TYPE) as CommonPropertyExtension

val KmConstructor.commonExtensions: CommonConstructorExtension
    get() = visitExtensions(CommonConstructorExtensionVisitor.TYPE) as CommonConstructorExtension

val KmTypeParameter.commonExtensions: CommonTypeParameterExtension
    get() = visitExtensions(CommonTypeParameterExtensionVisitor.TYPE) as CommonTypeParameterExtension

val KmPackage.commonExtensions: CommonPackageExtension
    get() = visitExtensions(CommonPackageExtensionVisitor.TYPE) as CommonPackageExtension

class CommonFunctionExtension : CommonFunctionExtensionVisitor(), KmFunctionExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun accept(visitor: KmFunctionExtensionVisitor) {
        require(visitor is CommonFunctionExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
    }
}

class CommonClassExtension : CommonClassExtensionVisitor(), KmClassExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun accept(visitor: KmClassExtensionVisitor) {
        require(visitor is CommonClassExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
    }
}

class CommonTypeExtension : CommonTypeExtensionVisitor(), KmTypeExtension {
    override fun accept(visitor: KmTypeExtensionVisitor) {
        require(visitor is CommonTypeExtensionVisitor)
    }
}

class CommonPropertyExtension : CommonPropertyExtensionVisitor(), KmPropertyExtension {

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
        require(visitor is CommonPropertyExtensionVisitor)
    }
}

class CommonConstructorExtension : CommonConstructorExtensionVisitor(), KmConstructorExtension {
    override fun accept(visitor: KmConstructorExtensionVisitor) {
        require(visitor is CommonConstructorExtensionVisitor)
    }
}

class CommonTypeParameterExtension : CommonTypeParameterExtensionVisitor(), KmTypeParameterExtension {
    override fun accept(visitor: KmTypeParameterExtensionVisitor) {
        require(visitor is CommonTypeParameterExtensionVisitor)
    }
}

class CommonPackageExtension : CommonPackageExtensionVisitor(), KmPackageExtension {
    override fun accept(visitor: KmPackageExtensionVisitor) {
        require(visitor is CommonPackageExtensionVisitor)
    }
}