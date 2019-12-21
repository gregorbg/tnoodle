package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfOutline
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.action.PdfAction
import com.itextpdf.kernel.pdf.navigation.PdfExplicitRemoteGoToDestination
import com.itextpdf.kernel.utils.PdfMerger
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.OutlineConfiguration

class MergedPdfWithOutline(val toMerge: List<PdfContent>, val configuration: List<OutlineConfiguration>) : BasePdfSheet() {
    override fun PdfDocument.writeContents() {
        val merger = PdfMerger(this)

        val root = getOutlines(true)
        val outlineByPuzzle = mutableMapOf<String, PdfOutline>()

        for ((origPdf, config) in toMerge.zip(configuration)) {
            val puzzleLink: PdfOutline = outlineByPuzzle.getOrPut(config.group) {
                root.addOutline(config.group).addLinkEntry(numberOfPages).apply {
                    setOpen(false)
                }
            }

            puzzleLink.addOutline(config.title).addLinkEntry(numberOfPages)

            val contentReader = PdfReader(origPdf.render().inputStream())
            val contentDocument = PdfDocument(contentReader)

            for (j in 0 until config.copies) {
                merger.merge(contentDocument, 1, contentDocument.numberOfPages)
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
