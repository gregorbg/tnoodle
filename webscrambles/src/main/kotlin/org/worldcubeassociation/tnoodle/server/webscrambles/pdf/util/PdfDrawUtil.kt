package org.worldcubeassociation.tnoodle.server.webscrambles.pdf.util

import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.ColumnDocumentRenderer
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.layout.LayoutArea
import com.itextpdf.layout.layout.LayoutContext
import com.itextpdf.layout.layout.LayoutResult
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.svg.converter.SvgConverter
import net.gnehzr.tnoodle.svglite.Svg

object PdfDrawUtil {
    fun PdfCanvas.renderSvgToPDF(svg: Svg, x: Float, y: Float, padding: Int = 0) {
        SvgConverter.drawOnCanvas(svg.toString(), this, padding + x, padding + y)
    }

    fun PdfCanvas.drawDashedLine(left: Int, right: Int, yPosition: Int) {
        setLineDash(3f, 3f)
        moveTo(left.toDouble(), yPosition.toDouble())
        lineTo(right.toDouble(), yPosition.toDouble())
        stroke()
    }

    fun Document.fitAndShowText(text: String, rect: Rectangle, pageNum: Int, bf: PdfFont, maxFontSize: Float, align: TextAlignment, leadingMultiplier: Float): Int {
        // We create a temp pdf and check if the text fit in a rectangle there.
        val status = showTextStatus(text, rect, pageNum, bf, maxFontSize, align, leadingMultiplier)

        if (status != LayoutResult.FULL) {
            val iterMaxFontSize = maxFontSize - 0.1f
            // FIXME brute-force approach doesn't seem healthy
            return fitAndShowText(text, rect, pageNum, bf, iterMaxFontSize, align, leadingMultiplier)
        }

        return showTextStatus(text, rect, pageNum, bf, maxFontSize, align, leadingMultiplier, true)
    }

    private fun Document.showTextStatus(text: String, rect: Rectangle, pageNum: Int, bf: PdfFont, fontSize: Float, align: TextAlignment, leadingMultiplier: Float, render: Boolean = false): Int {
        val par = Paragraph(text)
            .setFont(bf)
            .setFontSize(fontSize)
            .setTextAlignment(align)
            .setMultipliedLeading(leadingMultiplier) // FIXME singleLeading instead?

        val colRender = ColumnDocumentRenderer(this, arrayOf(rect))

        // If it's ok, we add the text to original pdf.
        val elementRenderer = par.createRendererSubTree()
        elementRenderer.parent = colRender

        val layoutArea = LayoutArea(pageNum, rect)
        val layoutResult = elementRenderer.layout(LayoutContext(layoutArea))

        if (render) {
            add(par)
        }

        return layoutResult.status
    }

    fun Document.populateRect(rect: Rectangle, pageNum: Int, itemsWithAlignment: List<Pair<String, TextAlignment>>, bf: PdfFont, fontSize: Int) {
        val totalHeight = rect.height
        val width = rect.width

        val x = rect.left
        val y = rect.top

        val height = totalHeight / itemsWithAlignment.size

        for ((i, content) in itemsWithAlignment.withIndex()) {
            val temp = Rectangle(x, y + height * i - totalHeight - fontSize.toFloat(), x + width, y + height * i - totalHeight)
            fitAndShowText(content.first, temp, pageNum, bf, 15f, content.second, 1f)
        }
    }
}
