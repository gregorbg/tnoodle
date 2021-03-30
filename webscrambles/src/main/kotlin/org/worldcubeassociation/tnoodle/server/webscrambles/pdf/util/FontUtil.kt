package org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util

import com.itextpdf.io.font.FontProgramFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import java.util.*

object FontUtil {
    val CJK_FONT: PdfFont get() = PdfFontFactory.createFont(CJK_FONT_PROGRAM, PdfEncodings.IDENTITY_H, true)
    val MONO_FONT: PdfFont get() = PdfFontFactory.createFont(MONO_FONT_PROGRAM, PdfEncodings.IDENTITY_H, true)
    val NOTO_SANS_FONT: PdfFont get() = PdfFontFactory.createFont(NOTO_SANS_FONT_PROGRAM, PdfEncodings.IDENTITY_H, true)

    private val CJK_FONT_PROGRAM = FontProgramFactory.createFont("fonts/wqy-microhei.ttf", true)
    private val MONO_FONT_PROGRAM = FontProgramFactory.createFont("fonts/LiberationMono-Regular.ttf", true)
    private val NOTO_SANS_FONT_PROGRAM = FontProgramFactory.createFont("fonts/NotoSans-Regular.ttf", true)

    private val CJK_LANGUAGES = listOf(
        Locale.forLanguageTag("zh-CN"),
        Locale.forLanguageTag("zh-TW"),
        Locale.forLanguageTag("ko"),
        Locale.forLanguageTag("ja")
    )

    fun getFontForLocale(locale: Locale) = if (locale in CJK_LANGUAGES) CJK_FONT else NOTO_SANS_FONT
}
