/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.name.ClassId

class FirTypeResolverImpl(private val session: FirSession) : FirTypeResolver {

    private val symbolProvider by lazy {
        session.firSymbolProvider
    }

    private data class ClassIdInSession(val session: FirSession, val id: ClassId)

    private val implicitBuiltinTypeSymbols = mutableMapOf<ClassIdInSession, FirClassLikeSymbol<*>>()

    // TODO: get rid of session used here, and may be also of the cache above (see KT-30275)
    private fun resolveBuiltInQualified(id: ClassId, session: FirSession): FirClassLikeSymbol<*> {
        val nameInSession = ClassIdInSession(session, id)
        return implicitBuiltinTypeSymbols.getOrPut(nameInSession) {
            symbolProvider.getClassLikeSymbolByFqName(id)!!
        }
    }

    override fun resolveToSymbol(
        typeRef: FirTypeRef,
        scope: FirScope
    ): FirClassifierSymbol<*>? {
        return when (typeRef) {
            is FirResolvedTypeRef -> typeRef.coneTypeSafe<ConeLookupTagBasedType>()?.lookupTag?.let(symbolProvider::getSymbolByLookupTag)
            is FirUserTypeRef -> {

                val qualifierResolver = FirQualifierResolver.getInstance(session)

                var resolvedSymbol: FirClassifierSymbol<*>? = null
                scope.processClassifiersByName(typeRef.qualifier.first().name) { symbol ->
                    resolvedSymbol = when (symbol) {
                        is FirClassLikeSymbol<*> -> {
                            if (typeRef.qualifier.size == 1) {
                                symbol
                            } else {
                                qualifierResolver.resolveSymbolWithPrefix(typeRef.qualifier, symbol.classId)
                            }
                        }
                        is FirTypeParameterSymbol -> {
                            assert(typeRef.qualifier.size == 1)
                            symbol
                        }
                        else -> error("!")
                    }
                    if (resolvedSymbol == null) ProcessorAction.NEXT else ProcessorAction.STOP
                }

                // TODO: Imports
                resolvedSymbol ?: qualifierResolver.resolveSymbol(typeRef.qualifier)
            }
            is FirImplicitBuiltinTypeRef -> {
                resolveBuiltInQualified(typeRef.id, session)
            }
            else -> null
        }
    }

    override fun resolveUserType(typeRef: FirUserTypeRef, symbol: FirClassifierSymbol<*>?, scope: FirScope): ConeKotlinType {
        symbol ?: return ConeKotlinErrorType("Symbol not found, for `${typeRef.render()}`")
        return symbol.constructType(typeRef.qualifier, typeRef.isMarkedNullable)
    }


    private fun createFunctionalType(typeRef: FirFunctionTypeRef): ConeClassType {
        val parameters =
            listOfNotNull((typeRef.receiverTypeRef as FirResolvedTypeRef?)?.type) +
                    typeRef.valueParameters.map { it.returnTypeRef.coneTypeUnsafe<ConeKotlinType>() } +
                    listOf(typeRef.returnTypeRef.coneTypeUnsafe())
        return ConeClassTypeImpl(
            resolveBuiltInQualified(KotlinBuiltIns.getFunctionClassId(typeRef.parametersCount), session).toLookupTag(),
            parameters.toTypedArray(),
            typeRef.isMarkedNullable
        )
    }

    override fun resolveType(
        typeRef: FirTypeRef,
        scope: FirScope
    ): ConeKotlinType {
        return when (typeRef) {
            is FirResolvedTypeRef -> typeRef.type
            is FirUserTypeRef -> {
                resolveUserType(typeRef, resolveToSymbol(typeRef, scope), scope)
            }
            is FirErrorTypeRef -> {
                ConeKotlinErrorType(typeRef.reason)
            }
            is FirFunctionTypeRef -> {
                createFunctionalType(typeRef)
            }
            is FirImplicitBuiltinTypeRef -> {
                resolveToSymbol(typeRef, scope)!!.constructType(emptyList(), isNullable = false)
            }
            is FirDelegatedTypeRef -> {
                resolveType(typeRef.typeRef, scope)
            }
            is FirDynamicTypeRef -> {
                ConeKotlinErrorType("Not supported: ${typeRef::class.simpleName}")
            }
            else -> error("!")
        }
    }
}
