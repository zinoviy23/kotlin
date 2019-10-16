/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.mockJDK

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

fun main() = removeInterfacesFromMockJdkClassfiles(
    mockJdkRuntimeJar = File("compiler/testData/mockJDK/jre/lib/rt.jar")
)

internal fun removeInterfacesFromMockJdkClassfiles(mockJdkRuntimeJar: File) {
    if (!mockJdkRuntimeJar.exists()) {
        throw AssertionError("$mockJdkRuntimeJar doesn't exist")
    }

    val tmpdir = FileUtil.createTempDirectory(
        File(System.getProperty("java.io.tmpdir")),
        "mockJdk",
        "",
        true
    )
    val copyJar = File(tmpdir, "rt.jar")
    FileUtil.copy(mockJdkRuntimeJar, copyJar)

    JarFile(mockJdkRuntimeJar).use { sourceJar ->
        JarOutputStream(FileOutputStream(copyJar))
            .use { targetJar ->
                transformJar(sourceJar, targetJar)
            }
    }

    FileUtil.copy(copyJar, mockJdkRuntimeJar)
    tmpdir.delete()
}

private fun transformJar(sourceJar: JarFile, targetJar: JarOutputStream) {
    val sourceEntries = sourceJar.entries().toList()
    for (entry in sourceEntries) {
        if (entry.name.endsWith(".class")) {
            val inputByteArray = sourceJar.getInputStream(entry).use { it.readBytes() }
            val classReader = ClassReader(inputByteArray)
            val classWriter = ClassWriter(classReader, 0) // Neither stack frames nor stack size changes

            classReader.accept(
                InterfacesFilter(classWriter),
                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES // Only implemented interfaces matter
            )

            targetJar.putNextEntry(ZipEntry(entry.name))
            targetJar.write(classWriter.toByteArray())

        } else {
            targetJar.putNextEntry(ZipEntry(entry.name))
            FileUtil.copy(sourceJar.getInputStream(entry), targetJar)
        }
    }
}

private val missingSuperInterfaces = setOf(
    "java/io/Flushable"
)

internal class InterfacesFilter(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.API_VERSION, classVisitor) {
    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val allowedInterfaces = filterInterfaces(interfaces)
        super.visit(version, access, name, signature, superName, allowedInterfaces)
    }

    private fun filterInterfaces(oldInterfaces: Array<out String>?): Array<out String>? =
        oldInterfaces
            ?.filter { it !in missingSuperInterfaces }
            ?.toTypedArray()
}
