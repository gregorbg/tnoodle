package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.layout.renderer.CellRenderer
import com.itextpdf.svg.converter.SvgConverter
import net.gnehzr.tnoodle.scrambles.Puzzle
import net.gnehzr.tnoodle.svglite.Color
import net.gnehzr.tnoodle.svglite.Dimension
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfUtil.splitToLineChunks
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.StringUtil
import kotlin.math.log10
import kotlin.math.min

class GeneralScrambleSheet(scrambleRequest: ScrambleRequest, globalTitle: String?) : BaseScrambleSheet(scrambleRequest, globalTitle) {
    override fun PdfDocument.writeContents() = addNewPage().writeScrambleContents()

    fun PdfPage.writeScrambleContents() {
        val foo = Document(document)

        val sideMargins = 100f + foo.leftMargin + foo.rightMargin
        val availableWidth = pageSize.width - sideMargins

        // Yeee magic numbers. This should make space for the headerTable.
        val extraScrambleSpacing = 20.takeIf { scrambleRequest.extraScrambles.isNotEmpty() } ?: 0

        val vertMargins = foo.topMargin + foo.bottomMargin
        val availableHeight = pageSize.height - vertMargins - extraScrambleSpacing

        val scramblesPerPage = min(MAX_SCRAMBLES_PER_PAGE, scrambleRequest.allScrambles.size)
        val maxScrambleImageHeight = (availableHeight / scramblesPerPage - 2 * SCRAMBLE_IMAGE_PADDING).toInt()

        // We don't let scramble images take up more than half the page
        val maxScrambleImageWidth = (availableWidth / 2).toInt().takeUnless {
            // TODO - If we allow the megaminx image to be too wide, the
            //   Megaminx scrambles get really tiny. This tweak allocates
            //   a more optimal amount of space to the scrambles. This is possible
            //   because the scrambles are so uniformly sized.
            scrambleRequest.scrambler.shortName == "minx"
        } ?: 190

        val scrambleImageSize = scrambleRequest.scrambler.getPreferredSize(maxScrambleImageWidth, maxScrambleImageHeight)

        // First check if any scramble requires highlighting.
        val forceStdHighlighting = requiresHighlighting(availableWidth, scrambleImageSize, scrambleRequest.scrambles, STD_SCRAMBLE_PREFIX)

        // Also check extra scrambles for visual consistency
        val forceExtraHighlighting = requiresHighlighting(availableWidth, scrambleImageSize, scrambleRequest.extraScrambles, EXTRA_SCRAMBLE_PREFIX)

        // Compute line lengths just to see if any scrambles require highlighting.
        val useHighlighting = forceStdHighlighting || forceExtraHighlighting

        val table = document.createTable(availableWidth, scrambleImageSize, scrambleRequest.scrambles, scrambleRequest.scrambler, scrambleRequest.colorScheme, STD_SCRAMBLE_PREFIX, useHighlighting)
        foo.add(table)

        if (scrambleRequest.extraScrambles.isNotEmpty()) {
            val headerTable = Table(1)
                .setWidth(availableWidth)

            val extraScramblesHeader = Cell()
                .add(Paragraph(TABLE_HEADING_EXTRA_SCRAMBLES))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingBottom(3f)

            headerTable.addCell(extraScramblesHeader)
            foo.add(headerTable)

            val extraTable = document.createTable(availableWidth, scrambleImageSize, scrambleRequest.extraScrambles, scrambleRequest.scrambler, scrambleRequest.colorScheme, EXTRA_SCRAMBLE_PREFIX, useHighlighting)
            foo.add(extraTable)
        }
    }

    fun getIndexColumnWidth(scrambles: List<String>, scrambleNumberPrefix: String): Float {
        val fooFont = FontUtil.MONO_FONT

        val scramblesOrdinalLength = log10(scrambles.size.toDouble()).toInt()
        // + 1 at the end represents the dot.
        val charsWide = scrambleNumberPrefix.length + scramblesOrdinalLength + 1

        // M has got to be as wide or wider than the widest digit in our font
        val wideString = WIDEST_CHAR_STRING.repeat(charsWide) + "."

        // I don't know why we need the +5, perhaps there's some padding?
        return fooFont.getWidth(wideString) + 5f // FIXME measurement unit?!
    }

    fun getScrambleColumnWidth(availableWidth: Float, scrambleImageSize: Dimension, scrambles: List<String>, scrambleNumberPrefix: String): Float {
        val col1Width = getIndexColumnWidth(scrambles, scrambleNumberPrefix)

        return availableWidth - col1Width - scrambleImageSize.width.toFloat() - (2 * SCRAMBLE_IMAGE_PADDING).toFloat()
    }

    fun getFontConfiguration(availableWidth: Float, scrambleImageSize: Dimension, scrambles: List<String>, scrambleNumberPrefix: String): Pair<Float, Boolean> {
        val scrambleColumnWidth = getScrambleColumnWidth(availableWidth, scrambleImageSize, scrambles, scrambleNumberPrefix)
        val availableScrambleHeight = scrambleImageSize.height - 2 * SCRAMBLE_IMAGE_PADDING

        val availableArea = Rectangle(scrambleColumnWidth - 2 * SCRAMBLE_PADDING_HORIZONTAL,
            (availableScrambleHeight - SCRAMBLE_PADDING_VERTICAL_TOP - SCRAMBLE_PADDING_VERTICAL_BOTTOM).toFloat())

        val longestScramble = scrambles.maxBy { it.length } ?: ""

        val longestAlignedScramble = scrambles.map { StringUtil.padTurnsUniformly(it, WIDEST_CHAR_STRING) }
            .maxBy { it.length } ?: longestScramble

        val longestScrambleOneLine = "\n" !in longestAlignedScramble//Masked

        // I don't know how to configure ColumnText.fitText's word wrapping characters,
        // so instead, I just replace each character I don't want to wrap with M, which
        // should be the widest character (we're using a monospaced font,
        // so that doesn't really matter), and won't get wrapped.
        val longestScrambleMasked = longestScramble.replace(".".toRegex(), WIDEST_CHAR_STRING)
        val longestAlignedScrambleMasked = longestAlignedScramble.replace("\\S".toRegex(), WIDEST_CHAR_STRING)
        val longestAlignedScrambleMaskedAndStuffed = longestAlignedScrambleMasked.replace(" ".toRegex(), WIDEST_CHAR_STRING)

        val fontSizeForMaskedUnaligned = PdfUtil.fitText(FontUtil.MONO_FONT, longestScrambleMasked, availableArea, FontUtil.MAX_SCRAMBLE_FONT_SIZE, false, 1f) // FIXME const

        // If the scramble contains newlines, then we *only* allow wrapping at the newlines.
        val longestRespectingNewlines = longestAlignedScrambleMasked.takeIf { longestScrambleOneLine }
            ?: longestAlignedScrambleMaskedAndStuffed
        val fontSizeIfIncludingNewlines = PdfUtil.fitText(FontUtil.MONO_FONT, longestRespectingNewlines, availableArea, FontUtil.MAX_SCRAMBLE_FONT_SIZE, true, 1f) // FIXME const

        val oneLine = longestScrambleOneLine && fontSizeForMaskedUnaligned >= FontUtil.MINIMUM_ONE_LINE_FONT_SIZE
        val perfectFontSize = fontSizeForMaskedUnaligned.takeIf { oneLine } ?: fontSizeIfIncludingNewlines

        return perfectFontSize to oneLine
    }

    fun requiresHighlighting(availableWidth: Float, scrambleImageSize: Dimension, scrambles: List<String>, scrambleNumberPrefix: String): Boolean {
        val scrambleColumnWidth = getScrambleColumnWidth(availableWidth, scrambleImageSize, scrambles, scrambleNumberPrefix)
        val (scrambleFontSize, oneLine) = getFontConfiguration(availableWidth, scrambleImageSize, scrambles, scrambleNumberPrefix)

        val paddedScrambles = scrambles.map { if (oneLine) it else StringUtil.padTurnsUniformly(it, PdfUtil.NON_BREAKING_SPACE.toString()) }
        val lineChunks = paddedScrambles.map { it.splitToLineChunks(FontUtil.MONO_FONT, scrambleFontSize, scrambleColumnWidth) }

        return lineChunks.any { it.size >= MIN_LINES_TO_ALTERNATE_HIGHLIGHTING }
    }

    fun PdfDocument.createTable(availableWidth: Float, scrambleImageSize: Dimension, scrambles: List<String>, scrambler: Puzzle, colorScheme: HashMap<String, Color>?, scrambleNumberPrefix: String, useHighlighting: Boolean): Table {
        // FIXME val indexColumnWidth = getIndexColumnWidth(scrambles, scrambleNumberPrefix)
        val scrambleColumnWidth = getScrambleColumnWidth(availableWidth, scrambleImageSize, scrambles, scrambleNumberPrefix)

        val (scrambleFontSize, oneLine) = getFontConfiguration(availableWidth, scrambleImageSize, scrambles, scrambleNumberPrefix)

        val table = Table(3).apply {
            // FIXME setTotalWidth(floatArrayOf(indexColumnWidth, scrambleColumnWidth, (scrambleImageSize.width + 2 * SCRAMBLE_IMAGE_PADDING).toFloat()))
        }

        for ((i, scramble) in scrambles.withIndex()) {
            val ch = Text("$scrambleNumberPrefix${i + 1}.")
            val nthscramble = Cell()
                .add(Paragraph(ch))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            table.addCell(nthscramble)

            val paddedScramble = if (oneLine) scramble else StringUtil.padTurnsUniformly(scramble, PdfUtil.NON_BREAKING_SPACE.toString())
            val lineChunks = paddedScramble.splitToLineChunks(FontUtil.MONO_FONT, scrambleFontSize, scrambleColumnWidth)
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
                .setPaddingTop(-SCRAMBLE_PADDING_VERTICAL_TOP.toFloat())
                .setPaddingBottom(SCRAMBLE_PADDING_VERTICAL_BOTTOM.toFloat())
                .setPaddingLeft(SCRAMBLE_PADDING_HORIZONTAL.toFloat())
                .setPaddingRight(SCRAMBLE_PADDING_HORIZONTAL.toFloat())
                // FIXME control line wrapping better
                /*.apply {
                    // We carefully inserted newlines ourselves to make stuff fit, don't
                    // let itextpdf wrap lines for us.
                    isNoWrap = true
                }*/

            table.addCell(scrambleCell)

            if (scrambleImageSize.width > 0 && scrambleImageSize.height > 0) {
                val svg = scrambler.drawScramble(scramble, colorScheme)
                val img = SvgConverter.convertToImage(svg.toString().byteInputStream(), this)

                val imgCell = Cell()
                    .add(img)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)

                table.addCell(imgCell)
            } else {
                table.addCell(EMPTY_CELL_CONTENT)
            }
        }

        return table
    }

    companion object {
        const val MAX_SCRAMBLES_PER_PAGE = 7
        const val SCRAMBLE_IMAGE_PADDING = 2

        const val SCRAMBLE_PADDING_VERTICAL_TOP = 3
        const val SCRAMBLE_PADDING_VERTICAL_BOTTOM = 6
        const val SCRAMBLE_PADDING_HORIZONTAL = 1

        const val WIDEST_CHAR_STRING = "M"

        const val EMPTY_CELL_CONTENT = ""

        const val STD_SCRAMBLE_PREFIX = EMPTY_CELL_CONTENT
        const val EXTRA_SCRAMBLE_PREFIX = "E"

        const val TABLE_HEADING_EXTRA_SCRAMBLES = "Extra Scrambles"

        private const val MIN_LINES_TO_ALTERNATE_HIGHLIGHTING = 4

        private val HIGHLIGHT_COLOR = DeviceRgb(230, 230, 230)
    }
}
