package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import java.io.File
import java.util.*
import kotlin.math.max
import com.itextpdf.layout.element.List as PdfList

open class FmcSolutionSheet(request: ScrambleRequest, globalTitle: String?, locale: Locale) : FmcSheet(request, globalTitle, locale) {
    override fun PdfDocument.writeContents() {
        val bf = FontUtil.getFontForLocale(locale)

        for (i in scrambleRequest.scrambles.indices) {
            this.addNewPage().addFmcSolutionSheet(title, i, bf)
        }
    }

    protected fun PdfPage.addFmcSolutionSheet(globalTitle: String?, index: Int, bf: PdfFont) {
        val withScramble = index != -1

        val highLevelDocument = Document(document)

        val table = Table(2)
            .useAllAvailableWidth()
            .setAutoLayout()

        table.addRules(bf)
        table.addCompetitorInfo(globalTitle, withScramble, index)
        table.addGrading()
        table.addScramble(withScramble, index, document)
        table.addSolutionSpace(bf)

        highLevelDocument.add(table)
    }

    fun Table.addRules(font: PdfFont) {
        val rulePar = Paragraph()
        val heading = Text(Translate.translate("fmc.event", locale))
            .setBold()
            .setFont(font)
            .setFontSize(25f)
            .setTextAlignment(TextAlignment.CENTER)

        rulePar.add(heading)

        val listing = PdfList()
            .setListSymbol("• ")
            .setFont(font)
            .setKeepTogether(true)
            .setFontSize(5f) // FIXME

        val substitutions = mapOf("maxMoves" to WCA_MAX_MOVES_FMC.toString())

        for (i in 1..6) {
            val ruleTranslation = Translate.translate("fmc.rule$i", locale, substitutions)

            val ruleItemPar = Paragraph(ruleTranslation)
                .setFixedLeading(0.75f) // FIXME

            val ruleListItem = ListItem()
                .add(ruleItemPar) as ListItem

            listing.add(ruleListItem)
        }

        rulePar.add(listing)

        rulePar.addMovesTable(font)

        val ruleCell = Cell(3, 1)
            .add(rulePar)

        addCell(ruleCell)
    }

    fun Paragraph.addMovesTable(font: PdfFont) {
        val direction = listOf(
            Translate.translate("fmc.clockwise", locale),
            Translate.translate("fmc.counterClockwise", locale),
            Translate.translate("fmc.double", locale)
        )

        val pureMoves = WCA_DIRECTION_MODIFIERS.map { mod -> WCA_MOVES.map { mov -> "$mov$mod" } }
        val rotationMoves = WCA_DIRECTION_MODIFIERS.map { mod -> WCA_ROTATIONS.map { mov -> "$mov$mod" } }

        val moveMap = direction.zip(pureMoves).toMap()
        val rotationMap = direction.zip(rotationMoves).toMap()

        val faceMovesLocal = Translate.translate("fmc.faceMoves", locale)
        val rotationsLocal = Translate.translate("fmc.rotations", locale)

        val movesType = mapOf(
            faceMovesLocal to moveMap,
            rotationsLocal to rotationMap
        )

        val maxMovesCount = 6 // FIXME dynamic
        val totalColWidth = maxMovesCount + 1

        val movesTable = Table(totalColWidth)
            .setAutoLayout()
            .setFont(font)
            .setFontSize(5f) // FIXME

        for ((typeHeading, dirGroups) in movesType) {
            val headingParagraph = Paragraph(typeHeading)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFont(font)
                .setFontSize(5f) // FIXME font size correct

            val headingCell = Cell(1, totalColWidth)
                .add(headingParagraph)
                .setBorder(Border.NO_BORDER)

            movesTable.addCell(headingCell)

            for ((dirHeading, moves) in dirGroups) {
                val directionNameLocal = Paragraph(dirHeading)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFont(font)
                    .setFontSize(5f) // FIXME font size correct

                val directionTitleCell = Cell()
                    .add(directionNameLocal)
                    .setBorder(Border.NO_BORDER)

                movesTable.addCell(directionTitleCell)

                val colspan = maxMovesCount / moves.size

                for (move in moves) {
                    val movePar = Paragraph(move)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFont(font)
                        .setFontSize(5f) // FIXME font size correct

                    val moveStringCell = Cell(1, colspan)
                        .add(movePar)
                        .setBorder(Border.NO_BORDER)

                    movesTable.addCell(moveStringCell)
                }
            }
        }

        this.add(movesTable)
    }

    fun Table.addCompetitorInfo(globalTitle: String?, withScramble: Boolean, index: Int) {
        // FIXME newlines are nasty. use listing?!
        val infoPar = Paragraph()

        val showScrambleCount = withScramble && (scrambleRequest.scrambles.size > 1 || scrambleRequest.totalAttempt > 1)

        if (withScramble) {
            val titleText = Text(globalTitle)
                .setTextAlignment(TextAlignment.CENTER)

            infoPar.add(titleText)
            infoPar.add(TEXT_NEWLINE)

            val subtitleText = Text(scrambleRequest.title)
                .setTextAlignment(TextAlignment.CENTER)

            infoPar.add(subtitleText)
            infoPar.add(TEXT_NEWLINE)

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

                val indexText = Text(translatedInfo)
                    .setTextAlignment(TextAlignment.CENTER)

                infoPar.add(indexText)
                infoPar.add(TEXT_NEWLINE)
            }
        } else {
            val competitionDesc = Translate.translate("fmc.competition", locale) + LONG_FILL
            val roundDesc = Translate.translate("fmc.round", locale) + SHORT_FILL
            val attemptDesc = Translate.translate("fmc.attempt", locale) + SHORT_FILL

            // TODO default font and fontSize for infoPar
            infoPar.add(competitionDesc)
            infoPar.add(TEXT_NEWLINE)
            infoPar.add(roundDesc)
            infoPar.add(TEXT_NEWLINE)
            infoPar.add(attemptDesc)
            infoPar.add(TEXT_NEWLINE)
        }

        val competitorDesc = Translate.translate("fmc.competitor", locale) + LONG_FILL
        val registrantIdDesc = Translate.translate("fmc.registrantId", locale) + SHORT_FILL

        infoPar.add(competitorDesc)
        infoPar.add(TEXT_NEWLINE)
        infoPar.add(FORM_TEMPLATE_WCA_ID)
        infoPar.add(TEXT_NEWLINE)
        infoPar.add(registrantIdDesc)

        val infoCell = Cell()
            .add(infoPar)

        this.addCell(infoCell)
    }

    fun Table.addGrading() {
        // FIXME newlines are nasty. Investigate!
        val gradingPar = Paragraph()

        val gradingStringGradedBy = Translate.translate("fmc.graded", locale) + LONG_FILL
        val gradingStringResult = Translate.translate("fmc.result", locale) + SHORT_FILL
        val warningString = Translate.translate("fmc.warning", locale)

        val gradingString = "$gradingStringGradedBy $gradingStringResult"

        val warningText = Text(warningString)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(9f)

        gradingPar.add(warningText)
        gradingPar.add(TEXT_NEWLINE)

        val gradingText = Text(gradingString)
            .setTextAlignment(TextAlignment.CENTER)

        gradingPar.add(gradingText)

        val gradingCell = Cell()
            .add(gradingPar)

        this.addCell(gradingCell)
    }

    fun Table.addScramble(withScramble: Boolean, index: Int, documentForImageRendering: PdfDocument) {
        if (withScramble) {
            val scrambleString = scrambleRequest.scrambles[index]
            val scrambleSvg = scrambleRequest.scrambler.drawScramble(scrambleString, scrambleRequest.colorScheme)

            val img = Image(SvgConverter.convertToXObject(scrambleSvg.toString(), documentForImageRendering))
                .setAutoScale(true)

            this.addCell(img)

            val scramblePrefix = Translate.translate("fmc.scramble", locale)
            val scrambleNote = "$scramblePrefix: $scrambleString"

            val scrambleNoteCell = Cell(1, 2)
                .add(Paragraph(scrambleNote))

            this.addCell(scrambleNoteCell)
        } else {
            val separateSheetAdvice = Translate.translate("fmc.scrambleOnSeparateSheet", locale)
            val separateSheetPar = Paragraph(separateSheetAdvice)
                .setTextAlignment(TextAlignment.CENTER)

            val separateSheetCell = Cell()
                .add(separateSheetPar)

            this.addCell(separateSheetCell)
        }
    }

    fun Table.addSolutionSpace(font: PdfFont) {
        val barSpacesTable = Table(SOLUTION_COLUMNS)
            .useAllAvailableWidth()
            .setFont(font)
            .setFontSize(25f)
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
        const val SOLUTION_ROWS = 8
        const val SOLUTION_COLUMNS = 10

        const val SOLUTION_BAR_SPACE = "__"

        const val FORM_TEMPLATE_WCA_ID = "WCA ID: __ __ __ __  __ __ __ __  __ __"

        const val SHORT_FILL = ": ____"
        const val LONG_FILL = ": __________________"

        const val TEXT_NEWLINE = "\n"

        val WCA_MOVES = arrayOf("F", "R", "U", "B", "L", "D")
        val WCA_ROTATIONS = arrayOf("x", "y", "z")

        val WCA_DIRECTION_MODIFIERS = arrayOf("", "'", "2")

        @JvmStatic
        fun main(args: Array<String>) {
            println("Parsing request")

            val reqMap = mapOf("fooBar" to "333fm*fmc")
            val scrRequest = ScrambleRequest.parseScrambleRequests(reqMap, "trololol").single()
            //.copy(extraScrambles = listOf("R U R' U' R' F R2 U' R' U' R U R' F'"))

            println("Generating sheet")

            val sheet = FmcSolutionSheet(scrRequest, "FMC TNoodle 2019", Translate.DEFAULT_LOCALE)

            println("Rendering…")
            val sheetBytes = sheet.render()

            println("Dumping to file")
            File("/home/suushie_maniac/jvdocs/tnoodle/pdf_debug.pdf").writeBytes(sheetBytes)

            System.exit(123)
        }
    }
}
