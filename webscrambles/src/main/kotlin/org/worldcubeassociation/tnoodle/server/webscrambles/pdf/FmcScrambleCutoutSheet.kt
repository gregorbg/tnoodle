package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.property.TextAlignment
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.drawDashedLine
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.populateRect
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.renderSvgToPDF
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.*
import java.util.*

class FmcScrambleCutoutSheet(scrambleSet: ScrambleSet, activityCode: ActivityCode, competitionTitle: String, locale: Locale, hasGroupID: Boolean) : FmcSheet(scrambleSet, activityCode, competitionTitle, locale, hasGroupID) {
    override fun PdfDocument.writeContents() {
        for (i in scrambleSet.scrambles.indices) {
            this.addNewPage()
                .addFmcScrambleCutoutSheet(i)
        }
    }

    private fun PdfPage.addFmcScrambleCutoutSheet(index: Int) {
        val scrambleModel = scrambleSet.scrambles[index]
        val scramble = scrambleModel.allScrambleStrings.single() // we assume FMC only has one scramble

        val right = (pageSize.width - LEFT).toInt()
        val top = (pageSize.height - BOTTOM).toInt()

        val height = top - BOTTOM
        val width = right - LEFT

        val availableScrambleHeight = height / SCRAMBLES_PER_SHEET

        val availableScrambleWidth = (width * .45).toInt()
        val availablePaddedScrambleHeight = availableScrambleHeight - 2 * SCRAMBLE_IMAGE_PADDING

        val dim = scramblingPuzzle.getPreferredSize(availableScrambleWidth, availablePaddedScrambleHeight)
        val svg = scramblingPuzzle.drawScramble(scramble, null)

        //val tp = directContent.renderSvgToPDF(svg, dim)

        val substitutions = mapOf(
            "scrambleIndex" to (index + 1).toString(),
            "scrambleCount" to expectedAttemptNum.toString()
        )

        val scrambleSuffix = Translate.translate("fmc.scrambleXofY", locale, substitutions)
            .takeIf { expectedAttemptNum > 1 } ?: ""

        val attemptTitle = activityCode.compileTitleString(locale, includeGroupID = hasGroupID)
        val title = "$competitionTitle - $attemptTitle$scrambleSuffix"

        // empty strings for space above and below
        val textList = listOf("", title, scramble, "")
        val alignList = List(textList.size) { TextAlignment.LEFT } // FIXME use TextAlignment or HorizontalAlignment here?!

        val paddedTitleItems = textList.zip(alignList)

        val foo = Document(document)
        val canvas = PdfCanvas(this)

        val pageNum = document.getPageNumber(this)

        for (i in 0 until SCRAMBLES_PER_SHEET) {
            val rect = Rectangle(LEFT.toFloat(), (top - i * availableScrambleHeight).toFloat(), (right - dim.width - SPACE_SCRAMBLE_IMAGE).toFloat(), (top - (i + 1) * availableScrambleHeight).toFloat())
            foo.populateRect(rect, pageNum, paddedTitleItems, BASE_FONT, FONT_SIZE.toInt())

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

        val FONT_SIZE = 20f

        val SCRAMBLES_PER_SHEET = 8
    }
}
