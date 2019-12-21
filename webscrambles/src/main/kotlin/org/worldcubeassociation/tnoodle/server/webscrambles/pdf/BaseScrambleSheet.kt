package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.*
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import java.io.ByteArrayOutputStream

abstract class BaseScrambleSheet(val scrambleRequest: ScrambleRequest, globalTitle: String?) : BasePdfSheet(globalTitle) {
    override fun openDocument(writer: PdfWriter): PdfDocument {
        return super.openDocument(writer).apply {
            defaultPageSize = PAGE_SIZE
            // FIXME 0, 0, 75, 75 margins!

            catalog.put(PdfName.CreationDate, PdfDate().pdfObject)
            catalog.put(PdfName.Producer, PdfString("TNoodle")) // FIXME const

            title?.let {
                catalog.put(PdfName.Title, PdfString(it))
            }
        }
    }

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
