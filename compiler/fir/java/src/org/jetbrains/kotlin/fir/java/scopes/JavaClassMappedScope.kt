/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.java.computeJvmDescriptor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirSuperTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name

class JavaClassMappedScope(
    klass: FirRegularClass,
    session: FirSession,
    private val mappedKotlinScope: FirScope,
    declaredMemberScope: FirScope,
    private val whiteListSignaturesByName: Map<Name, List<String>>
) : JavaClassUseSiteMemberScope(klass, session, FirSuperTypeScope(session, listOf(mappedKotlinScope)), declaredMemberScope) {

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val whiteListSignatures = whiteListSignaturesByName[name]
            ?: return mappedKotlinScope.processFunctionsByName(name, processor)
        if (!declaredMemberScope.processFunctionsByName(name) { symbol ->
                val jvmSignature = symbol.fir.computeJvmDescriptor()
                if (jvmSignature !in whiteListSignatures) {
                    ProcessorAction.NEXT
                } else {
                    processor(symbol)
                }
            }
        ) return ProcessorAction.STOP

        return mappedKotlinScope.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        return mappedKotlinScope.processPropertiesByName(name, processor)
    }

    companion object {
        fun prepareSignatures(klass: FirRegularClass): Map<Name, List<String>> {
            val signaturePrefix = klass.symbol.classId.toString()
            val filteredSignatures = JvmBuiltInsSettings.WHITE_LIST_METHOD_SIGNATURES.filter { signature ->
                signature.startsWith(signaturePrefix)
            }.map { signature ->
                // +1 to delete dot before function name
                signature.substring(signaturePrefix.length + 1)
            }
            return filteredSignatures.groupBy { Name.identifier(it.substringBefore("(")) }
        }
    }
}