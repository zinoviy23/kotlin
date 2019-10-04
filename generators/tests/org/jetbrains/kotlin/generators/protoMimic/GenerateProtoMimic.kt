/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.protoMimic

import kotlinx.serialization.toUtf8Bytes
import java.nio.file.Files
import java.nio.file.Paths

private val HEADER = """
/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

"""

private data class Input(val protoFile: String,
                         val outputFile: String,
                         val className: String,
                         val packageName: String)

private val PROTO = listOf(Input(
    protoFile = "compiler/ir/serialization.common/src/KotlinIr.proto",
    outputFile = "compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrProtoReaderMimic.kt",
    className = "IrProtoReaderMimic",
    packageName = "org.jetbrains.kotlin.backend.common.serialization.protoMimic"
))

fun main() {
    for (input in PROTO) {
        val protoPath = Paths.get(input.protoFile)
        val protoModel = ProtoParser(Files.readAllLines(protoPath)).parse()

        val deserializer = HEADER + "package ${input.packageName}\n\n" + protoModel.createSimpleDeserializer(input.className)

        Files.write(Paths.get(input.outputFile), deserializer.toUtf8Bytes())
    }
}