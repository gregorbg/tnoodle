package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.utils.PdfMerger

class MergedPdf(val toMerge: List<PdfContent>) : BasePdfSheet() {
    override fun PdfDocument.writeContents() {
        val merger = PdfMerger(this)
            .setCloseSourceDocuments(true)

        for (content in toMerge) {
            val contentReader = PdfReader(content.render().inputStream())
            val contentDocument = PdfDocument(contentReader)

            merger.merge(contentDocument, 1, contentDocument.numberOfPages)
        }
    }
}
