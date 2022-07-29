package org.worldcubeassociation.tnoodle.server.webscrambles.zip.model

import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.lang3.StringUtils
import org.worldcubeassociation.tnoodle.server.webscrambles.zip.util.StringUtil.toFileSafeString
import java.io.ByteArrayOutputStream

class ZipArchive(private val entries: List<ZipNode>) {
    val allFiles: List<File>
        get() = Folder.flattenFiles(entries)

    private val zippingCache by lazy { directCompress(null) }

    fun compress(password: String? = null): ByteArray {
        if (password == null) {
            return zippingCache
        }

        return directCompress(password)
    }

    fun directCompress(password: String?): ByteArray {
        val baosZip = ByteArrayOutputStream()

        val zipOut = ZipOutputStream(baosZip, password?.toCharArray())

        val usePassword = password != null
        val parameters = defaultZipParameters(usePassword)

        for (file in allFiles) {
            parameters.fileNameInZip = file.path.stripDiacritics()

            zipOut.putNextEntry(parameters)
            zipOut.write(file.content)

            zipOut.closeEntry()
        }

        zipOut.close()

        return baosZip.toByteArray()
    }

    companion object {
        private fun String.stripDiacritics() = StringUtils.stripAccents(this)

        private fun defaultZipParameters(useEncryption: Boolean = false) = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            compressionLevel = CompressionLevel.NORMAL

            if (useEncryption) {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.ZIP_STANDARD
            }
        }

        private tailrec fun String.toUniqueTitle(seenTitles: Set<String>, suffixSalt: Int = 0): String {
            val suffixedTitle = "$this (${suffixSalt})"
                .takeUnless { suffixSalt == 0 } ?: this

            if (suffixedTitle !in seenTitles) {
                return suffixedTitle
            }

            return toUniqueTitle(seenTitles, suffixSalt + 1)
        }

        fun <T> List<T>.withUniqueTitles(titleGen: (T) -> String = { it.toString() }): Map<String, T> {
            return fold(emptyMap()) { acc, req ->
                val fileTitle = titleGen(req).toFileSafeString()
                val safeTitle = fileTitle.toUniqueTitle(acc.keys)

                acc + (safeTitle to req)
            }
        }
    }
}
