/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

// TODO: remove these checkers before 1.4 is released

object TrailingCommaChecker {
    fun check(trailingComma: PsiElement?, trace: BindingTrace, languageVersionSettings: LanguageVersionSettings) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.TrailingCommas) && trailingComma != null) {
            trace.report(Errors.TRAILING_COMMA_IS_NOT_SUPPORTED_YET.on(trailingComma))
        }
    }
}

object TrailingCommaDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration is KtCallableDeclaration) {
            TrailingCommaChecker.check(declaration.valueParameterList?.trailingComma, context.trace, context.languageVersionSettings)
        }
    }
}

object TrailingCommaCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        when (val callElement = resolvedCall.call.callElement) {
            is KtArrayAccessExpression -> TrailingCommaChecker.check(
                callElement.trailingComma,
                context.trace,
                context.languageVersionSettings
            )
            is KtCollectionLiteralExpression -> TrailingCommaChecker.check(
                callElement.trailingComma,
                context.trace,
                context.languageVersionSettings
            )
            is KtWhenExpression -> {
                if (callElement.subjectExpression != null) {
                    callElement.entries.forEach { whenEntry ->
                        TrailingCommaChecker.check(whenEntry.trailingComma, context.trace, context.languageVersionSettings)
                    }
                }
            }
            else -> TrailingCommaChecker.check(
                resolvedCall.call.valueArgumentList?.trailingComma,
                context.trace,
                context.languageVersionSettings
            )
        }
    }
}
