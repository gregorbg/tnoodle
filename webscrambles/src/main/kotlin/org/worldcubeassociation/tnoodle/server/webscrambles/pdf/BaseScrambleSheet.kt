package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.*
import org.worldcubeassociation.tnoodle.server.webscrambles.Translate
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ActivityCode
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.ScrambleSet
import java.io.ByteArrayOutputStream

abstract class BaseScrambleSheet(val scrambleSet: ScrambleSet, val activityCode: ActivityCode) : BasePdfSheet() {
    override fun openDocument(writer: PdfWriter): PdfDocument {
        return super.openDocument(writer).apply {
            defaultPageSize = PAGE_SIZE
            // FIXME 0, 0, 75, 75 margins!

            catalog.put(PdfName.CreationDate, PdfDate().pdfObject)
            catalog.put(PdfName.Producer, PdfString("TNoodle")) // FIXME const

            activityCode.compileTitleString(Translate.DEFAULT_LOCALE).let {
                catalog.put(PdfName.Title, PdfString(it))
            }
        }
    }

    protected val scramblingPuzzle = activityCode.eventModel?.scrambler?.scrambler
        ?: error("Cannot draw PDF: Scrambler model for $activityCode not found")

    // FIXME this was used while opening the writer
    //  setBoxSize("art", Rectangle(36f, 54f, PAGE_SIZE.width - 36, PAGE_SIZE.height - 54))

    // FIXME is this really neccessary? Looks like effectively just creating a copy…
    override fun finalise(processedBytes: ByteArrayOutputStream, password: String?): ByteArray {
        val props = ReaderProperties().apply {
            password?.let { setPassword(it.toByteArray()) }
        }

        val pdfReader = PdfReader(processedBytes.toByteArray().inputStream(), props)

        val buffer = ByteArray(pdfReader.fileLength.toInt())
        pdfReader.safeFile.readFully(buffer)

        return buffer
    }
}
