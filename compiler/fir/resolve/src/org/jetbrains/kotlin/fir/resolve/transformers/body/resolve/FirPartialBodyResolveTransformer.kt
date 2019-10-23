/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult

abstract class FirPartialBodyResolveTransformer(
    val mainTransformer: FirMainBodyResolveTransformer
) : FirAbstractBodyResolveTransformer(mainTransformer.transformerPhase) {
    @Suppress("OVERRIDE_BY_INLINE")
    final override inline val components: BodyResolveTransformerComponents get() = mainTransformer.components

    override var implicitTypeOnly: Boolean
        get() = mainTransformer.implicitTypeOnly
        set(value) {
            mainTransformer.implicitTypeOnly = value
        }

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        return element.transform(mainTransformer, data)
    }
}