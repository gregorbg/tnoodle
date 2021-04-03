package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.model.EventData
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ActivityCode
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.Scramble
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ScrambleSet
import java.io.File

class GeneralScrambleSheet(scrambleSet: ScrambleSet, activityCode: ActivityCode) : BaseScrambleSheet(scrambleSet, activityCode) {
    private val scrambleFont = FontUtil.MONO_FONT

    override fun PdfDocument.writeContents() {
        val pdf = Document(this)

        pdf.addScrambleTable(scrambleSet.scrambles)

        if (scrambleSet.extraScrambles.isNotEmpty()) {
            pdf.addScrambleTable(scrambleSet.extraScrambles, EXTRA_SCRAMBLE_PREFIX, TABLE_HEADING_EXTRA_SCRAMBLES)
        }
    }

    fun Document.addScrambleTable(scrambles: List<Scramble>, scrambleNumberPrefix: String = STD_SCRAMBLE_PREFIX, specialHeading: String? = null) {
        val tableSizes = UnitValue.createPercentArray(floatArrayOf(1/25f, 2/3f, 1/3f))

        val table = Table(tableSizes)
            .useAllAvailableWidth()
            .setFixedLayout()

        if (specialHeading != null) {
            val headingPar = Paragraph(specialHeading)
                .setTextAlignment(TextAlignment.CENTER)

            val headingCell = Cell(1, 3)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
                .add(headingPar)

            table.addCell(headingCell)
        }

        val strScrambles = scrambles.toPDFStrings(scramblingPuzzle.shortName)

        for ((i, scramble) in strScrambles.withIndex()) {
            val indexLabel = Text("$scrambleNumberPrefix${i + 1}")

            val nthScramble = Cell()
                .add(Paragraph(indexLabel))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)

            table.addCell(nthScramble)

            val scramblePhrase = Paragraph(scramble)
                .setMultipliedLeading(SCRAMBLE_LINE_LEADING)
            // TODO highlighting, cell renderer

            val scrambleCell = Cell().add(scramblePhrase)
                .setKeepTogether(true)
                .setFont(scrambleFont)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            table.addCell(scrambleCell)

            val svg = scramblingPuzzle.drawScramble(scramble, null)
            val img = Image(SvgConverter.convertToXObject(svg.toString(), pdfDocument))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setAutoScale(true)

            table.addCell(img)
        }

        this.add(table)
    }

    companion object {
        private fun List<Scramble>.toPDFStrings(puzzleName: String) =
            flatMap { it.allScrambleStrings }
                .takeUnless { puzzleName == "minx" } // minx scrambles intentionally include "\n" chars for alignment
                ?: map { it.scrambleString }

        const val STD_SCRAMBLE_PREFIX = ""
        const val EXTRA_SCRAMBLE_PREFIX = "E"

        const val TABLE_HEADING_EXTRA_SCRAMBLES = "Extra Scrambles" // TODO Translate

        const val SCRAMBLE_LINE_LEADING = 1.1f

        const val MAX_SCRAMBLE_FONT_SIZE = 20f
        const val MINIMUM_ONE_LINE_FONT_SIZE = 15f

        private val HIGHLIGHT_COLOR = DeviceRgb(230, 230, 230)
    }
}
