/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.AbstractScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

/**
 * Configuration may be loaded from [memoryCache] or from [fileAttributeCache], on [memoryCache] miss.
 * On load from [fileAttributeCache] it also will be stored in [memoryCache].
 *
 * [all] will return only [memoryCache]-ed configuration, so each loading from [fileAttributeCache]
 * should call [AbstractScriptConfigurationManager.clearClassRootsCaches]. This should be done in the [afterLoadFromFs]
 */
abstract class ScriptConfigurationCompositeCache(val project: Project) :
    ScriptConfigurationCache {
    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    private val memoryCache = BlockingSLRUMap<VirtualFile, CachedConfiguration>(MAX_SCRIPTS_CACHED)
    private val fileAttributeCache = ScriptConfigurationFileAttributeCache(project)

    override operator fun get(file: VirtualFile): CachedConfiguration? {
        val fromMemory = memoryCache.get(file)
        if (fromMemory != null) return fromMemory

        val fromAttributes = fileAttributeCache.load(file) ?: return null

        memoryCache.replace(
            file,
            CachedConfiguration(
                file,
                fromAttributes,
                0 // to reload on first request
            )
        )

        afterLoadFromFs()

        return CachedConfiguration(file, fromAttributes)
    }

    protected abstract fun afterLoadFromFs()

    override operator fun set(file: VirtualFile, configuration: ScriptCompilationConfigurationWrapper) {
        memoryCache.replace(
            file,
            CachedConfiguration(
                file,
                configuration
            )
        )

        debug(file) { "configuration saved to file attributes: $configuration" }
        fileAttributeCache.save(file, configuration)
    }

    override fun markOutOfDate(file: VirtualFile) {
        memoryCache.update(file) {
            it?.copy(modificationStamp = 0)
        }
    }

    override fun all(): Collection<CachedConfiguration> = memoryCache.getAll().map { it.value }
}