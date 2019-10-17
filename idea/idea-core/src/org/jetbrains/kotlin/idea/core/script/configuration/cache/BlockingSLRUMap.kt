/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.util.containers.SLRUMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class BlockingSLRUMap<K, V>(val size: Int) {
    private val lock = ReentrantReadWriteLock()

    val cache = SLRUMap<K, V>(size, size)

    fun get(value: K): V? = lock.read {
        cache[value]
    }

    fun getOrPut(key: K, defaultValue: () -> V): V = lock.write {
        val value = cache.get(key)
        return if (value == null) {
            val answer = defaultValue()
            replace(key, answer)
            answer
        } else {
            value
        }
    }

    fun remove(file: K) = lock.write {
        cache.remove(file)
    }

    fun getAll(): Collection<Map.Entry<K, V>> = lock.read {
        cache.entrySet()
    }

    fun clear() = lock.write {
        cache.clear()
    }

    fun replace(file: K, value: V): V? = lock.write {
        val old = get(file)
        cache.put(file, value)
        old
    }
}