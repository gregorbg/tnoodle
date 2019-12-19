package org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util

import com.itextpdf.text.Chunk
import com.itextpdf.text.Font
import com.itextpdf.text.Rectangle

object PdfUtil {
    const val NON_BREAKING_SPACE = '\u00A0'
    const val TEXT_PADDING_HORIZONTAL = 1

    fun String.splitToLineChunks(font: Font, textColumnWidth: Float): List<Chunk> {
        val availableTextWidth = textColumnWidth - 2 * TEXT_PADDING_HORIZONTAL

        return split("\n").dropLastWhile { it.isEmpty() }
            .flatMap { it.splitLineToChunks(font, availableTextWidth) }
            .map { it.toLineWrapChunk(font) }
    }

    fun String.splitLineToChunks(font: Font, availableTextWidth: Float): List<String> {
        val lineChunks = mutableListOf<String>()
        val cutIndices = mutableListOf<Int>()

        for (i in indices) {
            // Walk past all whitespace that comes immediately after
            // the last line wrap we just inserted.
            if (this[i] == ' ') {
                continue
            }

            val cuttingProgress = cutIndices.max() ?: 0

            if (i < cuttingProgress) {
                continue
            }

            val optimalCutIndex = optimalCutIndex(i, font, availableTextWidth)
            cutIndices.add(optimalCutIndex)

            val substring = substring(i, optimalCutIndex).padNbsp()
                .fillToWidthMax(NON_BREAKING_SPACE.toString(), font, availableTextWidth)

            lineChunks.add(substring)
        }

        return lineChunks
    }

    private fun String.padNbsp() = NON_BREAKING_SPACE + this + NON_BREAKING_SPACE

    fun String.optimalCutIndex(startIndex: Int, font: Font, availableTextWidth: Float): Int {
        val endIndex = longestFittingSubstringIndex(font, availableTextWidth, startIndex)

        // If we're not at the end of the text, make sure we're not cutting
        // a word (or turn) in half by walking backwards until we're right before a turn.
        if (endIndex < length) {
            return tryBackwardsWordEndIndex(startIndex, endIndex)
        }

        return endIndex
    }

    fun String.longestFittingSubstringIndex(font: Font, maxWidth: Float, startIndex: Int = 0, fallback: Int = startIndex): Int {
        val searchRange = startIndex..length

        val endpoint = searchRange.findLast {
            val substring = substring(startIndex, it).padNbsp()
            val substringWidth = font.baseFont.getWidthPoint(substring, font.size)

            substringWidth <= maxWidth
        }

        return endpoint ?: fallback
    }

    fun String.tryBackwardsWordEndIndex(frontStopIndex: Int = 0, endIndex: Int = lastIndex, fallback: Int = endIndex): Int {
        for (perfectFitIndex in endIndex downTo frontStopIndex) {
            // Another dirty hack for sq1: turns only line up
            // nicely if every line starts with a (x,y). We ensure this
            // by forcing every line to end with a /.
            val isSquareOne = "/" in this

            // Any spaces added for padding after a turn are considered part of
            // that turn because they're actually NON_BREAKING_SPACE, not a ' '.
            val terminatingChar = if (isSquareOne) '/' else ' '
            val indexBias = if (isSquareOne) 1 else 0

            if (this[perfectFitIndex - indexBias] == terminatingChar) {
                return perfectFitIndex
            }
        }

        // We walked all the way to the beginning of the line
        // without finding a good breaking point.
        // Give up and break in the middle of a word =(.
        return fallback
    }

    fun String.fillToWidthMax(padding: String, font: Font, maxLength: Float): String {
        val paddingList = mutableListOf<String>()

        // Add $padding until the substring takes up as much
        // space as is available on a line.
        do {
            paddingList.add(padding)

            val currentPadding = paddingList.joinToString("")
            val paddedString = this + currentPadding

            val substringWidth = font.baseFont.getWidthPoint(paddedString, font.size)
        } while (substringWidth <= maxLength)

        // substring is now too big for our line, so remove the
        // last character.
        return this + paddingList.drop(1).joinToString("")
    }

    fun String.toLineWrapChunk(font: Font) = Chunk(this).apply {
        this.font = font

        // Force a line wrap!
        append("\n")
    }

    private val FITTEXT_FONTSIZE_PRECISION = 0.1f

    /**
     * Adapted from ColumnText.java in the itextpdf 5.3.0 source code.
     * Added the newlinesAllowed argument.
     *
     * Fits the text to some rectangle adjusting the font size as needed.
     * @param font the font to use
     * @param text the text
     * @param rect the rectangle where the text must fit
     * @param maxFontSize the maximum font size
     * @param newlinesAllowed output text can be split into lines
     * @param leadingMultiplier leading multiplier between lines
     *
     * @return the calculated font size that makes the text fit
     */
    fun fitText(font: Font, text: String, rect: Rectangle, maxFontSize: Float, newlinesAllowed: Boolean, leadingMultiplier: Float): Float {
        // ideally, we could pass the object in which our text is going to be rendered
        // as argument instead of asking leadingMultiplier, but we are currently rendering
        // text in pdfcell, columntext and others
        // it'd be painful to render lines in a common object to ask leadingMultiplier
        return estimateByAverageInterval(1f, maxFontSize, FITTEXT_FONTSIZE_PRECISION) {
            // FIXME inplace modification is no good
            font.size = it

            val lineChunks = text.splitToLineChunks(font, rect.width)

            // The font size seems to be a pretty good estimate for how
            // much vertical space a row actually takes up.
            val heightPerLine = it * leadingMultiplier
            val totalHeight = lineChunks.size.toFloat() * heightPerLine

            val shouldIncrease = totalHeight < rect.height
            // If newlines are NOT allowed, but we had to split the text into more than
            // one line, then our current guess is too large.
            val mustNotIncrease = !newlinesAllowed && lineChunks.size > 1

            shouldIncrease && !mustNotIncrease
        }
    }

    private fun estimateByAverageInterval(min: Float, max: Float, precision: Float, shouldIncrease: (Float) -> Boolean): Float {
        if (max - min < precision) {
            // Ground recursion: We have converged arbitrarily close to some target value.
            return min
        }

        val potentialFontSize = (min + max) / 2f
        val iterationShouldIncrease = shouldIncrease(potentialFontSize)

        return if (iterationShouldIncrease) {
            estimateByAverageInterval(potentialFontSize, max, precision, shouldIncrease)
        } else {
            estimateByAverageInterval(min, potentialFontSize, precision, shouldIncrease)
        }
    }
}
