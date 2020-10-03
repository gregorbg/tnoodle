package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.EncryptionConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import java.io.ByteArrayOutputStream

abstract class BasePdfSheet : PdfContent {
    private val renderingCache by lazy { directRender(null) }

    override fun render(password: String?): ByteArray {
        if (password == null) {
            return renderingCache
        }

        return directRender(password)
    }

    open fun openDocument(writer: PdfWriter) = PdfDocument(writer)

    private fun directRender(password: String?): ByteArray {
        val pdfBytes = ByteArrayOutputStream()
        val properties = WriterProperties()

        if (password != null) {
            properties.setStandardEncryption(
                password.toByteArray(),
                password.toByteArray(),
                EncryptionConstants.ALLOW_PRINTING,
                EncryptionConstants.STANDARD_ENCRYPTION_128
            )
        }

        val docWriter = PdfWriter(pdfBytes, properties)
        val document = openDocument(docWriter)

        document.writeContents()

        document.close()

        return this.finalise(pdfBytes, password)
    }

    abstract fun PdfDocument.writeContents()

    open fun finalise(processedBytes: ByteArrayOutputStream, password: String?): ByteArray = processedBytes.toByteArray()

    companion object {
        val PAGE_SIZE = PageSize.LETTER
    }
}
