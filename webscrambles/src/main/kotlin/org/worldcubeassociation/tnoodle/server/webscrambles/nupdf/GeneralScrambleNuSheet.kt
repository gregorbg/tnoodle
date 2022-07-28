package org.worldcubeassociation.tnoodle.server.webscrambles.nupdf

import org.worldcubeassociation.tnoodle.server.webscrambles.nupdf.model.dsl.DocumentBuilder
import org.worldcubeassociation.tnoodle.server.webscrambles.nupdf.model.dsl.TableBuilder
import org.worldcubeassociation.tnoodle.server.webscrambles.nupdf.model.properties.*
import org.worldcubeassociation.tnoodle.server.webscrambles.nupdf.model.properties.Paper.pixelsToInch
import org.worldcubeassociation.tnoodle.server.webscrambles.nupdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.nupdf.util.SheetRowScramble
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.*
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class GeneralScrambleNuSheet(val scrambleSet: ScrambleSet, activityCode: ActivityCode, locale: Locale) :
    BaseScrambleNuSheet(activityCode, locale) {
    private fun TableBuilder.scrambleRows(
        scrambles: List<String>,
        scrTextHeight: Float,
        scrTextWidth: Float,
        scrImageWidth: Float,
        baseUnit: Float,
        labelPrefix: String? = null
    ) {
        val basicScramblePhrases = scrambles.map {
            FontUtil.generatePhrase(it, scrTextHeight * 0.75f, scrTextWidth, baseUnit) // FIXME magic number
        }

        val smallestFontSize = basicScramblePhrases.minOf { it.fontSize }

        val scramblePhrases = basicScramblePhrases.map {
            val breakChunks = FontUtil.splitAtPossibleBreaks(it.rawTokens)
            val maxLineTokens = FontUtil.splitToFixedSizeLines(breakChunks, smallestFontSize, scrTextWidth, baseUnit)

            it.copy(lineTokens = maxLineTokens, fontSize = smallestFontSize)
        }

        val useHighlighting = scramblePhrases.any { it.lineTokens.size >= MIN_LINES_HIGHLIGHTING }

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

                    padding = 2 * Drawing.Padding.DEFAULT

                    paragraph {
                        fontName = Font.MONO
                        fontSize = scramble.fontSize

                        for ((ln, scrLine) in scramble.lineTokens.withIndex()) {
                            val joinedLine = scrLine.joinToString(" ")

                            line(joinedLine) {
                                if (useHighlighting && ln % 2 == 1) {
                                    background = SCRAMBLE_HIGHLIGHTING_COLOR
                                }
                            }
                        }
                    }
                }

                cell {
                    background = SCRAMBLE_BACKGROUND_COLOR

                    // TODO okay if we round here?
                    val scrImageWidthPx = (scrImageWidth * baseUnit * Paper.DPI).roundToInt()
                    svgScrambleImage(scramble.scramble, scrImageWidthPx)
                }
            }
        }
    }

    override fun DocumentBuilder.writeContents() {
        val allScrambles = SheetRowScramble.fromScrambleSet(scrambleSet, activityCode.eventModel)
        val scramblePageChunks = allScrambles.chunked(MAX_SCRAMBLES_PER_PAGE)

        val scramblerPreferredSize = scramblingPuzzle.preferredSize
        val scramblerWidthToHeight = scramblerPreferredSize.width.toFloat() / scramblerPreferredSize.height

        for (scramblePageChunk in scramblePageChunks) {
            page {
                val standardScrambles = scramblePageChunk.filter { it.isStandard }.map { it.scramble }
                val extraScrambles = scramblePageChunk.filter { it.isExtra }.map { it.scramble }

                val heightExtraPenalty = if (extraScrambles.isNotEmpty()) 2 * EXTRA_SCRAMBLE_LABEL_SIZE else 0f
                val paddingHeightPenalty = scramblePageChunk.size * 2 * Drawing.Padding.DEFAULT

                val actualWidthIn = size.widthIn - (marginLeft + marginRight).pixelsToInch
                val actualHeightIn = size.heightIn - (marginTop + marginBottom).pixelsToInch -
                    ceil(heightExtraPenalty).toInt().pixelsToInch -
                    paddingHeightPenalty.pixelsToInch

                table(3) {
                    // PDF tables are calculated by *relative* width. So to figure out the scramble image width we...
                    // 1. interpret the page height as a multiple of the unit page width
                    val relativeHeight = actualHeightIn / actualWidthIn
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

                    // TODO magic number hack
                    val scrambleDisplayWidth = scrambleStringParts / totalWidth - 0.02f
                    val scrambleImageWidth = 25f / totalWidth

                    scrambleRows(
                        standardScrambles,
                        relHeightPerScramble,
                        scrambleDisplayWidth,
                        scrambleImageWidth,
                        actualWidthIn
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
                            scrambleDisplayWidth,
                            scrambleImageWidth,
                            actualWidthIn,
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

        const val TABLE_HEADING_EXTRA_SCRAMBLES = "Extra Scrambles"
        const val EXTRA_SCRAMBLE_PREFIX = "E"

        const val EXTRA_SCRAMBLE_LABEL_SIZE = Font.Size.DEFAULT

        val SCRAMBLE_BACKGROUND_COLOR = RgbColor(192, 192, 192)
        val SCRAMBLE_HIGHLIGHTING_COLOR = RgbColor(230, 230, 230)
    }
}
