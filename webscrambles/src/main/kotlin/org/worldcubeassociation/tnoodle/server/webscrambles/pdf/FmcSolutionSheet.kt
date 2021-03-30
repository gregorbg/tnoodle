package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.*
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.model.EventData
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.PdfUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ActivityCode
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.Scramble
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ScrambleSet
import java.io.File
import java.util.*
import kotlin.math.max
import com.itextpdf.layout.element.List as PdfList

open class FmcSolutionSheet(
    scrambleSet: ScrambleSet,
    activityCode: ActivityCode,
    competitionTitle: String,
    locale: Locale,
    hasGroupID: Boolean
) : FmcSheet(scrambleSet, activityCode, competitionTitle, locale, hasGroupID) {
    override fun Document.addFmcSheet(index: Int) {
        setMargins(SHEET_MARGIN, SHEET_MARGIN, SHEET_MARGIN, SHEET_MARGIN)

        val includeScramble = index != FmcGenericSolutionSheet.INDEX_SKIP_SCRAMBLE

        val table = Table(2)
            .useAllAvailableWidth()
            .setAutoLayout()

        table.addRules()
        table.addCompetitorInfo(includeScramble, index)
        table.addGrading()
        table.addScramble(includeScramble, index, pdfDocument)
        table.addSolutionSpace()

        this.add(table)
    }

    fun Table.addRules() {
        val rulePar = Paragraph()
            .setMultipliedLeading(NARROW_TEXT_LEADING)

        val heading = Text(Translate.translate("fmc.event", locale))
            .setBold()
            .setFont(localFont)
            .setFontSize(TITLE_FONT_SIZE)

        rulePar.add(heading)

        val listing = PdfList()
            .setTextAlignment(TextAlignment.LEFT)
            .setListSymbol(LIST_SYMBOL_RULES)
            .setFont(localFont)
            .setFontSize(RULE_FONT_SIZE)
            .setKeepTogether(true)

        val substitutions = mapOf("maxMoves" to WCA_MAX_MOVES_FMC.toString())

        for (i in 1..FMC_RULES_COUNT) {
            val ruleTranslation = Translate.translate("fmc.rule$i", locale, substitutions)

            val ruleItemPar = Paragraph(ruleTranslation)
                .setMultipliedLeading(RULES_LISTING_LEADING)

            val ruleListItem = ListItem()
                .apply { add(ruleItemPar) }

            listing.add(ruleListItem)
        }

        rulePar.add(listing)
        rulePar.addMovesTable()

        val ruleCell = Cell(3, 1)
            .setTextAlignment(TextAlignment.CENTER)
            .add(rulePar)

        addCell(ruleCell)
    }

    fun Paragraph.addMovesTable() {
        val directionKeys = listOf(
            "fmc.clockwise",
            "fmc.counterClockwise",
            "fmc.double"
        )

        val directionData = directionKeys
            .map { Translate.translate(it, locale) }
            .zip(WCA_DIRECTION_MODIFIERS)
            .toMap()

        val tableConfig = mapOf(
            "fmc.faceMoves" to WCA_MOVES,
            "fmc.rotations" to WCA_ROTATIONS
        )

        val tableData = tableConfig
            .mapKeys { Translate.translate(it.key, locale) }

        val maxMovesCount = tableConfig.values.sumBy { it.size }
        val totalColWidth = maxMovesCount + tableConfig.size + 1

        val movesTable = Table(totalColWidth)
            .setAutoLayout()
            .setFontSize(MOVES_TABLE_FONT_SIZE)
            .setMarginTop(MOVE_TABLE_TOP_MARGIN)

        // blank cell in the top-right corner
        val blankCornerCell = Cell()
            .setBorder(Border.NO_BORDER)

        movesTable.addCell(blankCornerCell)

        for ((typeHeading, typeMoves) in tableData) {
            val verticalGapCell = Cell(directionData.size + 1, 1)
                .setPadding(MOVE_TABLE_PADDING)
                .setBorder(Border.NO_BORDER)

            movesTable.addCell(verticalGapCell)

            val headingParagraph = Paragraph(typeHeading)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFont(localFont)
                .setFontSize(MOVES_TABLE_FONT_SIZE)

            val headingCell = Cell(1, typeMoves.size)
                .add(headingParagraph)
                .setBorder(Border.NO_BORDER)

            movesTable.addCell(headingCell)
        }

        for ((dirHeading, modifier) in directionData) {
            val directionNameLocal = Paragraph(dirHeading)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFont(localFont)
                .setFontSize(MOVES_TABLE_FONT_SIZE)
                .setMultipliedLeading(NARROW_TEXT_LEADING)

            val directionTitleCell = Cell()
                .add(directionNameLocal)
                .setBorder(Border.NO_BORDER)

            movesTable.addCell(directionTitleCell)

            for (baseMoves in tableData.values) {
                for (move in baseMoves) {
                    val compositeMove = "$move$modifier"

                    val movePar = Paragraph(compositeMove)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFont(FontUtil.NOTO_SANS_FONT)
                        .setFontSize(MOVES_TABLE_FONT_SIZE)
                        .setMultipliedLeading(NARROW_TEXT_LEADING)

                    val moveStringCell = Cell()
                        .add(movePar)
                        .setBorder(Border.NO_BORDER)

                    movesTable.addCell(moveStringCell)
                }
            }
        }

        this.add(movesTable)
    }

    fun Table.addCompetitorInfo(includeScramble: Boolean, index: Int) {
        val infoPar = Paragraph()

        val competitionListing = PdfList()
            .setListSymbol(LIST_SYMBOL_BLANK)
            .setTextAlignment(TextAlignment.CENTER)
            .setMultipliedLeading(NARROW_TEXT_LEADING)

        if (includeScramble) {
            competitionListing.add(competitionTitle)

            val activityTitle = activityCode.copyParts(attemptNumber = null)
                .compileTitleString(locale, includeGroupID = hasGroupID)

            competitionListing.add(activityTitle)

            val showScrambleCount =
                includeScramble && (scrambleSet.scrambles.size > 1 || activityCode.attemptNumber != null)

            if (showScrambleCount) {
                // this is for ordered scrambles
                val attemptIndex = activityCode.attemptNumber ?: index
                val orderedIndex = max(attemptIndex, index) + 1

                val substitutions = mapOf(
                    "scrambleIndex" to orderedIndex.toString(),
                    "scrambleCount" to expectedAttemptNum.toString()
                )

                val translatedInfo = Translate.translate("fmc.scrambleXofY", locale, substitutions)
                competitionListing.add(translatedInfo)
            }
        } else {
            val fillOutGaps = mapOf(
                "fmc.competition" to LONG_FILL,
                "fmc.round" to SHORT_FILL,
                "fmc.attempt" to SHORT_FILL
            )

            for ((key, fill) in fillOutGaps) {
                val translatedText = Translate.translate(key, locale) + fill
                competitionListing.add(translatedText)
            }

            competitionListing
                .setTextAlignment(TextAlignment.LEFT)
                .setMultipliedLeading(HANDWRITTEN_GAPS_LEADING)
        }

        infoPar.add(competitionListing)

        val competitorListing = PdfList()
            .setListSymbol(LIST_SYMBOL_BLANK)
            .setTextAlignment(TextAlignment.LEFT)
            .setMultipliedLeading(HANDWRITTEN_GAPS_LEADING)

        val competitorDesc = Translate.translate("fmc.competitor", locale) + LONG_FILL
        val registrantIdDesc = Translate.translate("fmc.registrantId", locale) + SHORT_FILL

        competitorListing.add(competitorDesc)
        competitorListing.add(FORM_TEMPLATE_WCA_ID)
        competitorListing.add(registrantIdDesc)

        infoPar.add(competitorListing)

        val infoCell = Cell()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(COMPETITOR_INFO_FONT_SIZE)
            .add(infoPar)

        this.addCell(infoCell)
    }

    fun Table.addGrading() {
        val gradingPar = PdfList()
            .setListSymbol(LIST_SYMBOL_BLANK)
            .setTextAlignment(TextAlignment.CENTER)
            .setFont(localFont)
            .setFontSize(GRADING_FONT_SIZE)
            .setMultipliedLeading(GRADING_GAPS_LEADING)

        val warningString = Translate.translate("fmc.warning", locale)

        gradingPar.add(warningString)

        val gradingStringGradedBy = Translate.translate("fmc.graded", locale) + LONG_FILL
        val gradingStringResult = Translate.translate("fmc.result", locale) + SHORT_FILL

        val gradingString = "$gradingStringGradedBy$NBSP$gradingStringResult"

        gradingPar.add(gradingString)

        val gradingCell = Cell()
            .add(gradingPar)

        this.addCell(gradingCell)
    }

    fun Table.addScramble(includeScramble: Boolean, index: Int, documentForImageRendering: PdfDocument) {
        if (includeScramble) {
            val scrambleModel = scrambleSet.scrambles[index]
            val scrambleString = scrambleModel.allScrambleStrings.single()
            val scrambleSvg = scramblingPuzzle.drawScramble(scrambleString, null)

            val img = Image(SvgConverter.convertToXObject(scrambleSvg.toString(), documentForImageRendering))
                .setAutoScale(true)

            val imgCell = Cell()
                .add(img)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            this.addCell(imgCell)

            val scramblePrefix = "${Translate.translate("fmc.scramble", locale)}:$NBSP"
            val scramblePrefixText = Text(scramblePrefix)
                .setFont(localFont)
                .setFontSize(SCRAMBLE_FONT_SIZE)

            val scrambleMonoString = scrambleString.replace(' ', NBSP)
            val scrambleText = Text(scrambleMonoString)
                .setFont(FontUtil.NOTO_SANS_FONT)

            val scrambleNotePar = Paragraph()
                .setMultipliedLeading(NARROW_TEXT_LEADING)
                .add(scramblePrefixText)
                .add(scrambleText)

            val prefixWidth = localFont.getWidth(scramblePrefix, SCRAMBLE_FONT_SIZE)
            val remaining = PAGE_SIZE.width - prefixWidth - 4 * SHEET_MARGIN

            val idealFontSize = PdfUtil.binarySearchInc(SCRAMBLE_FONT_SIZE / 2, SCRAMBLE_FONT_SIZE * 2, 0.125f) {
                FontUtil.NOTO_SANS_FONT.getWidth(scrambleMonoString, it) < remaining
            }

            scrambleNotePar.setFontSize(idealFontSize)

            val scrambleNoteCell = Cell(1, 2)
                .setTextAlignment(TextAlignment.CENTER)
                .add(scrambleNotePar)

            this.addCell(scrambleNoteCell)
        } else {
            val separateSheetAdvice = Translate.translate("fmc.scrambleOnSeparateSheet", locale)

            val separateSheetCell = Cell()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(SCRAMBLE_FONT_SIZE)
                .add(Paragraph(separateSheetAdvice))

            this.addCell(separateSheetCell)
        }
    }

    fun Table.addSolutionSpace() {
        val barSpacesTable = Table(SOLUTION_COLUMNS)
            .useAllAvailableWidth()
            .setFont(localFont)
            .setFontSize(SCRAMBLE_DASHES_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)

        repeat(SOLUTION_ROWS) {
            repeat(SOLUTION_COLUMNS) {
                val spaceBarCell = Cell()
                    .add(Paragraph(SOLUTION_BAR_SPACE))
                    .setBorder(Border.NO_BORDER)

                barSpacesTable.addCell(spaceBarCell)
            }
        }

        val solutionCell = Cell(1, 2)
            .add(barSpacesTable)

        this.addCell(solutionCell)
    }

    companion object {
        const val TITLE_FONT_SIZE = 25f
        const val RULE_FONT_SIZE = 11f
        const val MOVES_TABLE_FONT_SIZE = 9f
        const val COMPETITOR_INFO_FONT_SIZE = 12f
        const val GRADING_FONT_SIZE = 9f
        const val SCRAMBLE_FONT_SIZE = 15f
        const val SCRAMBLE_DASHES_FONT_SIZE = 18f

        const val SOLUTION_ROWS = 8
        const val SOLUTION_COLUMNS = 10

        const val SHEET_MARGIN = 36f

        const val MOVE_TABLE_PADDING = 5f
        const val MOVE_TABLE_TOP_MARGIN = 5f

        const val RULES_LISTING_LEADING = 0.7f
        const val NARROW_TEXT_LEADING = 0.9f
        const val GRADING_GAPS_LEADING = 1.1f
        const val HANDWRITTEN_GAPS_LEADING = 1.6f

        const val LIST_SYMBOL_RULES = "• "
        const val LIST_SYMBOL_BLANK = ""

        const val NBSP = Typography.nbsp

        const val SOLUTION_BAR_SPACE = "___"

        const val FORM_TEMPLATE_WCA_ID =
            "WCA${NBSP}ID:${NBSP}__${NBSP}__${NBSP}__${NBSP}__${NBSP}${NBSP}${NBSP}__${NBSP}__${NBSP}__${NBSP}__${NBSP}${NBSP}${NBSP}__${NBSP}__"

        const val SHORT_FILL = ":${NBSP}____"
        const val LONG_FILL = ":${NBSP}__________________"

        const val FMC_RULES_COUNT = 6

        const val WCA_MAX_MOVES_FMC = 80

        val WCA_MOVES = arrayOf("R", "U", "F", "L", "D", "B")
        val WCA_ROTATIONS = arrayOf("x", "y", "z")

        val WCA_DIRECTION_MODIFIERS = arrayOf("", "'", "2")

        fun PdfList.setMultipliedLeading(leading: Float) =
            apply { setProperty(Property.LEADING, Leading(Leading.MULTIPLIED, leading)) }
    }
}
