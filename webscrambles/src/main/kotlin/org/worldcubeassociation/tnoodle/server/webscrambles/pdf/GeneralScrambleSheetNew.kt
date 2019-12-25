package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.svg.converter.SvgConverter
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import java.io.File

class GeneralScrambleSheetNew(scrambleRequest: ScrambleRequest, globalTitle: String?) : BaseScrambleSheet(scrambleRequest, globalTitle) {
    override fun PdfDocument.writeContents() = addNewPage().writeScrambleContents()

    fun PdfPage.writeScrambleContents() {
        val foo = Document(document)

        foo.addScrambleTable(scrambleRequest.scrambles, scrambleRequest)

        if (scrambleRequest.extraScrambles.isNotEmpty()) {
            foo.addScrambleTable(scrambleRequest.extraScrambles, scrambleRequest, EXTRA_SCRAMBLE_PREFIX, TABLE_HEADING_EXTRA_SCRAMBLES)
        }
    }

    fun Document.addScrambleTable(scrambles: List<String>, scrambleRequest: ScrambleRequest, scrambleNumberPrefix: String = STD_SCRAMBLE_PREFIX, specialHeading: String? = null) {
        val table = Table(3)
            .useAllAvailableWidth()
            .setAutoLayout()

        if (specialHeading != null) {
            val headingCell = Cell(1, 3)
                .add(Paragraph(specialHeading))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            table.addCell(headingCell)
        }

        val puzzle = scrambleRequest.scrambler
        val colorScheme = scrambleRequest.colorScheme

        val scrambleFont = FontUtil.MONO_FONT

        for ((i, scramble) in scrambles.withIndex()) {
            val indexLabel = Text("$scrambleNumberPrefix${i + 1}.")

            val nthScramble = Cell()
                .add(Paragraph(indexLabel))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            table.addCell(nthScramble)

            val scramblePhrase = Paragraph(scramble)
                .setMultipliedLeading(1.1f) // FIXME const
            // TODO highlighting, cell renderer

            val scrambleCell = Cell().add(scramblePhrase)
                .setKeepTogether(true)
                .setFont(scrambleFont)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)

            table.addCell(scrambleCell)

            val svg = puzzle.drawScramble(scramble, colorScheme)
            val img = Image(SvgConverter.convertToXObject(svg.toString(), pdfDocument))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setAutoScale(true)

            table.addCell(img)
        }

        this.add(table)
    }

    companion object {
        const val STD_SCRAMBLE_PREFIX = ""
        const val EXTRA_SCRAMBLE_PREFIX = "E"

        const val TABLE_HEADING_EXTRA_SCRAMBLES = "Extra Scrambles"

        @JvmStatic
        fun main(args: Array<String>) {
            println("Parsing request")

            val reqMap = mapOf("fooBar" to "sq1fast*5")
            val scrRequest = ScrambleRequest.parseScrambleRequests(reqMap, "trololol").single()
            //.copy(extraScrambles = listOf("R U R' U' R' F R2 U' R' U' R U R' F'"))

            println("Generating sheet")

            val sheet = GeneralScrambleSheetNew(scrRequest, "FMC TNoodle 2019")

            println("Rendering…")
            val sheetBytes = sheet.render()

            println("Dumping to file")
            File("/home/suushie_maniac/jvdocs/tnoodle/pdf_debug.pdf").writeBytes(sheetBytes)

            System.exit(123)
        }
    }
}
