/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.konanCommonLibraryPath
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import java.io.File
import java.nio.file.Files.isDirectory
import java.nio.file.Files.list
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.system.exitProcess
import org.jetbrains.kotlin.konan.file.File as KFile

fun main(args: Array<String>) {
    if (args.isEmpty()) printUsageAndExit()

    val parsedArgs = parseArgs(args)

    val repository = parsedArgs["repository"]?.firstOrNull() ?: printUsageAndExit("repository not specified")

    val targets = with(HostManager()) {
        val targetNames = parsedArgs["target"]?.toSet() ?: printUsageAndExit("no targets specified")
        targetNames.map { targetName ->
            targets[targetName] ?: printUsageAndExit("unknown target name: $targetName")
        }
    }

    val output = parsedArgs["output"]?.firstOrNull() ?: printUsageAndExit("output not specified")

    execute(
        repository = File(repository),
        targets = targets,
        output = File(output)
    )
}

private fun parseArgs(args: Array<String>): Map<String, List<String>> {
    val commandLine = mutableMapOf<String, MutableList<String>>()
    for (index in args.indices step 2) {
        val key = args[index]
        if (key[0] != '-') printUsageAndExit("Expected a flag with initial dash: $key")
        if (index + 1 == args.size) printUsageAndExit("Expected a value after $key")
        val value = args[index + 1]
        commandLine.computeIfAbsent(key) { mutableListOf() }.add(value)
    }
    return commandLine
}

private fun printErrorAndExit(errorMessage: String): Nothing {
    println("Error: $errorMessage")
    println()

    exitProcess(1)
}

private fun printUsageAndExit(errorMessage: String? = null): Nothing {
    if (errorMessage != null) {
        println("Error: $errorMessage")
        println()
    }

    println("Usage: commonizer <options>")
    println("where possible options include:")
    println("\t-repository <path>\tWork with the specified Kotlin/Native repository")
    println("\t-target <name>\t\tAdd hardware target to commonization")
    println("\t-output <path>\t\tDestination of commonized KLIBs")
    println()

    exitProcess(if (errorMessage != null) 1 else 0)
}

private fun execute(
    repository: File,
    targets: List<KonanTarget>,
    output: File
) {
    when {
        !repository.isDirectory -> printErrorAndExit("repository does not exist: $repository")
        targets.size < 2 -> printUsageAndExit("too few targets specified: $targets")
        !output.exists() -> output.mkdirs()
        !output.isDirectory -> printErrorAndExit("output already exists: $output")
        output.walkTopDown().any() -> printErrorAndExit("output is not empty: $output")
    }

    val stdlibPath = repository.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME)).toPath()
    val stdlib = createLibrary(stdlibPath)

    val librariesByTargets = targets.map { target ->
        val platformLibsPath = repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
            .resolve(target.name)
            .toPath()

        val platformLibs = platformLibsPath.takeIf { isDirectory(it) }
            ?.let { list(it).collect(Collectors.toList()) }
            ?.takeIf { it.isNotEmpty() }
            ?.map { createLibrary(it) }
            ?: printErrorAndExit("no platform libraries found for target $target in $platformLibsPath")

        target to listOf(stdlib) + platformLibs
    }.toMap()


}

private fun createLibrary(path: Path): KotlinLibrary {
    if (!isDirectory(path)) printErrorAndExit("library not found: $path")
    return createKotlinLibrary(KFile(path))
}
