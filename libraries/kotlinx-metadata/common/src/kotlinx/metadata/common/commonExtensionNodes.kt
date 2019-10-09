/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.common

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.KmClassExtension
import kotlinx.metadata.impl.extensions.KmFunctionExtension
import kotlinx.metadata.impl.extensions.KmPropertyExtension
import kotlinx.metadata.impl.extensions.KmTypeExtension

val KmFunction.commonExtensions: CommonFunctionExtension
    get() = visitExtensions(CommonFunctionExtensionVisitor.TYPE) as CommonFunctionExtension

val KmClass.commonExtensions: CommonClassExtension
    get() = visitExtensions(CommonClassExtensionVisitor.TYPE) as CommonClassExtension

val KmType.commonExtensions: CommonTypeExtension
    get() = visitExtensions(CommonTypeExtensionVisitor.TYPE) as CommonTypeExtension

val KmProperty.commonExtensions: CommonPropertyExtension
    get() = visitExtensions(CommonPropertyExtensionVisitor.TYPE) as CommonPropertyExtension

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
    override fun accept(visitor: KmClassExtensionVisitor) {
        require(visitor is CommonClassExtensionVisitor)
    }
}

class CommonTypeExtension : CommonTypeExtensionVisitor(), KmTypeExtension {
    override fun accept(visitor: KmTypeExtensionVisitor) {
        require(visitor is CommonTypeExtensionVisitor)
    }
}

class CommonPropertyExtension : CommonPropertyExtensionVisitor(), KmPropertyExtension {
    override fun accept(visitor: KmPropertyExtensionVisitor) {
        require(visitor is CommonPropertyExtensionVisitor)
    }
}