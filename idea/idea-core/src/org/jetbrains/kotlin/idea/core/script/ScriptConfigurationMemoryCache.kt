/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationMemoryCache.Companion.MAX_SCRIPTS_CACHED
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ScriptConfigurationMemoryCache internal constructor() : ScriptConfigurationCache {
    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    private val scriptDependenciesCache = BlockingSLRUMap<VirtualFile, CachedConfiguration>()

    override fun set(file: VirtualFile, configuration: ScriptCompilationConfigurationWrapper) {
        scriptDependenciesCache.replace(file, CachedConfiguration(file, configuration))
    }

    override fun all(): Collection<CachedConfiguration> = scriptDependenciesCache.getAll().map { it.value }

    override fun get(file: VirtualFile): CachedConfiguration? = scriptDependenciesCache.get(file)
}

private class BlockingSLRUMap<K, V> {
    private val lock = ReentrantReadWriteLock()

    val cache = SLRUMap<K, V>(
        MAX_SCRIPTS_CACHED,
        MAX_SCRIPTS_CACHED
    )

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