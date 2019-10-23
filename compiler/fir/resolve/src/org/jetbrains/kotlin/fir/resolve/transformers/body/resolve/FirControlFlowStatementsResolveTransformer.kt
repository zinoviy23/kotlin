/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.resolve.transformers.FirSyntheticCallGenerator
import org.jetbrains.kotlin.fir.resolve.transformers.FirWhenExhaustivenessTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.resultType
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle

class FirControlFlowStatementsResolveTransformer(mainTransformer: FirMainBodyResolveTransformer) :
    FirPartialBodyResolveTransformer(mainTransformer) {

    private val syntheticCallGenerator: FirSyntheticCallGenerator = FirSyntheticCallGenerator(components)
    private val whenExhaustivenessTransformer = FirWhenExhaustivenessTransformer(components)


    // ------------------------------- Loops -------------------------------

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: Any?): CompositeTransformResult<FirStatement> {
        return whileLoop.also(dataFlowAnalyzer::enterWhileLoop)
            .transformCondition(mainTransformer, data).also(dataFlowAnalyzer::exitWhileLoopCondition)
            .transformBlock(mainTransformer, data).also(dataFlowAnalyzer::exitWhileLoop)
            .transformOtherChildren(mainTransformer, data).compose()
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Any?): CompositeTransformResult<FirStatement> {
        return doWhileLoop.also(dataFlowAnalyzer::enterDoWhileLoop)
            .transformBlock(mainTransformer, data).also(dataFlowAnalyzer::enterDoWhileLoopCondition)
            .transformCondition(mainTransformer, data).also(dataFlowAnalyzer::exitDoWhileLoop)
            .transformOtherChildren(mainTransformer, data).compose()
    }

    // ------------------------------- When expressions -------------------------------

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): CompositeTransformResult<FirStatement> {
        if (whenExpression.calleeReference is FirResolvedCallableReference && whenExpression.resultType !is FirImplicitTypeRef) {
            return whenExpression.compose()
        }
        dataFlowAnalyzer.enterWhenExpression(whenExpression)
        return withScopeCleanup(localScopes) with@{
            if (whenExpression.subjectVariable != null) {
                localScopes += FirLocalScope()
            }
            @Suppress("NAME_SHADOWING")
            var whenExpression = whenExpression.transformSubject(mainTransformer, noExpectedType)

            when {
                whenExpression.branches.isEmpty() -> {
                }
                whenExpression.isOneBranch() -> {
                    whenExpression = whenExpression.transformBranches(mainTransformer, noExpectedType)
                    whenExpression.resultType = whenExpression.branches.first().result.resultType
                }
                else -> {
                    whenExpression = whenExpression.transformBranches(mainTransformer, null)

                    whenExpression = syntheticCallGenerator.generateCalleeForWhenExpression(whenExpression) ?: run {
                        dataFlowAnalyzer.exitWhenExpression(whenExpression)
                        whenExpression.resultType = FirErrorTypeRefImpl(null, "")
                        return@with whenExpression.compose()
                    }

                    val expectedTypeRef = data as FirTypeRef?
                    whenExpression = callCompleter.completeCall(whenExpression, expectedTypeRef)
                }
            }
            whenExpression = whenExpression.transformSingle(whenExhaustivenessTransformer, null)
            dataFlowAnalyzer.exitWhenExpression(whenExpression)
            whenExpression = whenExpression.replaceReturnTypeIfNotExhaustive()
            whenExpression.compose()
        }
    }

    private fun FirWhenExpression.replaceReturnTypeIfNotExhaustive(): FirWhenExpression {
        if (!isExhaustive) {
            resultType = resultType.resolvedTypeFromPrototype(session.builtinTypes.unitType.type)
        }
        return this
    }

    private fun FirWhenExpression.isOneBranch(): Boolean {
        if (branches.size == 1) return true
        if (branches.size > 2) return false
        val lastBranch = branches.last()
        return lastBranch.condition is FirElseIfTrueCondition && lastBranch.result is FirEmptyExpressionBlock
    }

    override fun transformWhenBranch(whenBranch: FirWhenBranch, data: Any?): CompositeTransformResult<FirWhenBranch> {
        return whenBranch.also { dataFlowAnalyzer.enterWhenBranchCondition(whenBranch) }
            .transformCondition(mainTransformer, data).also { dataFlowAnalyzer.exitWhenBranchCondition(it) }
            .transformResult(mainTransformer, data).also { dataFlowAnalyzer.exitWhenBranchResult(it) }
            .compose()
    }

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        val parentWhen = whenSubjectExpression.whenSubject.whenExpression
        val subjectType = parentWhen.subject?.resultType ?: parentWhen.subjectVariable?.returnTypeRef
        if (subjectType != null) {
            whenSubjectExpression.resultType = subjectType
        }
        return whenSubjectExpression.compose()
    }

    // ------------------------------- Try/catch expressions -------------------------------

    override fun transformTryExpression(tryExpression: FirTryExpression, data: Any?): CompositeTransformResult<FirStatement> {
        if (tryExpression.calleeReference is FirResolvedCallableReference && tryExpression.resultType !is FirImplicitTypeRef) {
            return tryExpression.compose()
        }

        dataFlowAnalyzer.enterTryExpression(tryExpression)
        tryExpression.transformTryBlock(mainTransformer, null)
        dataFlowAnalyzer.exitTryMainBlock(tryExpression)
        tryExpression.transformCatches(this, null)

        @Suppress("NAME_SHADOWING")
        var result = syntheticCallGenerator.generateCalleeForTryExpression(tryExpression)?.let {
            val expectedTypeRef = data as FirTypeRef?
            callCompleter.completeCall(it, expectedTypeRef)
        } ?: run {
            tryExpression.resultType = FirErrorTypeRefImpl(null, "")
            tryExpression
        }

        result = if (result.finallyBlock != null) {
            result.also(dataFlowAnalyzer::enterFinallyBlock)
                .transformFinallyBlock(mainTransformer, noExpectedType)
                .also(dataFlowAnalyzer::exitFinallyBlock)
        } else {
            result
        }
        dataFlowAnalyzer.exitTryExpression(result)
        return result.compose()
    }

    override fun transformCatch(catch: FirCatch, data: Any?): CompositeTransformResult<FirCatch> {
        dataFlowAnalyzer.enterCatchClause(catch)
        return withScopeCleanup(localScopes) {
            localScopes += FirLocalScope()
            catch.transformParameter(mainTransformer, noExpectedType)
            catch.transformBlock(mainTransformer, null)
        }.also { dataFlowAnalyzer.exitCatchClause(it) }.compose()
    }

    // ------------------------------- Jumps -------------------------------

    override fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: Any?): CompositeTransformResult<FirStatement> {
        val result = mainTransformer.transformExpression(jump, data)
        dataFlowAnalyzer.exitJump(jump)
        return result
    }

    override fun transformThrowExpression(throwExpression: FirThrowExpression, data: Any?): CompositeTransformResult<FirStatement> {
        return mainTransformer.transformExpression(throwExpression, data).also {
            dataFlowAnalyzer.exitThrowExceptionNode(it.single as FirThrowExpression)
        }
    }
}
