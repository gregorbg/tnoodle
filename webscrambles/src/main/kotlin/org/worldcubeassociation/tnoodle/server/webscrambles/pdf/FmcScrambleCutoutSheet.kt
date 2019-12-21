package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.property.TextAlignment
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.drawDashedLine
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.populateRect
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.renderSvgToPDF
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil

class FmcScrambleCutoutSheet(request: ScrambleRequest, globalTitle: String?): FmcSheet(request, globalTitle) {
    override fun PdfDocument.writeContents() {
        for (i in scrambleRequest.scrambles.indices) {
            this.addNewPage()
                .addFmcScrambleCutoutSheet(scrambleRequest, title, i)
        }
    }

    private fun PdfPage.addFmcScrambleCutoutSheet(scrambleRequest: ScrambleRequest, globalTitle: String?, index: Int) {
        val pageSize = this.pageSize
        val scramble = scrambleRequest.scrambles[index]

        val right = (pageSize.width - LEFT).toInt()
        val top = (pageSize.height - BOTTOM).toInt()

        val height = top - BOTTOM
        val width = right - LEFT

        val availableScrambleHeight = height / SCRAMBLES_PER_SHEET

        val availableScrambleWidth = (width * .45).toInt()
        val availablePaddedScrambleHeight = availableScrambleHeight - 2 * SCRAMBLE_IMAGE_PADDING

        val dim = scrambleRequest.scrambler.getPreferredSize(availableScrambleWidth, availablePaddedScrambleHeight)
        val svg = scrambleRequest.scrambler.drawScramble(scramble, scrambleRequest.colorScheme)

        //val tp = directContent.renderSvgToPDF(svg, dim)

        val scrambleSuffix = " - Scramble ${index + 1} of ${scrambleRequest.scrambles.size}"
            .takeIf { scrambleRequest.scrambles.size > 1 } ?: ""

        val title = "$globalTitle - ${scrambleRequest.title}$scrambleSuffix"

        // empty strings for space above and below
        val textList = listOf("", title, scramble, "")
        val alignList = List(textList.size) { TextAlignment.LEFT } // FIXME use TextAlignment or HorizontalAlignment here?!

        val paddedTitleItems = textList.zip(alignList)

        val foo = Document(document)
        val canvas = PdfCanvas(this)

        val pageNum = document.getPageNumber(this)

        for (i in 0 until SCRAMBLES_PER_SHEET) {
            val rect = Rectangle(LEFT.toFloat(), (top - i * availableScrambleHeight).toFloat(), (right - dim.width - SPACE_SCRAMBLE_IMAGE).toFloat(), (top - (i + 1) * availableScrambleHeight).toFloat())
            foo.populateRect(rect, pageNum, paddedTitleItems, BASE_FONT, FONT_SIZE)

            val imgX = (right - dim.width).toFloat()
            val imgY = top.toFloat() - (i + 1) * availableScrambleHeight + (availableScrambleHeight - dim.getHeight()) / 2

            canvas.renderSvgToPDF(svg, imgX, imgY)

            canvas.drawDashedLine(LEFT, right, top - i * availableScrambleHeight)
        }

        canvas.drawDashedLine(LEFT, right, top - SCRAMBLES_PER_SHEET * availableScrambleHeight)
    }

    companion object {
        val BASE_FONT = FontUtil.getFontForLocale(Translate.DEFAULT_LOCALE)

        val BOTTOM = 10
        val LEFT = 20

        val SPACE_SCRAMBLE_IMAGE = 5 // scramble image won't touch the scramble
        val SCRAMBLE_IMAGE_PADDING = 8 // scramble image won't touch the dashed lines

        val FONT_SIZE = 20

        val SCRAMBLES_PER_SHEET = 8
    }
}
