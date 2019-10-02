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
    val cache = SLRUMap<K, V>(size, size)

    @Synchronized
    fun get(value: K): V? = cache[value]

    @Synchronized
    fun getOrPut(key: K, defaultValue: () -> V): V {
        val value = cache.get(key)
        return if (value == null) {
            val answer = defaultValue()
            replace(key, answer)
            answer
        } else {
            value
        }
    }

    @Synchronized
    fun remove(file: K) {
        cache.remove(file)
    }

    @Synchronized
    fun getAll(): Collection<Map.Entry<K, V>> = cache.entrySet()

    @Synchronized
    fun clear() {
        cache.clear()
    }

    @Synchronized
    fun replace(file: K, value: V): V? {
        val old = get(file)
        cache.put(file, value)
        return old
    }

    @Synchronized
    fun update(file: K, updater: (V?) -> V?) {
        val new = updater(get(file))
        if (new != null) {
            cache.put(file, new)
        }
    }
}