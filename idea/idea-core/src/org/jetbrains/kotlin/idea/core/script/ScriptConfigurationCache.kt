/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptConfigurationFileAttributeCache
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

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
    fun clear()

    //    fun setUpToDate(file: VirtualFile)

//    val scriptsDependenciesClasspathScopeCache: MutableMap<VirtualFile, GlobalSearchScope>
//    val scriptsSdksCache: MutableMap<VirtualFile, Sdk?>
//    val firstScriptSdk: Sdk?
//    val allSdks: List<Sdk>
//    val allNonIndexedSdks: List<Sdk>
//    val allDependenciesClassFiles: List<VirtualFile>
//    val allDependenciesSources: List<VirtualFile>
//    val allDependenciesClassFilesScope: GlobalSearchScope
//    val allDependenciesSourcesScope: GlobalSearchScope
//    fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean
//
}

class ScriptsConfiguration(
    val allConfigurations: List<CachedConfiguration>,
    val project: Project,
    val getActualConfiguration: (KtFile) -> ScriptCompilationConfigurationWrapper
) {
}