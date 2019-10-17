/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

class CachedConfiguration(
    val file: VirtualFile,
    val result: ScriptCompilationConfigurationWrapper,
    val modificationStamp: Long = file.modificationStamp
) {
    val isUpToDate
        get() = file.modificationStamp == modificationStamp
}

interface ScriptConfigurationCache {
    operator fun get(file: VirtualFile): CachedConfiguration?
    operator fun set(file: VirtualFile, configuration: ScriptCompilationConfigurationWrapper)

    fun all(): Collection<CachedConfiguration>
}

class ScriptCompositeCache(val project: Project): ScriptConfigurationCache {
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

        return CachedConfiguration(file, fromAttributes)
    }

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

    override fun all(): Collection<CachedConfiguration> = memoryCache.getAll().map { it.value }
}