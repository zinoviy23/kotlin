/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.common

import kotlinx.metadata.*

abstract class CommonFunctionExtensionVisitor : KmFunctionExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(CommonFunctionExtensionVisitor::class)
    }

    abstract fun visitAnnotation(annotation: KmAnnotation)
}

abstract class CommonClassExtensionVisitor : KmClassExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(CommonClassExtensionVisitor::class)
    }
}

abstract class CommonTypeExtensionVisitor : KmTypeExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(CommonTypeExtensionVisitor::class)
    }
}

abstract class CommonPropertyExtensionVisitor : KmPropertyExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(CommonPropertyExtensionVisitor::class)
    }
}