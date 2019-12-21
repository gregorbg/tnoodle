package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfOutline
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.action.PdfAction
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination
import com.itextpdf.kernel.pdf.navigation.PdfExplicitRemoteGoToDestination
import com.itextpdf.kernel.utils.PdfMerger

class MergedPdfWithOutline(val toMerge: List<PdfContent>, val configuration: List<Triple<String, String, Int>>, globalTitle: String?) : BasePdfSheet(globalTitle) {
    override fun PdfDocument.writeContents() {
        val merger = PdfMerger(this)

        val root = getOutlines(true)
        val outlineByPuzzle = mutableMapOf<String, PdfOutline>()

        var pages = 1

        for ((origPdf, configData) in toMerge.zip(configuration)) {
            val (title, group, copies) = configData

            val puzzleLink: PdfOutline = outlineByPuzzle.getOrPut(group) {
                root.addOutline(group).addLinkEntry(pages).apply {
                    setOpen(false)
                }
            }

            puzzleLink.addOutline(title).addLinkEntry(pages)

            val contentReader = PdfReader(origPdf.render().inputStream())
            val contentDocument = PdfDocument(contentReader)

            for (j in 0 until copies) {
                merger.merge(contentDocument, 1, contentDocument.numberOfPages)

                pages += contentDocument.numberOfPages
            }

            contentDocument.close() // FIXME is this necessary?
        }
    }

    private fun PdfOutline.addLinkEntry(destPageNum: Int) = apply {
        val destAction = PdfExplicitRemoteGoToDestination.createFit(destPageNum)
        val gotoAction = PdfAction.createGoTo(destAction)

        addAction(gotoAction)
    }
}
