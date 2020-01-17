package org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util

import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.layout.LayoutContext
import com.itextpdf.layout.layout.LayoutResult
import com.itextpdf.layout.property.Property
import com.itextpdf.layout.property.UnitValue
import com.itextpdf.layout.renderer.ParagraphRenderer

class FontSizeRenderer(val content: Paragraph) : ParagraphRenderer(content) {
    override fun getNextRenderer() = FontSizeRenderer(content)

    override fun layout(layoutContext: LayoutContext?): LayoutResult {
        val currentFontSize = content.getProperty<UnitValue>(Property.FONT_SIZE).value

        return layoutBinarySearch(layoutContext, 1f, currentFontSize, 20)
    }

    private tailrec fun layoutBinarySearch(layoutContext: LayoutContext?, minFontSize: Float, maxFontSize: Float, iterationThreshold: Int): LayoutResult {
        val currentLayout = super.layout(layoutContext)

        if (iterationThreshold <= 0) {
            return currentLayout
        }

        val currentFontSize = content.getProperty<UnitValue>(Property.FONT_SIZE).value

        return if (currentLayout.status == LayoutResult.FULL) {
            val increment = (currentFontSize + maxFontSize) / 2
            content.setFontSize(increment)

            layoutBinarySearch(layoutContext, currentFontSize, maxFontSize, iterationThreshold - 1)
        } else {
            val decrement = (minFontSize + currentFontSize) / 2
            content.setFontSize(decrement)

            layoutBinarySearch(layoutContext, minFontSize, currentFontSize, iterationThreshold - 1)
        }
    }
}
