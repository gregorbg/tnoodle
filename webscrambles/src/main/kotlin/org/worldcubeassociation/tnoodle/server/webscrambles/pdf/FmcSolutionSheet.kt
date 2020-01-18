package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.*
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontSizeRenderer
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ActivityCode
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ScrambleSet
import java.util.*
import kotlin.math.max
import com.itextpdf.layout.element.List as PdfList

open class FmcSolutionSheet(scrambleSet: ScrambleSet, activityCode: ActivityCode, competitionTitle: String, locale: Locale, hasGroupID: Boolean) : FmcSheet(scrambleSet, activityCode, competitionTitle, locale, hasGroupID) {
    override fun PdfDocument.writeContents() {
        for (i in scrambleSet.scrambles.indices) {
            this.addNewPage()
                .addFmcSolutionSheet(i)
        }
    }

    protected fun PdfPage.addFmcSolutionSheet(index: Int) {
        val withScramble = index != FmcGenericSolutionSheet.INDEX_SKIP_SCRAMBLE
        val bf = FontUtil.getFontForLocale(locale)

        val highLevelDocument = Document(document)

        val table = Table(2)
            .useAllAvailableWidth()
            .setAutoLayout()

        table.addRules(bf)
        table.addCompetitorInfo(withScramble, index)
        table.addGrading()
        table.addScramble(withScramble, index, document)
        table.addSolutionSpace(bf)

        highLevelDocument.add(table)
    }

    fun Table.addRules(font: PdfFont) {
        val rulePar = Paragraph()
            .setHorizontalAlignment(HorizontalAlignment.CENTER)

        val heading = Text(Translate.translate("fmc.event", locale))
            .setBold()
            .setFont(font)
            .setFontSize(TITLE_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)

        rulePar.add(heading)

        val listing = PdfList()
            .setTextAlignment(TextAlignment.LEFT)
            .setListSymbol("• ")
            .setFont(font)
            .setKeepTogether(true)
            .setFontSize(RULE_FONT_SIZE)

        val substitutions = mapOf("maxMoves" to WCA_MAX_MOVES_FMC.toString())

        for (i in 1..6) {
            val ruleTranslation = Translate.translate("fmc.rule$i", locale, substitutions)

            val ruleItemPar = Paragraph(ruleTranslation)
                .setMultipliedLeading(0.7f) // FIXME

            val ruleListItem = ListItem()
                .add(ruleItemPar) as ListItem

            // FIXME leading for list items is dumb!
            ruleListItem.setProperty(Property.LEADING, Leading(Leading.MULTIPLIED, 0.7f))

            listing.add(ruleListItem)
        }

        rulePar.add(listing)

        rulePar.addMovesTable(font)

        val ruleCell = Cell(3, 1)
            .setTextAlignment(TextAlignment.CENTER)
            .add(rulePar)

        addCell(ruleCell)
    }

    fun Paragraph.addMovesTable(font: PdfFont) {
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
        val totalColWidth = maxMovesCount + 1

        val movesTable = Table(totalColWidth)
            .setAutoLayout()
            .setFont(font)
            .setFontSize(MOVES_TABLE_FONT_SIZE)

        // blank cell in the top-right corner
        val blankCornerCell = Cell()
            .setBorder(Border.NO_BORDER)

        movesTable.addCell(blankCornerCell)

        val dividerBorder = SolidBorder(1f)

        for ((typeHeading, typeMoves) in tableData) {
            val headingParagraph = Paragraph(typeHeading)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFont(font)
                .setFontSize(MOVES_TABLE_FONT_SIZE)

            val headingCell = Cell(1, typeMoves.size)
                .add(headingParagraph)
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(dividerBorder)
                .setBorderRight(dividerBorder)

            movesTable.addCell(headingCell)
        }

        for ((dirHeading, modifier) in directionData) {
            val directionNameLocal = Paragraph(dirHeading)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFont(font)
                .setFontSize(MOVES_TABLE_FONT_SIZE)

            val directionTitleCell = Cell()
                .add(directionNameLocal)
                .setBorder(Border.NO_BORDER)

            movesTable.addCell(directionTitleCell)

            for (baseMoves in tableData.values) {
                for ((i, move) in baseMoves.withIndex()) {
                    val compositeMove = "$move$modifier"

                    val movePar = Paragraph(compositeMove)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFont(font)
                        .setFontSize(MOVES_TABLE_FONT_SIZE)

                    val moveStringCell = Cell()
                        .add(movePar)
                        .setBorder(Border.NO_BORDER)

                    if (i == 0) {
                        moveStringCell.setBorderLeft(dividerBorder)
                    }

                    if (i == baseMoves.lastIndex) {
                        moveStringCell.setBorderRight(dividerBorder)
                    }

                    movesTable.addCell(moveStringCell)
                }
            }
        }

        this.add(movesTable)
    }

    fun Table.addCompetitorInfo(withScramble: Boolean, index: Int) {
        // FIXME newlines are nasty. use listing?!
        val infoPar = Paragraph()

        val showScrambleCount = withScramble && (scrambleSet.scrambles.size > 1 || activityCode.attemptNumber != null)

        if (withScramble) {
            infoPar.add(competitionTitle)
            infoPar.add(TEXT_NEWLINE)

            val activityTitle = activityCode.copyParts(attemptNumber = null)
                .compileTitleString(locale, includeGroupID = hasGroupID)

            infoPar.add(activityTitle)
            infoPar.add(TEXT_NEWLINE)

            if (showScrambleCount) {
                // this is for ordered scrambles
                val attemptIndex = activityCode.attemptNumber ?: index
                val orderedIndex = max(attemptIndex, index) + 1

                val substitutions = mapOf(
                    "scrambleIndex" to orderedIndex.toString(),
                    "scrambleCount" to expectedAttemptNum.toString()
                )

                val translatedInfo = Translate.translate("fmc.scrambleXofY", locale, substitutions)

                val indexText = Text(translatedInfo)
                    .setTextAlignment(TextAlignment.CENTER)

                infoPar.add(indexText)
                infoPar.add(TEXT_NEWLINE)
            }
        } else {
            val fillOutGaps = mapOf(
                "fmc.competition" to LONG_FILL,
                "fmc.round" to SHORT_FILL,
                "fmc.attempt" to SHORT_FILL
            )

            for ((key, fill) in fillOutGaps) {
                val translatedText = Translate.translate(key, locale) + fill

                val textElement = Text(translatedText)
                    .setTextAlignment(TextAlignment.LEFT)

                infoPar.add(textElement)
                infoPar.add(TEXT_NEWLINE)
            }
        }

        val competitorDesc = Translate.translate("fmc.competitor", locale) + LONG_FILL
        val registrantIdDesc = Translate.translate("fmc.registrantId", locale) + SHORT_FILL
        val wcaIdTemplate = FORM_TEMPLATE_WCA_ID.replace(' ', NBSP)

        infoPar.add(Text(competitorDesc).setTextAlignment(TextAlignment.LEFT))
        infoPar.add(TEXT_NEWLINE)
        infoPar.add(wcaIdTemplate)
        infoPar.add(TEXT_NEWLINE)
        infoPar.add(registrantIdDesc)

        val infoCell = Cell()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(COMPETITOR_INFO_FONT_SIZE)
            .add(infoPar)

        this.addCell(infoCell)
    }

    fun Table.addGrading() {
        // FIXME newlines are nasty. Investigate!
        val gradingPar = Paragraph()

        val warningString = Translate.translate("fmc.warning", locale)
            .replace(' ', NBSP)

        val warningText = Text(warningString)

        gradingPar.add(warningText)
        gradingPar.add(TEXT_NEWLINE)

        val gradingStringGradedBy = Translate.translate("fmc.graded", locale) + LONG_FILL
        val gradingStringResult = Translate.translate("fmc.result", locale) + SHORT_FILL

        val gradingString = "$gradingStringGradedBy$NBSP$gradingStringResult"

        val gradingText = Text(gradingString)

        gradingPar.add(gradingText)

        val gradingCell = Cell()
            .setFontSize(GRADING_FONT_SIZE)
            .setTextAlignment(TextAlignment.CENTER)
            .add(gradingPar)

        this.addCell(gradingCell)
    }

    fun Table.addScramble(withScramble: Boolean, index: Int, documentForImageRendering: PdfDocument) {
        if (withScramble) {
            val scrambleModel = scrambleSet.scrambles[index]
            val scrambleString = scrambleModel.allScrambleStrings.single()
            val scrambleSvg = scramblingPuzzle.drawScramble(scrambleString, null)

            val img = Image(SvgConverter.convertToXObject(scrambleSvg.toString(), documentForImageRendering))
                .setAutoScale(true)

            val imgCell = Cell()
                .add(img)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            this.addCell(imgCell)

            val scramblePrefix = Translate.translate("fmc.scramble", locale)
            val scrambleNote = "$scramblePrefix:$NBSP$scrambleString"

            val scrambleNotePar = Paragraph(scrambleNote)
                .setFontSize(SCRAMBLE_FONT_SIZE)

            val renderer = FontSizeRenderer(scrambleNotePar)
            scrambleNotePar.setNextRenderer(renderer)

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

    fun Table.addSolutionSpace(font: PdfFont) {
        val barSpacesTable = Table(SOLUTION_COLUMNS)
            .useAllAvailableWidth()
            .setFont(font)
            .setFontSize(SCRAMBLE_DASHES_FONT_SIZE)
            .setBorder(Border.NO_BORDER)

        repeat(SOLUTION_ROWS) {
            repeat(SOLUTION_COLUMNS) {
                val spaceBarCell = Cell()
                    .add(Paragraph(SOLUTION_BAR_SPACE))
                    .setTextAlignment(TextAlignment.CENTER)
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
        const val SCRAMBLE_FONT_SIZE = 140f
        const val SCRAMBLE_DASHES_FONT_SIZE = 18f

        const val SOLUTION_ROWS = 8
        const val SOLUTION_COLUMNS = 10

        const val NBSP = Typography.nbsp

        const val SOLUTION_BAR_SPACE = "___"

        const val FORM_TEMPLATE_WCA_ID = "WCA ID: __ __ __ __  __ __ __ __  __ __"

        const val SHORT_FILL = ":${NBSP}____"
        const val LONG_FILL = ":${NBSP}__________________"

        const val TEXT_NEWLINE = "\n"

        val WCA_MOVES = arrayOf("R", "U", "F", "L", "D", "B")
        val WCA_ROTATIONS = arrayOf("x", "y", "z")

        val WCA_DIRECTION_MODIFIERS = arrayOf("", "'", "2")

        const val FMC_LINE_THICKNESS = 0.5f
        //const val UNDERLINE_THICKNESS = 0.2f

        //const val LEADING_MULTIPLIER = 1.3f
    }
}
