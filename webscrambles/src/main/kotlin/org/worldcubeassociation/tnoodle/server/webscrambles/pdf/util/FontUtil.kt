package org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util

import com.itextpdf.io.font.FontProgramFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.counter.EventCounter
import com.itextpdf.kernel.counter.EventCounterHandler
import com.itextpdf.kernel.counter.IEventCounterFactory
import com.itextpdf.kernel.counter.SimpleEventCounterFactory
import com.itextpdf.kernel.counter.event.IEvent
import com.itextpdf.kernel.counter.event.IMetaInfo
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import java.util.*

object FontUtil {
    val CJK_FONT: PdfFont get() = PdfFontFactory.createFont(CJK_FONT_PROGRAM, PdfEncodings.IDENTITY_H, true)
    val MONO_FONT: PdfFont get() = PdfFontFactory.createFont(MONO_FONT_PROGRAM, PdfEncodings.IDENTITY_H, true)
    val NOTO_SANS_FONT: PdfFont get() = PdfFontFactory.createFont(NOTO_SANS_FONT_PROGRAM, PdfEncodings.IDENTITY_H, true)

    val CJK_FONT_PROGRAM = FontProgramFactory.createFont("fonts/wqy-microhei.ttf", true)
    val MONO_FONT_PROGRAM = FontProgramFactory.createFont("fonts/LiberationMono-Regular.ttf", true)
    val NOTO_SANS_FONT_PROGRAM = FontProgramFactory.createFont("fonts/NotoSans-Regular.ttf", true)

    const val MAX_SCRAMBLE_FONT_SIZE = 20f
    const val MINIMUM_ONE_LINE_FONT_SIZE = 15f

    private val CJK_LANGUAGES = listOf(
        Locale.forLanguageTag("zh-CN"),
        Locale.forLanguageTag("zh-TW"),
        Locale.forLanguageTag("ko"),
        Locale.forLanguageTag("ja")
    )

    init {
        // Email agpl@itextpdf.com if you want to know what this is about =)
        val counterFactory: IEventCounterFactory = SimpleEventCounterFactory(NoopCounter)
        EventCounterHandler.getInstance().register(counterFactory)
    }

    fun getFontForLocale(locale: Locale) = if (locale in CJK_LANGUAGES) CJK_FONT else NOTO_SANS_FONT

    internal object NoopCounter : EventCounter() {
        override fun onEvent(event: IEvent?, metaInfo: IMetaInfo?) {
            // noop.
        }
    }
}
