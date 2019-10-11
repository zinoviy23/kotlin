/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.impl.PackageWriter
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataStringTable
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf

// TODO:
//  1. Package should be split into fragments
class KlibPackageWriter(val stringTable: KlibMetadataStringTable) : PackageWriter(stringTable) {

    fun write(packageName: String): SerializedMetadata {
        val fragments = mutableListOf<List<ByteArray>>()
        val fragmentNames = mutableListOf<String>()
        val emptyPackages = mutableListOf<String>()

        val packageProto = t.build()

        val header = serializeHeader()
        return SerializedMetadata(TODO(), TODO(), TODO())
    }

    private fun buildFragment(
        packageProto: ProtoBuf.Package,
        fqName: String,
        isEmpty: Boolean
    ): ProtoBuf.PackageFragment {
        val (stringTableProto, nameTableProto) = stringTable.buildProto()
        return ProtoBuf.PackageFragment.newBuilder()
            .setPackage(packageProto)
            .setStrings(stringTableProto)
            .setQualifiedNames(nameTableProto)
            .also { packageFragment ->
                packageFragment.setExtension(KlibMetadataProtoBuf.fqName, fqName)
                packageFragment.setExtension(KlibMetadataProtoBuf.isEmpty, isEmpty)
            }
            .build()
    }

    private fun serializeHeader(): KlibMetadataProtoBuf.Header {
        val builder = KlibMetadataProtoBuf.Header.newBuilder()
        return builder.build()
    }
}