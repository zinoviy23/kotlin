/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.resolve.calls.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowInferenceContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirMainBodyResolveTransformer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ErrorTypeConstructor
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate

/*
open class FirBodyResolveTransformer(
    final override val session: FirSession,
    phase: FirResolvePhase,
    implicitTypeOnly: Boolean,
    final override val scopeSession: ScopeSession = ScopeSession()
) : FirAbstractPhaseTransformer<Any?>(phase), BodyResolveComponents {
    var implicitTypeOnly: Boolean = implicitTypeOnly
        private set

    final override val returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorWithJump(session, scopeSession)
    final override val noExpectedType = FirImplicitTypeRefImpl(null)
    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes

    final override val symbolProvider = session.firSymbolProvider

    private var packageFqName = FqName.ROOT
    final override lateinit var file: FirFile
        private set

    private var _container: FirDeclaration? = null
    final override var container: FirDeclaration
        get() = _container!!
        private set(value) {
            _container = value
        }

    private val localScopes = mutableListOf<FirLocalScope>()
    private val topLevelScopes = mutableListOf<FirScope>()
    final override val implicitReceiverStack: ImplicitReceiverStackImpl = ImplicitReceiverStackImpl()
    final override val inferenceComponents = inferenceComponents(session, returnTypeCalculator, scopeSession)
    final override val samResolver: FirSamResolver = FirSamResolverImpl(session, scopeSession)

    private var primaryConstructorParametersScope: FirLocalScope? = null

    private val callCompleter: FirCallCompleter = FirCallCompleter(this)

    final override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner(inferenceComponents)
    private val callResolver: FirCallResolver = FirCallResolver(
        this,
        topLevelScopes,
        localScopes,
        implicitReceiverStack,
        qualifiedResolver
    )

    private val syntheticCallGenerator: FirSyntheticCallGenerator = FirSyntheticCallGenerator(this)
    private val dataFlowAnalyzer: FirDataFlowAnalyzer = FirDataFlowAnalyzer(this)


    override val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() {
            val requiredPhase = transformerPhase.prev
            return phasedFir(session, requiredPhase)
        }



    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    // ----------------------- Util functions -----------------------


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

    private inline fun <T> withFullBodyResolve(crossinline l: () -> T): T {
        if (!implicitTypeOnly) return l()
        implicitTypeOnly = false
        return try {
            l()
        } finally {
            implicitTypeOnly = true
        }
    }



    private fun <T> withContainer(declaration: FirDeclaration, f: () -> T): T {
        val prevContainer = _container
        _container = declaration
        val result = f()
        _container = prevContainer
        return result
    }
}
*/

internal fun inferenceComponents(session: FirSession, returnTypeCalculator: ReturnTypeCalculator, scopeSession: ScopeSession) =
    InferenceComponents(object : ConeInferenceContext, TypeSystemInferenceExtensionContextDelegate, DataFlowInferenceContext {
        override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
            // TODO: implement
            return null
        }

        override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker {
            TODO("not implemented")
        }

        override val session: FirSession
            get() = session

        override fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker {
            return this
        }

        override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
            require(this is ErrorTypeConstructor)
            return ConeClassErrorType(reason)
        }
    }, session, returnTypeCalculator, scopeSession)


class FirDesignatedBodyResolveTransformer(
    private val designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession = ScopeSession(),
    implicitTypeOnly: Boolean = true
) : FirMainBodyResolveTransformer(
    session,
    phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = implicitTypeOnly,
    scopeSession = scopeSession
) {
    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        if (designation.hasNext()) {
            designation.next().visitNoTransform(this, data)
            return element.compose()
        }
        return super.transformElement(element, data)
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: Any?): CompositeTransformResult<FirDeclaration> {
        return components.withContainer(declaration) {
            declaration.replaceResolvePhase(transformerPhase)
            transformElement(declaration, data)
        }
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirImplicitTypeBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    private val scopeSession = ScopeSession()

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val transformer = FirMainBodyResolveTransformer(
            file.session,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            implicitTypeOnly = true,
            scopeSession = scopeSession
        )
        return file.transform(transformer, null)
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    private val scopeSession = ScopeSession()

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        // Despite of real phase is EXPRESSIONS, we state IMPLICIT_TYPES here, because DECLARATIONS previous phase is OK for us
        val transformer = FirMainBodyResolveTransformer(
            file.session,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            implicitTypeOnly = false,
            scopeSession = scopeSession
        )
        return file.transform(transformer, null)
    }
}


inline fun <reified T : FirElement> FirBasedSymbol<*>.firUnsafe(): T {
    val fir = this.fir
    require(fir is T) {
        "Not an expected fir element type = ${T::class}, symbol = ${this}, fir = ${fir.renderWithType()}"
    }
    return fir
}

internal inline var FirExpression.resultType: FirTypeRef
    get() = typeRef
    set(type) {
        replaceTypeRef(type)
    }
