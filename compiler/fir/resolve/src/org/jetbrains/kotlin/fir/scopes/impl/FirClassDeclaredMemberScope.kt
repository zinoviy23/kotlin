/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.memberScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirClassDeclaredMemberScopeProvider : FirSessionComponent {

    private val declaredMemberCache = mutableMapOf<FirRegularClass, FirClassDeclaredMemberScope>()
    private val nestedClassifierCache = mutableMapOf<FirRegularClass, FirNestedClassifierScope>()

    fun declaredMemberScope(klass: FirRegularClass): FirClassDeclaredMemberScope {
        return declaredMemberCache.getOrPut(klass) {
            FirClassDeclaredMemberScope(klass)
        }
    }

    fun nestedClassifierScope(klass: FirRegularClass): FirNestedClassifierScope {
        return nestedClassifierCache.getOrPut(klass) {
            FirNestedClassifierScope(klass)
        }
    }
}

fun declaredMemberScope(klass: FirRegularClass): FirClassDeclaredMemberScope {
    return klass
        .session
        .memberScopeProvider
        .declaredMemberScope(klass)
}

fun nestedClassifierScope(klass: FirRegularClass): FirNestedClassifierScope {
    return klass
        .session
        .memberScopeProvider
        .nestedClassifierScope(klass)
}

fun nestedClassifierScope(classId: ClassId, session: FirSession): FirLazyNestedClassifierScope {
    return FirLazyNestedClassifierScope(classId, session)
}

class FirClassDeclaredMemberScope(klass: FirRegularClass) : FirScope() {
    private val nestedClassifierScope = nestedClassifierScope(klass)

    private val callablesIndex: Map<Name, List<FirCallableSymbol<*>>> = run {
        val result = mutableMapOf<Name, MutableList<FirCallableSymbol<*>>>()
        for (declaration in klass.declarations) {
            when (declaration) {
                is FirCallableMemberDeclaration<*> -> {
                    val name = if (declaration is FirConstructor) klass.name else declaration.name
                    result.getOrPut(name) { mutableListOf() } += declaration.symbol
                }
                is FirRegularClass -> {
                    for (nestedDeclaration in declaration.declarations) {
                        if (nestedDeclaration is FirConstructor) {
                            result.getOrPut(declaration.name) { mutableListOf() } += nestedDeclaration.symbol
                        }
                    }
                }
            }
        }
        result
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val symbols = callablesIndex[name] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is FirFunctionSymbol<*> && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val symbols = callablesIndex[name] ?: emptyList()
        for (symbol in symbols) {
            if (symbol is FirVariableSymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (FirClassifierSymbol<*>) -> ProcessorAction
    ): ProcessorAction = nestedClassifierScope.processClassifiersByName(name, position, processor)
}
