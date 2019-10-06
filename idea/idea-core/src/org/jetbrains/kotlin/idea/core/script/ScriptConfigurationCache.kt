/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptConfigurationFileAttributeCache
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import kotlin.script.experimental.api.valueOrNull

class CachedConfiguration(
    val file: VirtualFile,
    val result: ScriptCompilationConfigurationWrapper
) {
    val modificationStamp: Long = file.modificationStamp

    val isUpToDate
        get() = file.modificationStamp == modificationStamp
}

interface ScriptConfigurationCache {
    operator fun get(file: VirtualFile): CachedConfiguration?
    operator fun set(file: VirtualFile, configuration: ScriptCompilationConfigurationWrapper)

    fun all(): Collection<CachedConfiguration>
}

class ScriptCompositeCache(
    val project: Project,
    val memoryCache: ScriptConfigurationCache,
    val fileAttributeCache: ScriptConfigurationFileAttributeCache
): ScriptConfigurationCache {
    override fun get(file: VirtualFile): CachedConfiguration? {
        val fromMemory = memoryCache[file]
        if (fromMemory != null) return fromMemory

        val fromAttributes = fileAttributeCache.load(file) ?: return null
        memoryCache[file] = fromAttributes
        return CachedConfiguration(file, fromAttributes)
    }

    override fun set(file: VirtualFile, configuration: ScriptCompilationConfigurationWrapper) {
        memoryCache[file] = configuration

        debug(file) { "configuration saved to file attributes: $configuration" }
        fileAttributeCache.save(file, configuration)
    }

    override fun all(): Collection<CachedConfiguration> = memoryCache.all()
}