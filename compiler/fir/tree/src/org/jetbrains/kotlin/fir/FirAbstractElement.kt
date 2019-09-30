/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirPureAbstractElement : FirElement {
    @Suppress("UNCHECKED_CAST")
    open fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E> =
        accept(visitor, data) as CompositeTransformResult<E>
}

abstract class FirAbstractElement(
    final override val psi: PsiElement?
) : FirPureAbstractElement()