package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.model.dsl.DocumentBuilder
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.model.dsl.TableBuilder
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.model.properties.*
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.model.properties.Paper.inchesToPixel
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.model.properties.Paper.pixelsToInch
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil.joinToStringWithPadding
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.ScramblePhrase
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.*
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

class GeneralScrambleSheet(
    val scrambleSet: ScrambleSet,
    val tNoodleVersion: String,
    competitionTitle: String,
    activityCode: ActivityCode,
    hasGroupId: Boolean,
    locale: Locale,
    watermark: String? = null
) : ScrambleSheet(competitionTitle, activityCode, hasGroupId, locale, watermark) {
    override val scrambles: List<Scramble>
        get() = scrambleSet.allScrambles

    override val scrambleSetId: Int
        get() = scrambleSet.id

    private fun TableBuilder.scrambleRows(
        scramblePhrases: List<ScramblePhrase>,
        scrLineHeight: Float,
        scrImageWidth: Float,
        unitToInches: Float,
        labelPrefix: String? = null
    ) {
        val highestLineCount = scramblePhrases.maxOf { it.lineTokens.size }
        val useHighlighting = highestLineCount >= MIN_LINES_HIGHLIGHTING

        for ((index, scramble) in scramblePhrases.withIndex()) {
            row {
                cell {
                    verticalAlignment = Alignment.Vertical.MIDDLE
                    horizontalAlignment = Alignment.Horizontal.CENTER

                    val labelString = "${labelPrefix.orEmpty()}${index + 1}".trim()
                    text(labelString)
                }

                cell {
                    horizontalAlignment = Alignment.Horizontal.JUSTIFIED
                    verticalAlignment = Alignment.Vertical.MIDDLE

                    leading = SCRAMBLE_TEXT_LEADING
                    padding = 2 * Drawing.Padding.DEFAULT

                    paragraph {
                        fontName = Font.MONO
                        fontSize = scramble.fontSize

                        val rawScrambleLines = scramble.lineTokens.map {
                            it.joinToStringWithPadding(" ", ScramblePhrase.NBSP_STRING)
                        }

                        val maxLength = rawScrambleLines.maxOf { it.length }

                        for ((ln, scrLine) in rawScrambleLines.withIndex()) {
                            val paddedLine = scrLine.padEnd(maxLength, Typography.nbsp)

                            line(paddedLine) {
                                if (useHighlighting && ln % 2 == 1) {
                                    background = SCRAMBLE_HIGHLIGHTING_COLOR
                                }
                            }
                        }
                    }
                }

                cell {
                    background = SCRAMBLE_BACKGROUND_COLOR

                    val scrImageWidthPx = (scrImageWidth * unitToInches).inchesToPixel - (2 * padding + 1)
                    val scrLineHeightPx = (scrLineHeight * unitToInches).inchesToPixel - (2 * padding + 1)

                    svgScrambleImage(scramble.scramble, scrImageWidthPx, scrLineHeightPx)
                }
            }
        }
    }

    private fun computeScramblePhrases(
        scramblePageChunk: List<Pair<String, Boolean>>,
        availableHeight: Float,
        availableTextWidth: Float,
        unitToInches: Float
    ): List<ScramblePhrase> {
        val basicScramblePhrases = scramblePageChunk.map { (scr, isExtra) ->
            FontUtil.generatePhrase(
                scr,
                isExtra,
                availableHeight,
                availableTextWidth,
                unitToInches,
                SCRAMBLE_TEXT_LEADING
            )
        }

        val smallestFontSize = basicScramblePhrases.minOf { it.fontSize }
        val allOneLine = basicScramblePhrases.all { it.lineTokens.size == 1 }

        return basicScramblePhrases.map {
            val breakChunks = if (allOneLine)
                it.scramble.split(" ")
                    .map(::listOf) else
                        FontUtil.splitAtPossibleBreaks(it.rawTokens)

            val maxLineTokens = FontUtil.splitToFixedSizeLines(
                breakChunks,
                smallestFontSize,
                availableTextWidth,
                unitToInches,
                ScramblePhrase.NBSP_STRING
            )

            it.copy(lineTokens = maxLineTokens, fontSize = smallestFontSize)
        }
    }

    override fun DocumentBuilder.writeContents() {
        showHeaderTimestamp = true

        val allScrambles = ScramblePhrase.splitScrambleSet(scrambleSet)
        val scramblePageChunks = allScrambles.chunked(MAX_SCRAMBLES_PER_PAGE)

        val scramblerPreferredSize = scramblingPuzzle.preferredSize
        val scramblerWidthToHeight = scramblerPreferredSize.width.toFloat() / scramblerPreferredSize.height

        showPageNumbers = scramblePageChunks.size > 1

        val roundDetails = activityCode.compileTitleString(locale, true, hasGroupId)

        for (scramblePageChunk in scramblePageChunks) {
            page {
                headerLines = competitionTitle to roundDetails
                footerLine = "Generated by $tNoodleVersion" // TODO i18n

                val heightExtraPenalty =
                    if (scrambleSet.extraScrambles.isNotEmpty()) 2 * EXTRA_SCRAMBLE_LABEL_SIZE else 0f

                val tableWidthIn = availableWidthIn
                val tableHeightIn = availableHeightIn - ceil(heightExtraPenalty).toInt().pixelsToInch

                table(3) {
                    // PDF tables are calculated by *relative* width. So to figure out the scramble image width we...
                    // 1. interpret the page height as a multiple of the unit page width
                    val relativeHeight = tableHeightIn / tableWidthIn
                    // 2. split it by however many scrambles we want,
                    val relHeightPerScramble = relativeHeight / scramblePageChunk.size
                    // 3. scale those heights by the preferred proportions of the scramblers
                    val fullWidth = relHeightPerScramble * scramblerWidthToHeight
                    // and finally limit it down to one third of the page.
                    val scrambleImageProportion = 1 / fullWidth
                    val scrambleImageParts = max(3f, scrambleImageProportion)

                    // label column is 1/25, scrambles are 1/scrambleImageParts.
                    // poor man's LCM: 25*scrambleImageParts :)
                    val gcd = 25 * scrambleImageParts
                    // 25 parts go to label column, $scrambleImageParts parts go to the scramble Image.
                    val scrambleStringParts = gcd - 25 - scrambleImageParts
                    // finally, put it all together :)
                    // when calculating the GCD, we extended 1/25 by n and 1/n by 25.
                    // that's why the column order _seems_ to be flipped around here.
                    // but proportionally everything is in order!
                    relativeWidths = listOf(scrambleImageParts, scrambleStringParts, 25f)

                    val totalWidth = relativeWidths.sum()

                    val scrambleImageWidth = 25f / totalWidth
                    val scrambleTextWidth = scrambleStringParts / totalWidth

                    val desiredPaddingPx = 2 * Drawing.Padding.DEFAULT
                    val paddingPenalty = (desiredPaddingPx * 2).pixelsToInch / tableWidthIn

                    val chunkHeight = (relHeightPerScramble - paddingPenalty) * SCRAMBLE_ONE_PAGE_BACKOFF
                    val chunkWidth = (scrambleTextWidth - paddingPenalty) * SCRAMBLE_ONE_PAGE_BACKOFF

                    val scramblePhrases = computeScramblePhrases(
                        scramblePageChunk,
                        chunkHeight,
                        chunkWidth,
                        tableWidthIn
                    )

                    val (standardScrambles, extraScrambles) = scramblePhrases.partition { !it.isExtra }

                    scrambleRows(
                        standardScrambles,
                        relHeightPerScramble,
                        scrambleImageWidth,
                        tableWidthIn
                    )

                    if (extraScrambles.isNotEmpty()) {
                        row(1) {
                            cell {
                                horizontalAlignment = Alignment.Horizontal.CENTER

                                border = Drawing.Border.NONE
                                padding = 2 * Drawing.Padding.DEFAULT

                                text(TABLE_HEADING_EXTRA_SCRAMBLES) {
                                    fontWeight = Font.Weight.BOLD
                                    fontSize = EXTRA_SCRAMBLE_LABEL_SIZE
                                }
                            }
                        }

                        scrambleRows(
                            extraScrambles,
                            relHeightPerScramble,
                            scrambleImageWidth,
                            tableWidthIn,
                            EXTRA_SCRAMBLE_PREFIX
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val MAX_SCRAMBLES_PER_PAGE = 7
        const val MIN_LINES_HIGHLIGHTING = 4

        const val SCRAMBLE_TEXT_LEADING = 1.2f

        const val SCRAMBLE_ONE_PAGE_BACKOFF = 1f

        const val TABLE_HEADING_EXTRA_SCRAMBLES = "Extra Scrambles" // TODO i18n
        const val EXTRA_SCRAMBLE_PREFIX = "E"

        const val EXTRA_SCRAMBLE_LABEL_SIZE = Font.Size.DEFAULT

        val SCRAMBLE_BACKGROUND_COLOR = RgbColor(192, 192, 192)
        val SCRAMBLE_HIGHLIGHTING_COLOR = RgbColor(230, 230, 230)
    }
}
