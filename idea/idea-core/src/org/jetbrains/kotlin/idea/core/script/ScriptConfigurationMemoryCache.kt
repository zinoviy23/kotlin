/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.ProjectTopics
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationMemoryCache.Companion.MAX_SCRIPTS_CACHED
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

class ScriptConfigurationMemoryCache internal constructor(project: Project) : ScriptConfigurationCache(project) {
    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    private val scriptDependenciesCache = SLRUCacheWithLock<VirtualFile, ScriptCompilationConfigurationWrapper>()
    private val scriptsModificationStampsCache = SLRUCacheWithLock<VirtualFile, Long>()

    override val allConfigurations: List<CachedConfiguration>
        get() = scriptDependenciesCache.getAll().map {
            CachedConfiguration(it.key, it.value, scriptsModificationStampsCache.get(it.key) ?: 0)
        }

    override fun get(file: VirtualFile): CachedConfiguration? =
        CachedConfiguration(file, scriptDependenciesCache[file], scriptsModificationStampsCache[file])

    override fun getCachedConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? = scriptDependenciesCache.get(file)

    override fun setUpToDate(file: VirtualFile) {
        scriptsModificationStampsCache.replace(file, file.modificationStamp)
    }

    override fun replaceConfiguration(file: VirtualFile, new: ScriptCompilationConfigurationWrapper) {
        scriptDependenciesCache.replace(file, new)
    }

    override fun clear(): List<VirtualFile> {
        val files = scriptDependenciesCache.getAll().map { it.key }
        scriptDependenciesCache.clear()
        scriptsModificationStampsCache.clear()
        return files
    }
}

private class SLRUCacheWithLock<K, V> {
    private val lock = ReentrantReadWriteLock()

    val cache = SLRUMap<K, V>(
        MAX_SCRIPTS_CACHED,
        MAX_SCRIPTS_CACHED
    )

    fun get(value: K): V? = lock.write {
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

    fun getAll(): Collection<Map.Entry<K, V>> = lock.write {
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