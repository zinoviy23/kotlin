/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractPhaseTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.phasedFir
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

abstract class FirAbstractBodyResolveTransformer(phase: FirResolvePhase) :
    FirAbstractPhaseTransformer<Any?>(phase), BodyResolveComponents {

    internal abstract val localScopes: MutableList<FirLocalScope>
    internal abstract val callCompleter: FirCallCompleter
    abstract var implicitTypeOnly: Boolean
        internal set
    internal abstract val dataFlowAnalyzer: FirDataFlowAnalyzer
    abstract internal var _container: FirDeclaration?
    final override var container: FirDeclaration
        get() = _container!!
        private set(value) {
            _container = value
        }

    abstract val topLevelScopes: MutableList<FirScope>

    final override val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() {
            val requiredPhase = transformerPhase.prev
            return phasedFir(session, requiredPhase)
        }

    protected inline fun <T> withScopeCleanup(scopes: MutableList<*>, crossinline l: () -> T): T {
        val sizeBefore = scopes.size
        return try {
            l()
        } finally {
            val size = scopes.size
            assert(size >= sizeBefore)
            repeat(size - sizeBefore) {
                scopes.let { it.removeAt(it.size - 1) }
            }
        }
    }

    internal inline fun <T> withFullBodyResolve(crossinline l: () -> T): T {
        if (!implicitTypeOnly) return l()
        implicitTypeOnly = false
        return try {
            l()
        } finally {
            implicitTypeOnly = true
        }
    }

    internal fun <T> withContainer(declaration: FirDeclaration, f: () -> T): T {
        val prevContainer = _container
        _container = declaration
        val result = f()
        _container = prevContainer
        return result
    }
}

