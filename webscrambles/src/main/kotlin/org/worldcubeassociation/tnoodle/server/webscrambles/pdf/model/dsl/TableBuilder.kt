package org.worldcubeassociation.tnoodle.server.webscrambles.pdf.model.dsl

import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.model.Row
import org.worldcubeassociation.tnoodle.server.webscrambles.pdf.model.Table

class TableBuilder(val numColumns: Int, parent: ElementBuilder?) : PropertiesElementBuilder(parent) {
    private val rows = mutableListOf<Row>()

    // TODO make sure this is always the same as numColumns?
    var relativeWidths = Table.unitWidths(numColumns)

    fun row(columns: Int = numColumns, fn: RowBuilder.() -> Unit) {
        val colSpanCoefficient = numColumns / columns
        val row = RowBuilder(colSpanCoefficient, this).apply(fn).compile()
        rows.add(row)
    }

    fun compile(): Table {
        return Table(relativeWidths, rows)
    }
}
