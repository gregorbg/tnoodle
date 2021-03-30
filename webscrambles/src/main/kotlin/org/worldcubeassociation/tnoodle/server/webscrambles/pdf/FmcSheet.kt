package org.worldcubeassociation.tnoodle.server.webscrambles.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.property.AreaBreakType
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util.FontUtil
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.*
import org.worldcubeassociation.tnoodle.server.webscrambles.wcif.model.extension.FmcAttemptCountExtension
import java.util.Locale

abstract class FmcSheet(scrambleSet: ScrambleSet, activityCode: ActivityCode, val competitionTitle: String, val locale: Locale, val hasGroupID: Boolean) : BaseScrambleSheet(scrambleSet, activityCode) {
    val expectedAttemptNum: Int
        get() = scrambleSet.findExtension<FmcAttemptCountExtension>()
            ?.totalAttempts ?: scrambleSet.scrambles.size

    val localFont = FontUtil.getFontForLocale(locale)

    override fun PdfDocument.writeContents() {
        val doc = Document(this)

        for (i in scrambleSet.scrambles.indices) {
            doc.addFmcSheet(i)

            if (i < scrambleSet.scrambles.lastIndex) {
                doc.add(AreaBreak(AreaBreakType.NEXT_PAGE))
            }
        }
    }

    protected abstract fun Document.addFmcSheet(index: Int)
}
