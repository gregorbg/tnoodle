package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfUtil.splitToLineChunks
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ActivityCode
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.Scramble
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ScrambleSet
import org.worldcubeassociation.tnoodle.svglite.Dimension
import kotlin.math.min

class GeneralScrambleSheet(scrambleSet: ScrambleSet, activityCode: ActivityCode) : BaseScrambleSheet(scrambleSet, activityCode) {
    override fun PdfDocument.writeContents() = addNewPage().writeScrambleContents()

    fun PdfPage.writeScrambleContents() {
        val foo = Document(document)

        val availableWidth = pageSize.width - 2 * HORIZONTAL_MARGIN
        val availableHeight = pageSize.height - 2 * VERTICAL_MARGIN

        val headerAndFooterHeight = availableHeight / WatermarkPdfWrapper.HEADER_AND_FOOTER_HEIGHT_RATIO
        val extraScrambleLabelHeight = if (scrambleSet.extraScrambles.isNotEmpty()) availableHeight / EXTRA_SCRAMBLES_HEIGHT_RATIO else 0f

        val indexColumnWidth = availableWidth / INDEX_COLUMN_WIDTH_RATIO

        // Available height for all scrambles (including extras)
        val allScramblesHeight = availableHeight - 2 * headerAndFooterHeight - extraScrambleLabelHeight

        val scramblesPerPage = min(MAX_SCRAMBLES_PER_PAGE, scrambleSet.allScrambles.size)
        val maxScrambleImageHeight = (allScramblesHeight / scramblesPerPage - 2 * SCRAMBLE_IMAGE_PADDING).toInt()

        // We don't let scramble images take up too much of a the page
        val maxScrambleImageWidth = (availableWidth / MAX_SCRAMBLE_IMAGE_RATIO).toInt()

        val scrambleImageSize = scramblingPuzzle.getPreferredSize(maxScrambleImageWidth, maxScrambleImageHeight)

        val allScrambleStrings = scrambleSet.allScrambles.toPDFStrings(scramblingPuzzle.shortName)

        val scrambleImageHeight = scrambleImageSize.height.toFloat()
        val scrambleColumnWidth = availableWidth - indexColumnWidth - scrambleImageSize.width

        val availableScrambleArea = Rectangle(scrambleColumnWidth, scrambleImageHeight - 2 * SCRAMBLE_MARGIN)

        val (scrambleFont, fontSize) = getFontConfiguration(availableScrambleArea, allScrambleStrings)

        // First check if any scramble requires highlighting.
        val useHighlighting = requiresHighlighting(scrambleColumnWidth, scrambleFont, fontSize, allScrambleStrings)

        // Add main scrambles
        val table = document.createTable(scrambleColumnWidth, indexColumnWidth, scrambleFont, fontSize, scrambleImageSize, scrambleSet.scrambles, STD_SCRAMBLE_PREFIX, useHighlighting)
        foo.add(table)

        // Maybe add extra scrambles
        if (scrambleSet.extraScrambles.isNotEmpty()) {
            val headerTable = Table(1)
                .useAllAvailableWidth()

            val extraScramblesHeader = Cell()
                .add(Paragraph(TABLE_HEADING_EXTRA_SCRAMBLES))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingBottom(3f)

            headerTable.addCell(extraScramblesHeader)
            foo.add(headerTable)

            val extraTable = document.createTable(scrambleColumnWidth, indexColumnWidth, scrambleFont, fontSize, scrambleImageSize, scrambleSet.extraScrambles, EXTRA_SCRAMBLE_PREFIX, useHighlighting)
            foo.add(extraTable)
        }
    }

    private fun getFontConfiguration(availableArea: Rectangle, scrambles: List<String>): Pair<PdfFont, Float> {
        val longestScramble = scrambles.flatMap { it.split(NEW_LINE) }.maxByOrNull { it.length }.orEmpty()
        val maxLines = scrambles.map { it.split(NEW_LINE) }.map { it.count() }.maxOrNull() ?: 1

        val fontSize = PdfUtil.fitText(FontUtil.MONO_FONT, longestScramble, availableArea, FontUtil.MAX_SCRAMBLE_FONT_SIZE, true, 1f) // FIXME const
        val fontSizeIfIncludingNewlines = availableArea.height / maxLines

        // fontSize should fit horizontally. fontSizeIfIncludingNewlines should fit considering \n
        // In case maxLines = 1, fontSizeIfIncludingNewlines is just ignored (as 1 font size should fill the whole rectangle's height)
        // in case we have maxLines > 1, we fit width or height and take the min of it.
        val perfectFontSize = min(fontSize, fontSizeIfIncludingNewlines)

        return FontUtil.MONO_FONT to perfectFontSize
    }

    private fun requiresHighlighting(scrambleColumnWidth: Float, scrambleFont: PdfFont, fontSize: Float, scrambles: List<String>): Boolean {
        val lineChunks = scrambles.map { it.splitToLineChunks(scrambleFont, fontSize, scrambleColumnWidth) }

        return lineChunks.any { it.size >= MIN_LINES_TO_ALTERNATE_HIGHLIGHTING }
    }

    fun PdfDocument.createTable(scrambleColumnWidth: Float, indexColumnWidth: Float, scrambleFont: PdfFont, fontSize: Float, scrambleImageSize: Dimension, scrambles: List<Scramble>, scrambleNumberPrefix: String, useHighlighting: Boolean): Table {
        val table = Table(3)
            .useAllAvailableWidth()
            .setAutoLayout().apply {
            // FIXME setTotalWidth(floatArrayOf(indexColumnWidth, scrambleColumnWidth, (scrambleImageSize.width + 2 * SCRAMBLE_IMAGE_PADDING).toFloat()))
        }

        val strScrambles = scrambles.toPDFStrings(scramblingPuzzle.shortName)

        for ((i, scramble) in strScrambles.withIndex()) {
            val ch = Text("$scrambleNumberPrefix${i + 1}")
            val indexCell = Cell()
                .add(Paragraph(ch))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)

            table.addCell(indexCell)

            val lineChunks = scramble.splitToLineChunks(scrambleFont, fontSize, scrambleColumnWidth)
            val scramblePhrase = Paragraph().setMultipliedLeading(1.1f) // FIXME const

            for ((nthLine, lineChunk) in lineChunks.withIndex()) {
                if (useHighlighting && nthLine % 2 == 1) {
                    lineChunk.setBackgroundColor(HIGHLIGHT_COLOR)
                }
                scramblePhrase.add(lineChunk)
            }

            val scrambleCell = Cell().add(scramblePhrase)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                // This shifts everything up a little bit, because I don't like how
                // ALIGN_MIDDLE works.
                //.setPaddingTop(-SCRAMBLE_PADDING_VERTICAL_TOP.toFloat())
                //.setPaddingBottom(SCRAMBLE_PADDING_VERTICAL_BOTTOM.toFloat())
                //.setPaddingLeft(SCRAMBLE_PADDING_HORIZONTAL.toFloat())
                //.setPaddingRight(SCRAMBLE_PADDING_HORIZONTAL.toFloat())
                // FIXME control line wrapping better
                /*.apply {
                    // We carefully inserted newlines ourselves to make stuff fit, don't
                    // let itextpdf wrap lines for us.
                    isNoWrap = true
                }*/

            table.addCell(scrambleCell)

            if (scrambleImageSize.width > 0 && scrambleImageSize.height > 0) {
                val svg = scramblingPuzzle.drawScramble(scramble, null)
                val img = Image(SvgConverter.convertToXObject(svg.toString(), this))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setAutoScale(true)

                table.addCell(img)
            } else {
                table.addCell(EMPTY_CELL_CONTENT)
            }
        }

        return table
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

        const val EMPTY_CELL_CONTENT = ""

        const val STD_SCRAMBLE_PREFIX = EMPTY_CELL_CONTENT
        const val EXTRA_SCRAMBLE_PREFIX = "E"

        const val TABLE_HEADING_EXTRA_SCRAMBLES = "Extra Scrambles"

        private const val MIN_LINES_TO_ALTERNATE_HIGHLIGHTING = 4

        private val HIGHLIGHT_COLOR = DeviceRgb(230, 230, 230)

        const val INDEX_COLUMN_WIDTH_RATIO = 25
        const val EXTRA_SCRAMBLES_HEIGHT_RATIO = 30

        const val VERTICAL_MARGIN = 15f
        const val HORIZONTAL_MARGIN = 35f

        const val NEW_LINE = "\n"
    }
}
