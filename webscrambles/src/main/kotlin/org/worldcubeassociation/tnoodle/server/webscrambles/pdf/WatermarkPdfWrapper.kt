package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.ColumnText
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class WatermarkPdfWrapper(val original: PdfContent, val creationTitle: String, val creationDate: LocalDate, val versionTag: String, val globalTitle: String?) : BasePdfSheet<PdfWriter>() {
    override fun openDocument() = Document(PAGE_SIZE, 0f, 0f, 75f, 75f)

    override fun Document.getWriter(bytes: ByteArrayOutputStream): PdfWriter = PdfWriter.getInstance(this, bytes)

    override fun PdfWriter.writeContents(document: Document) {
        val cb = directContent
        val pr = PdfReader(original.render())

        for (pageN in 1..pr.numberOfPages) {
            val page = getImportedPage(pr, pageN)

            document.newPage()
            cb.addTemplate(page, 0f, 0f)

            val rect = pr.getBoxSize(pageN, "art")

            // Header
            ColumnText.showTextAligned(cb,
                Element.ALIGN_LEFT, Phrase(creationDate.toString()),
                rect.left, rect.top, 0f)

            ColumnText.showTextAligned(cb,
                Element.ALIGN_CENTER, Phrase(globalTitle),
                (PAGE_SIZE.left + PAGE_SIZE.right) / 2, PAGE_SIZE.top - 60, 0f)

            ColumnText.showTextAligned(cb,
                Element.ALIGN_CENTER, Phrase(creationTitle),
                (PAGE_SIZE.left + PAGE_SIZE.right) / 2, PAGE_SIZE.top - 45, 0f)

            if (pr.numberOfPages > 1) {
                ColumnText.showTextAligned(cb,
                    Element.ALIGN_RIGHT, Phrase(pageN.toString() + "/" + pr.numberOfPages),
                    rect.right, rect.top, 0f)
            }

            // Footer
            val generatedBy = "Generated by $versionTag"

            ColumnText.showTextAligned(cb,
                Element.ALIGN_CENTER, Phrase(generatedBy),
                (PAGE_SIZE.left + PAGE_SIZE.right) / 2, PAGE_SIZE.bottom + 40, 0f)
        }
    }
}
