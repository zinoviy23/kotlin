/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.*

abstract class AbstractFirTreeImplementationConfigurator(private val treeBuilder: AbstractFirTreeBuilder) {
    private val elementsWithImpl = mutableSetOf<Element>()

    fun impl(element: Element, name: String? = null, config: ImplementationContext.() -> Unit = {}) {
        val implementation = if (name == null) {
            element.defaultImplementation
        } else {
            element.customImplementations.firstOrNull { it.name == name }
        } ?: Implementation(element, name)
        val context = ImplementationContext(implementation)
        context.apply(config)
        elementsWithImpl += element
    }

    fun generateDefaultImplementations(builder: AbstractFirTreeBuilder) {
        collectLeafsWithoutImplementation(builder).forEach {
            impl(it)
        }
    }

    private fun collectLeafsWithoutImplementation(builder: AbstractFirTreeBuilder): Set<Element> {
        val elements = builder.elements.toMutableSet()
        builder.elements.forEach {
            elements.removeAll(it.parents)
        }
        elements.removeAll(elementsWithImpl)
        return elements
    }

    inner class ImplementationContext(private val implementation: Implementation) {
        fun sep(vararg names: String, needTransformOthers: Boolean = false) {
            val fields = names.map { name ->
                getField(name).also { require(it is FirField || it is FieldList) }
            }.toTypedArray()
            sep(*fields, needRestTransform = needTransformOthers)
        }

        fun sep(vararg fields: Field, needRestTransform: Boolean = false) {
            implementation.separateTransformations += fields
            implementation.needRestTransforms = needRestTransform
        }


        private fun getField(name: String): Field {
            val result = implementation.element.allFields.firstOrNull { it.name == name }
            requireNotNull(result) {
                "Field \"$name\" not found in fields of ${implementation.element}\nExisting fields:\n" +
                        implementation.element.allFields.joinToString(separator = "\n  ", prefix = "  ") { it.name }
            }
            return result
        }

        fun lateinit(vararg fields: String) {
            for (field in fields) {
                val realField = getField(field)
                require(realField !is FieldList)
                realField.isLateinit = true
            }
        }

        fun isVal(vararg fields: String) {
            fields.forEach {
                val field = getField(it)
                require(field !is FirField)
                field.isVal = true
            }
        }

        fun defaultReceivers() {
            defaultNull("explicitReceiver")
            default("dispatchReceiver", "FirNoReceiverExpression")
            default("extensionReceiver", "FirNoReceiverExpression")
        }

        fun default(field: String, value: String) {
            default(field) {
                this.value = value
            }
        }

        fun defaultImplicitTypeRef() {
            default("typeRef", "FirImplicitTypeRefImpl(null)")
        }

        fun defaultNull(field: String) {
            default(field, "null")
            require(getField(field).nullable) {
                "$field is not nullable field"
            }
        }

        fun defaultEmptyList(field: String) {
            default(field) {
                value = "emptyList()"
                withGetter = true
            }
            require(getField(field) is FieldList) {
                "$field is list field"
            }
        }

        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(getField(field)).apply(init).applyConfiguration()
        }

        fun delegateFields(fields: List<String>, delegate: String) {
            for (field in fields) {
                default(field) {
                    this.delegate = delegate
                }
            }
        }

        inner class DefaultValueContext(private val field: Field) {
            var value: String? = null

            var delegate: String? = null
                set(value) {
                    field = value
                    if (value != null) {
                        withGetter = true
                    }
                }
            var delegateCall: String? = null

            var isVal: Boolean = false
            var withGetter: Boolean = false
                set(value) {
                    field = value
                    if (value) {
                        isVal = true
                    }
                }

            fun applyConfiguration() {
                field.withGetter = withGetter
                require(!(field !is SimpleField && field.isVal))
                field.isVal = isVal
                when {
                    value != null -> field.defaultValue = value
                    delegate != null -> {
                        val actualDelegateField = getField(delegate!!)
                        val name = delegateCall ?: field.name
                        field.defaultValue = "${actualDelegateField.name}${actualDelegateField.call()}$name"
                    }
                }

            }
        }
    }
}