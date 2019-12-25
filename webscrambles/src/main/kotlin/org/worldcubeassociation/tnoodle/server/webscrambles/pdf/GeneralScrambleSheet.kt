package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ActivityCode
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.Scramble
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ScrambleSet

class GeneralScrambleSheet(scrambleSet: ScrambleSet, activityCode: ActivityCode) : BaseScrambleSheet(scrambleSet, activityCode) {
    override fun PdfDocument.writeContents() = addNewPage().writeScrambleContents()

    fun PdfPage.writeScrambleContents() {
        val foo = Document(document)

        foo.addScrambleTable(scrambleSet.scrambles)

        if (scrambleSet.extraScrambles.isNotEmpty()) {
            foo.addScrambleTable(scrambleSet.extraScrambles, EXTRA_SCRAMBLE_PREFIX, TABLE_HEADING_EXTRA_SCRAMBLES)
        }
    }

    fun Document.addScrambleTable(scrambles: List<Scramble>, scrambleNumberPrefix: String = STD_SCRAMBLE_PREFIX, specialHeading: String? = null) {
        val table = Table(3)
            .useAllAvailableWidth()
            .setAutoLayout()

        if (specialHeading != null) {
            val headingCell = Cell(1, 3)
                .add(Paragraph(specialHeading))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            table.addCell(headingCell)
        }

        val strScrambles = scrambles.toPDFStrings(scramblingPuzzle.shortName)

        val scrambleFont = FontUtil.MONO_FONT

        for ((i, scramble) in strScrambles.withIndex()) {
            val indexLabel = Text("$scrambleNumberPrefix${i + 1}")

            val nthScramble = Cell()
                .add(Paragraph(indexLabel))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)

            table.addCell(nthScramble)

            val scramblePhrase = Paragraph(scramble)
                .setMultipliedLeading(1.1f) // FIXME const
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

        const val MAX_SCRAMBLES_PER_PAGE = 7
        const val MAX_SCRAMBLE_IMAGE_RATIO = 3
        const val SCRAMBLE_IMAGE_PADDING = 2

        const val SCRAMBLE_MARGIN = 5
        const val STD_SCRAMBLE_PREFIX = ""
        const val EXTRA_SCRAMBLE_PREFIX = "E"

        const val TABLE_HEADING_EXTRA_SCRAMBLES = "Extra Scrambles"

        private val HIGHLIGHT_COLOR = DeviceRgb(230, 230, 230)

        const val INDEX_COLUMN_WIDTH_RATIO = 25
        const val EXTRA_SCRAMBLES_HEIGHT_RATIO = 30

        const val VERTICAL_MARGIN = 15f
        const val HORIZONTAL_MARGIN = 35f

        const val NEW_LINE = "\n"
    }
}
