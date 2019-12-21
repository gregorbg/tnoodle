package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.VerticalAlignment
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.renderSvgToPDF
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.fitAndShowText
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfDrawUtil.populateRect
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfUtil
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

open class FmcSolutionSheet(request: ScrambleRequest, globalTitle: String?, locale: Locale) : FmcSheet(request, globalTitle, locale) {
    override fun PdfDocument.writeContents() {
        val bf = FontUtil.getFontForLocale(locale)

        for (i in scrambleRequest.scrambles.indices) {
            this.addNewPage()
                .addFmcSolutionSheet(scrambleRequest, title, i, locale, bf)
        }
    }

    protected fun PdfPage.addFmcSolutionSheet(scrambleRequest: ScrambleRequest, globalTitle: String?, index: Int, locale: Locale, bf: PdfFont) {
        val withScramble = index != -1

        val highLevelDocument = Document(document)
        val pageNum = document.getPageNumber(this)

        val canvas = PdfCanvas(this)

        val right = (pageSize.width - LEFT).toInt()
        val top = (pageSize.height - BOTTOM).toInt()

        val height = top - BOTTOM
        val width = right - LEFT

        val solutionBorderTop = BOTTOM + (height * .5).toInt()
        val scrambleBorderTop = solutionBorderTop + 40

        val competitorInfoBottom = top - (height * if (withScramble) .15 else .27).toInt()
        val gradeBottom = competitorInfoBottom - 50
        val competitorInfoLeft = right - (width * .45).toInt()

        canvas.drawFrameBorders(pageSize.width, pageSize.height, withScramble)
        canvas.drawSolutionMoveDashes(pageSize.width, pageSize.height, withScramble)

        if (withScramble) {
            val scramble = scrambleRequest.scrambles[index]

            canvas.drawScrambleAndImage(pageSize.width, pageSize.height, bf, scramble)
        }

        val showScrambleCount = withScramble && (scrambleRequest.scrambles.size > 1 || scrambleRequest.totalAttempt > 1)

        val competitorInfoRect = Rectangle((competitorInfoLeft + MARGIN).toFloat(), top.toFloat(), (right - MARGIN).toFloat(), competitorInfoBottom.toFloat())
        val gradeRect = Rectangle((competitorInfoLeft + MARGIN).toFloat(), competitorInfoBottom.toFloat(), (right - MARGIN).toFloat(), gradeBottom.toFloat())
        val scrambleImageRect = Rectangle((competitorInfoLeft + MARGIN).toFloat(), gradeBottom.toFloat(), (right - MARGIN).toFloat(), scrambleBorderTop.toFloat())

        // TODO consider sub-method along the lines of "writePersonalCompetitorInformation"?
        val personalDetailsItems = collectPersonalDetailsItems(withScramble, showScrambleCount, index, globalTitle!!)
        highLevelDocument.populateRect(competitorInfoRect, pageNum, personalDetailsItems, bf, FONT_SIZE)

        highLevelDocument.writeGradingInformation(gradeRect, bf, pageNum)

        // FIXME sub-method here
        if (!withScramble) {
            val separateSheetAdvice = Translate.translate("fmc.scrambleOnSeparateSheet", locale)

            val separateSheetAdviceItems = listOf(
                "" to TextAlignment.CENTER,
                separateSheetAdvice to TextAlignment.CENTER
            )

            highLevelDocument.populateRect(scrambleImageRect, pageNum, separateSheetAdviceItems, bf, 11) // FIXME const
        }

        // Table
        highLevelDocument.writeMovesTable(bf, pageSize.width, pageSize.height)

        // Rules
        val rect = Rectangle(LEFT.toFloat(), (top - MAGIC_NUMBER + RULE_FONT_SIZE).toFloat(), competitorInfoLeft.toFloat(), (top - MAGIC_NUMBER).toFloat())
        highLevelDocument.fitAndShowText(Translate.translate("fmc.event", locale), rect, pageNum, bf, RULE_FONT_SIZE.toFloat(), TextAlignment.CENTER, LEADING_MULTIPLIER)

        val substitutions = mapOf("maxMoves" to WCA_MAX_MOVES_FMC.toString())

        val rulesList = listOf(
            Translate.translate("fmc.rule1", locale),
            Translate.translate("fmc.rule2", locale),
            Translate.translate("fmc.rule3", locale),
            Translate.translate("fmc.rule4", locale, substitutions),
            Translate.translate("fmc.rule5", locale),
            Translate.translate("fmc.rule6", locale)
        )

        val rulesTop = competitorInfoBottom + if (withScramble) 65 else 153

        val rulesRectangle = Rectangle((LEFT + FMC_MARGIN).toFloat(), (scrambleBorderTop + TABLE_HEIGHT + FMC_MARGIN).toFloat(), (competitorInfoLeft - FMC_MARGIN).toFloat(), (rulesTop + FMC_MARGIN).toFloat())
        val rules = rulesList.joinToString("\n") { "• $it" }

        highLevelDocument.fitAndShowText(rules, rulesRectangle, pageNum, bf, 15f, TextAlignment.JUSTIFIED, 1.5f) // TODO const
        highLevelDocument.close()
    }

    private fun PdfCanvas.drawFrameBorders(pageWidth: Float, pageHeight: Float, withScramble: Boolean) {
        val right = (pageWidth - LEFT).toInt()
        val top = (pageHeight - BOTTOM).toInt()

        val height = top - BOTTOM
        val width = right - LEFT

        val solutionBorderTop = BOTTOM + (height * .5).toInt()
        val scrambleBorderTop = solutionBorderTop + 40

        val competitorInfoBottom = top - (height * if (withScramble) .15 else .27).toInt()
        val gradeBottom = competitorInfoBottom - 50
        val competitorInfoLeft = right - (width * .45).toInt()

        // Outer border
        setLineWidth(2f)
        moveTo(LEFT.toDouble(), top.toDouble())
        lineTo(LEFT.toDouble(), BOTTOM.toDouble())
        lineTo(right.toDouble(), BOTTOM.toDouble())
        lineTo(right.toDouble(), top.toDouble())

        // Solution border
        if (withScramble) {
            moveTo(LEFT.toDouble(), solutionBorderTop.toDouble())
            lineTo(right.toDouble(), solutionBorderTop.toDouble())
        }

        // Rules bottom border
        moveTo(LEFT.toDouble(), scrambleBorderTop.toDouble())
        lineTo((if (withScramble) competitorInfoLeft else right).toDouble(), scrambleBorderTop.toDouble())

        // Rules right border
        if (!withScramble) {
            moveTo(competitorInfoLeft.toDouble(), scrambleBorderTop.toDouble())
        }

        lineTo(competitorInfoLeft.toDouble(), gradeBottom.toDouble())

        // Grade bottom border
        moveTo(competitorInfoLeft.toDouble(), gradeBottom.toDouble())
        lineTo(right.toDouble(), gradeBottom.toDouble())

        // Competitor info bottom border
        moveTo(competitorInfoLeft.toDouble(), competitorInfoBottom.toDouble())
        lineTo(right.toDouble(), competitorInfoBottom.toDouble())

        // Competitor info left border
        moveTo(competitorInfoLeft.toDouble(), gradeBottom.toDouble())
        lineTo(competitorInfoLeft.toDouble(), top.toDouble())

        setLineWidth(FMC_LINE_THICKNESS)
        stroke()
    }

    private fun PdfCanvas.drawSolutionMoveDashes(pageWidth: Float, pageHeight: Float, withScramble: Boolean) {
        val right = (pageWidth - LEFT).toInt()
        val top = (pageHeight - BOTTOM).toInt()

        val height = top - BOTTOM

        val solutionBorderTop = BOTTOM + (height * .5).toInt()
        val scrambleBorderTop = solutionBorderTop + 40

        // Solution lines
        val availableSolutionWidth = right - LEFT
        val availableSolutionHeight = scrambleBorderTop - BOTTOM
        val linesY = ceil(1.0 * WCA_MAX_MOVES_FMC / LINES_X).toInt()

        val excessX = availableSolutionWidth - LINES_X * LINE_WIDTH

        for (y in 0 until linesY) {
            for (x in 0 until LINES_X) {
                val moveCount = y * linesY + x

                if (moveCount >= WCA_MAX_MOVES_FMC) {
                    break
                }

                val xPos = LEFT + x * LINE_WIDTH + (x + 1) * excessX / (LINES_X + 1)
                val yPos = (if (withScramble) solutionBorderTop else scrambleBorderTop) - (y + 1) * availableSolutionHeight / (linesY + 1)

                moveTo(xPos.toDouble(), yPos.toDouble())
                lineTo((xPos + LINE_WIDTH).toDouble(), yPos.toDouble())
            }
        }

        setLineWidth(UNDERLINE_THICKNESS)
        stroke()
    }

    private fun PdfCanvas.drawScrambleAndImage(pageWidth: Float, pageHeight: Float, font: PdfFont, scramble: String) {
        val right = (pageWidth - LEFT).toInt()
        val top = (pageHeight - BOTTOM).toInt()

        val height = top - BOTTOM
        val width = right - LEFT

        val solutionBorderTop = BOTTOM + (height * .5).toInt()
        val scrambleBorderTop = solutionBorderTop + 40

        val competitorInfoBottom = top - (height * .15).toInt()
        val gradeBottom = competitorInfoBottom - 50
        val competitorInfoLeft = right - (width * .45).toInt()

        beginText()
        val scrambleStr = Translate.translate("fmc.scramble", locale) + ": " + scramble

        val availableScrambleSpace = right - LEFT - 2 * PADDING

        val scrambleFontSizes = 0 until 20
        val scrambleFontSize = scrambleFontSizes.reversed().find {
            font.getWidth(scrambleStr, it.toFloat()) <= availableScrambleSpace
        } ?: 20

        setFontAndSize(font, scrambleFontSize.toFloat())
        val scrambleY = 3 + solutionBorderTop + (scrambleBorderTop - solutionBorderTop - scrambleFontSize) / 2
        showText(scrambleStr)
        moveText((LEFT + PADDING).toDouble(), scrambleY.toDouble())
        endText()

        val availableScrambleWidth = right - competitorInfoLeft
        val availableScrambleHeight = gradeBottom - scrambleBorderTop

        val dim = scrambleRequest.scrambler.getPreferredSize(availableScrambleWidth - 2, availableScrambleHeight - 2)
        val svg = scrambleRequest.scrambler.drawScramble(scramble, scrambleRequest.colorScheme)

        renderSvgToPDF(svg, (competitorInfoLeft + (availableScrambleWidth - dim.width) / 2).toFloat(), (scrambleBorderTop + (availableScrambleHeight - dim.height) / 2).toFloat())
    }

    private fun collectPersonalDetailsItems(withScramble: Boolean, showScrambleCount: Boolean, index: Int, globalTitle: String): List<Pair<String, TextAlignment>> {
        val personalDetailsItems = mutableListOf<Pair<String, TextAlignment>>()

        if (withScramble) {
            personalDetailsItems.add(globalTitle to TextAlignment.CENTER)
            personalDetailsItems.add(scrambleRequest.title to TextAlignment.CENTER)

            if (showScrambleCount) {
                // this is for ordered scrambles
                val attemptIndex = scrambleRequest.takeIf { it.totalAttempt > 1 }?.attempt ?: index
                val orderedIndex = max(attemptIndex, index)

                val absoluteTotal = scrambleRequest.totalAttempt.takeIf { it > 1 } ?: scrambleRequest.scrambles.size

                val substitutions = mapOf(
                    "scrambleIndex" to (orderedIndex + 1).toString(),
                    "scrambleCount" to absoluteTotal.toString()
                )

                val translatedInfo = Translate.translate("fmc.scrambleXofY", locale, substitutions)
                personalDetailsItems.add(translatedInfo to TextAlignment.CENTER)
            }
        } else {
            val competitionDesc = Translate.translate("fmc.competition", locale) + LONG_FILL
            val roundDesc = Translate.translate("fmc.round", locale) + SHORT_FILL
            val attemptDesc = Translate.translate("fmc.attempt", locale) + SHORT_FILL

            personalDetailsItems.add(competitionDesc to TextAlignment.LEFT)
            personalDetailsItems.add(roundDesc to TextAlignment.LEFT)
            personalDetailsItems.add(attemptDesc to TextAlignment.LEFT)
        }

        if (withScramble) { // more space for filling name
            personalDetailsItems.add("" to TextAlignment.LEFT)
        }

        val competitorDesc = Translate.translate("fmc.competitor", locale) + LONG_FILL
        personalDetailsItems.add(competitorDesc to TextAlignment.LEFT)

        if (withScramble) {
            personalDetailsItems.add("" to TextAlignment.LEFT)
        }

        personalDetailsItems.add(FORM_TEMPLATE_WCA_ID to TextAlignment.LEFT)

        if (withScramble) { // add space below
            personalDetailsItems.add("" to TextAlignment.LEFT)
        }

        val registrantIdDesc = Translate.translate("fmc.registrantId", locale) + SHORT_FILL
        personalDetailsItems.add(registrantIdDesc to TextAlignment.LEFT)

        if (withScramble) {
            personalDetailsItems.add("" to TextAlignment.LEFT)
        }

        return personalDetailsItems
    }

    private fun Document.writeGradingInformation(gradeRect: Rectangle, font: PdfFont, pageNum: Int) {
        // graded
        val gradingTextGradedBy = Translate.translate("fmc.graded", locale) + LONG_FILL
        val gradingTextResult = Translate.translate("fmc.result", locale) + SHORT_FILL

        val gradingText = "$gradingTextGradedBy $gradingTextResult"
        val warningText = Translate.translate("fmc.warning", locale)

        val gradingItemsWithAlignment = listOf(
            warningText to TextAlignment.CENTER,
            gradingText to TextAlignment.CENTER
        )

        // FIXME split here
        populateRect(gradeRect, pageNum, gradingItemsWithAlignment, font, 11) // FIXME const
    }

    private fun Document.writeMovesTable(font: PdfFont, pageWidth: Float, pageHeight: Float) {
        val right = (pageWidth - LEFT).toInt()
        val top = (pageHeight - BOTTOM).toInt()

        val height = top - BOTTOM
        val width = right - LEFT

        val solutionBorderTop = BOTTOM + (height * .5).toInt()
        // FIXME val scrambleBorderTop = solutionBorderTop + 40

        val competitorInfoLeft = right - (width * .45).toInt()

        val tableWidth = competitorInfoLeft - LEFT - 2 * FMC_MARGIN
        val cellHeight = TABLE_HEIGHT / TABLE_LINES
        val firstColumnWidth = tableWidth - (COLUMNS - 1) * CELL_WIDTH

        val table = Table(COLUMNS).apply {
            // FIXME setTotalWidth(floatArrayOf(firstColumnWidth.toFloat(), cellWidth.toFloat(), cellWidth.toFloat(), cellWidth.toFloat(), cellWidth.toFloat(), cellWidth.toFloat(), cellWidth.toFloat()))
            //isLockedWidth = true
        }

        val movesType = listOf(
            Translate.translate("fmc.faceMoves", locale),
            Translate.translate("fmc.rotations", locale)
        )

        val direction = listOf(
            Translate.translate("fmc.clockwise", locale),
            Translate.translate("fmc.counterClockwise", locale),
            Translate.translate("fmc.double", locale)
        )

        val pureMoves = DIRECTION_MODIFIERS.map { mod -> WCA_MOVES.map { mov -> "$mov$mod" } }
        val rotationMoves = DIRECTION_MODIFIERS.map { mod -> WCA_MOVES.map { mov -> "[${mov.toLowerCase()}$mod]" } }

        val movesCell = listOf(pureMoves, rotationMoves)

        val firstColumnRectangle = Rectangle(firstColumnWidth.toFloat(), cellHeight.toFloat())
        val firstColumnBaseFontSize = PdfUtil.fitText(font, movesType[0], firstColumnRectangle, 10f, false, 1f)

        // FIXME where did the "firstColumnBaseFontSize" parameter go?
        // FIXME bold font!!
        val movesTypeMinFont = movesType.map { PdfUtil.fitText(font, it, firstColumnRectangle, 10f, false, 1f) }
            .min() ?: firstColumnBaseFontSize

        // FIXME NOT bold font!!
        // FIXME firstColumnBaseFontSize
        val directionMinFont = direction.map { PdfUtil.fitText(font, it, firstColumnRectangle, 10f, false, 1f) }
            .min() ?: firstColumnBaseFontSize

        val firstColumnFontSize = min(firstColumnBaseFontSize, min(movesTypeMinFont, directionMinFont))

        // Center the table
        val movesTypeMaxWidth = movesType.map { font.getWidth(it, firstColumnFontSize) }.max() ?: 0f
        val directionMaxWidth = direction.map { font.getWidth(it, firstColumnFontSize) }.max() ?: 0f
        val maxFirstColumnWidth = max(movesTypeMaxWidth, directionMaxWidth)

        val lastColumnValues = movesCell.flatMap { c -> c.map { it.last() } }
        val maxLastColumnWidth = lastColumnValues.map { font.getWidth(it, MOVES_FONT_SIZE.toFloat()) }.max() ?: 0f

        for (i in movesType.indices) {
            val foo = Paragraph(movesType[i])
                .setFont(font) // FIXME bold font
                .setFontSize(firstColumnFontSize)

            val explanationStringCell = Cell()
                .add(foo)
                .setHeight(cellHeight.toFloat())
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)

            table.addCell(explanationStringCell)

            val emptyCell = Cell(1, COLUMNS - 1)
                .setHeight(cellHeight.toFloat())
                .setBorder(Border.NO_BORDER)

            table.addCell(emptyCell)

            for (j in DIRECTION_MODIFIERS.indices) {
                val bar = Paragraph(direction[j])
                    .setFont(font)
                    .setFontSize(firstColumnFontSize)

                val directionTitleCell = Cell()
                    .add(bar)
                    .setHeight(cellHeight.toFloat())
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)

                table.addCell(directionTitleCell)

                for (k in WCA_MOVES.indices) {
                    val baz = Paragraph(movesCell[i][j][k])
                        .setFont(font)
                        .setFontSize(MOVES_FONT_SIZE.toFloat())

                    val moveStringCell = Cell().add(baz)
                        .setHeight(cellHeight.toFloat())
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .setBorder(Border.NO_BORDER)

                    table.addCell(moveStringCell)
                }
            }
        }

        add(table)
        // FIXME Position the table
        // table.writeSelectedRows(0, -1, LEFT.toFloat() + FMC_MARGIN.toFloat() + (CELL_WIDTH - maxLastColumnWidth) / 2 - (firstColumnWidth - maxFirstColumnWidth) / 2, (scrambleBorderTop + tableHeight + fmcMargin).toFloat(), this)
    }

    companion object {
        const val BOTTOM = 30
        const val LEFT = 35

        const val FMC_MARGIN = 10

        const val TABLE_HEIGHT = 160
        const val TABLE_LINES = 8
        const val CELL_WIDTH = 25
        const val COLUMNS = 7

        const val FONT_SIZE = 15
        const val MOVES_FONT_SIZE = 10

        const val MARGIN = 5

        const val LEADING_MULTIPLIER = 1f
        const val RULE_FONT_SIZE = 25

        const val LINE_WIDTH = 25
        const val LINES_X = 10

        const val PADDING = 5

        const val FMC_LINE_THICKNESS = 0.5f

        const val UNDERLINE_THICKNESS = 0.2f

        const val MAGIC_NUMBER = 30 // kill me now

        const val FORM_TEMPLATE_WCA_ID = "WCA ID: __ __ __ __  __ __ __ __  __ __"

        const val SHORT_FILL = ": ____"
        const val LONG_FILL = ": __________________"

        val WCA_MOVES = arrayOf("F", "R", "U", "B", "L", "D")
        val DIRECTION_MODIFIERS = arrayOf("", "'", "2")
    }
}
