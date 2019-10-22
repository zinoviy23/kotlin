/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirPartialBodyResolveTransformer(
    val mainTransformer: FirMainBodyResolveTransformer
) : FirAbstractBodyResolveTransformer(mainTransformer.transformerPhase) {

    final override inline var _container: FirDeclaration?
        get() = mainTransformer._container
        set(value) {
            mainTransformer._container = value
        }
    final override inline val dataFlowAnalyzer: FirDataFlowAnalyzer get() = mainTransformer.dataFlowAnalyzer
    final override inline val localScopes: MutableList<FirLocalScope> get() = mainTransformer.localScopes
    final override inline val callCompleter: FirCallCompleter get() = mainTransformer.callCompleter
    final override inline val session: FirSession get() = mainTransformer.session
    final override inline val returnTypeCalculator: ReturnTypeCalculator get() = mainTransformer.returnTypeCalculator
    final override inline val implicitReceiverStack: ImplicitReceiverStack get() = mainTransformer.implicitReceiverStack
    final override inline val noExpectedType: FirTypeRef get() = mainTransformer.noExpectedType
    final override inline val symbolProvider: FirSymbolProvider get() = mainTransformer.symbolProvider
    final override inline val file: FirFile get() = mainTransformer.file
    final override inline val inferenceComponents: InferenceComponents get() = mainTransformer.inferenceComponents
    final override inline val resolutionStageRunner: ResolutionStageRunner get() = mainTransformer.resolutionStageRunner
    final override inline val scopeSession: ScopeSession get() = mainTransformer.scopeSession
    final override inline val samResolver: FirSamResolver get() = mainTransformer.samResolver
    final override inline val topLevelScopes: MutableList<FirScope> get() = mainTransformer.topLevelScopes
    override var implicitTypeOnly: Boolean
        get() = mainTransformer.implicitTypeOnly
        set(value) {
            mainTransformer.implicitTypeOnly = value
        }

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        return element.transform(mainTransformer, data)
    }


}