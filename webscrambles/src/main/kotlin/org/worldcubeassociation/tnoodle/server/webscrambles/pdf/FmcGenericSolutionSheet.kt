package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import org.worldcubeassociation.tnoodle.server.webscrambles.ScrambleRequest
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import java.util.*

class FmcGenericSolutionSheet(request: ScrambleRequest, globalTitle: String?, locale: Locale) : FmcSolutionSheet(request, globalTitle, locale) {
    override fun PdfDocument.writeContents() {
        val bf = FontUtil.getFontForLocale(locale)

        this.addNewPage()
            .addFmcSolutionSheet(scrambleRequest, title, -1, locale, bf)
    }
}
