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
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataStringTable
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.serialization.StringTable

val KmFunction.annotations: MutableList<KmAnnotation>
    get() = commonExtensions.annotations

val KmClass.annotations: MutableList<KmAnnotation>
    get() = commonExtensions.annotations

val KmProperty.annotations: MutableList<KmAnnotation>
    get() = commonExtensions.annotations

val KmProperty.setterAnnotations: MutableList<KmAnnotation>
    get() = commonExtensions.setterAnnotations

val KmProperty.getterAnnotations: MutableList<KmAnnotation>
    get() = commonExtensions.getterAnnotations