package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.canvas.draw.DashedLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ActivityCode
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ScrambleSet
import java.util.*

class FmcScrambleCutoutSheet(
    scrambleSet: ScrambleSet,
    activityCode: ActivityCode,
    competitionTitle: String,
    locale: Locale,
    hasGroupID: Boolean
) : FmcSheet(scrambleSet, activityCode, competitionTitle, locale, hasGroupID) {
    override fun Document.addFmcSheet(index: Int) {
        val scrambleModel = scrambleSet.scrambles[index]
        val scramble = scrambleModel.allScrambleStrings.single() // we assume FMC only has one scramble

        val svg = scramblingPuzzle.drawScramble(scramble, null)

        val substitutions = mapOf(
            "scrambleIndex" to (index + 1).toString(),
            "scrambleCount" to expectedAttemptNum.toString()
        )

        val scrambleSuffix = Translate.translate("fmc.scrambleXofY", locale, substitutions)
            .takeIf { expectedAttemptNum > 1 }.orEmpty()

        val attemptTitle = activityCode.compileTitleString(locale, includeGroupID = hasGroupID)
        val title = "$competitionTitle - $attemptTitle$scrambleSuffix"

        val table = Table(floatArrayOf(3.75f, 1f))
            .useAllAvailableWidth()
            .setFixedLayout()
            .setVerticalBorderSpacing(SPACE_SCRAMBLE_IMAGE)
            .setHorizontalBorderSpacing(SCRAMBLE_IMAGE_PADDING)

        val titleParagraph = Paragraph(title)
            .setFont(localFont)
            .setFontSize(FONT_SIZE)

        val titleCell = Cell().add(titleParagraph)
            .setBorder(Border.NO_BORDER)

        val scrambleImg = Image(SvgConverter.convertToXObject(svg.toString(), pdfDocument))
            .setAutoScale(true)

        val scrambleImgCell = Cell(2, 1).add(scrambleImg)
            .setBorder(Border.NO_BORDER)
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)

        val scrambleParagraph = Paragraph(scramble)
            .setFont(localFont)
            .setFontSize(FONT_SIZE)

        val scrambleStrCell = Cell().add(scrambleParagraph)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setBorder(Border.NO_BORDER)

        table.addCell(titleCell)
        table.addCell(scrambleImgCell)
        table.addCell(scrambleStrCell)

        val dashedLineSep = LineSeparator(DashedLine(1f)) // FIXME, posted on SO

        this.add(dashedLineSep)

        for (i in 0 until SCRAMBLES_PER_SHEET) {
            this.add(table)
            this.add(dashedLineSep)
        }
    }

    companion object {
        val SPACE_SCRAMBLE_IMAGE = 5f // scramble image won't touch the scramble
        val SCRAMBLE_IMAGE_PADDING = 8f // scramble image won't touch the dashed lines

        val FONT_SIZE = 12f

        val SCRAMBLES_PER_SHEET = 8
    }
}
