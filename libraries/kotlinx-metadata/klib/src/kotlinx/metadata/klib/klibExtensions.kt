/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.*

val KmFunction.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

val KmClass.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

val KmProperty.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

val KmProperty.setterAnnotations: MutableList<KmAnnotation>
    get() = klibExtensions.setterAnnotations

val KmProperty.getterAnnotations: MutableList<KmAnnotation>
    get() = klibExtensions.getterAnnotations

val KmPackage.classes: MutableList<KmClass>
    get() = klibExtensions.classes