package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.UnitValue
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import java.io.File

class FmcScrambleCutoutSheet(request: ScrambleRequest, globalTitle: String?): FmcSheet(request, globalTitle) {
    override fun PdfDocument.writeContents() {
        for (i in scrambleRequest.scrambles.indices) {
            this.addNewPage()
                .addFmcScrambleCutoutSheet(scrambleRequest, title, i)
        }
    }

    private fun PdfPage.addFmcScrambleCutoutSheet(scrambleRequest: ScrambleRequest, globalTitle: String?, index: Int) {
        val scramble = scrambleRequest.scrambles[index]

        val svg = scrambleRequest.scrambler.drawScramble(scramble, scrambleRequest.colorScheme)

        val scrambleSuffix = " - Scramble ${index + 1} of ${scrambleRequest.scrambles.size}"
            .takeIf { scrambleRequest.scrambles.size > 1 } ?: ""

        val title = "$globalTitle - ${scrambleRequest.title}$scrambleSuffix"

        val table = Table(floatArrayOf(3.75f, 1f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setFixedLayout() // FIXME
            .setVerticalBorderSpacing(SPACE_SCRAMBLE_IMAGE)
            .setHorizontalBorderSpacing(SCRAMBLE_IMAGE_PADDING)
            .setBorder(Border.NO_BORDER)

        val titleParagraph = Paragraph(title)
            .setFont(BASE_FONT)
            .setFontSize(FONT_SIZE)

        val titleCell = Cell().add(titleParagraph)
            .setBorder(Border.NO_BORDER)

        val scrambleImg = Image(SvgConverter.convertToXObject(svg.toString(), document))
            .setAutoScale(true)

        val scrambleImgCell = Cell(2, 1).add(scrambleImg)
            .setBorder(Border.NO_BORDER)
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)

        val scrambleParagraph = Paragraph(scramble)
            .setFont(BASE_FONT)
            .setFontSize(FONT_SIZE)

        val scrambleStrCell = Cell().add(scrambleParagraph)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setBorder(Border.NO_BORDER)

        table.addCell(titleCell)
        table.addCell(scrambleImgCell)
        table.addCell(scrambleStrCell)

        val dashedLineSep = LineSeparator(DashedLine(2f)) // FIXME, posted on SO
        val doc = Document(document)

        doc.add(dashedLineSep)

        for (i in 0 until SCRAMBLES_PER_SHEET) {
            doc.add(table)
            doc.add(dashedLineSep)
        }

        doc.close()
    }

    companion object {
        val BASE_FONT = FontUtil.getFontForLocale(Translate.DEFAULT_LOCALE)

        val SPACE_SCRAMBLE_IMAGE = 5f // scramble image won't touch the scramble
        val SCRAMBLE_IMAGE_PADDING = 8f // scramble image won't touch the dashed lines

        val FONT_SIZE = 12f

        val SCRAMBLES_PER_SHEET = 8
    }
}
