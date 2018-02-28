package com.infowings.catalog.units

import com.infowings.catalog.wrappers.table.*
import react.*
import react.dom.span
import kotlin.js.Json
import kotlin.js.json

private fun header(columnName: String) = rFunction<RTableRendererProps>("UnitsTableHeader") {
    span {
        +columnName
    }
}

private fun column(accessor: String, header: RClass<RTableRendererProps>, width: Double? = null) =
    RTableColumnDescriptor {
        this.accessor = accessor
        this.Header = header
        if (width != null) {
            this.width = width
        }
    }

data class UnitsTableRowData(val measure: String, val name: String, val symbol: String, val containsFilterText: Boolean)

class UnitsTableProperties(var data: Array<UnitsTableRowData>, var defaultExpandedRows: Json) : RProps

class UnitsTable : RComponent<UnitsTableProperties, RState>() {

    override fun RBuilder.render() {
        treeTable(ReactTable)({
            attrs {
                pivotBy = arrayOf("measure")
                columns = arrayOf(
                    column("measure", header("Measure")),
                    column("name", header("Unit"), 300.0),
                    column("symbol", header("Symbol"))
                )
                data = props.data
                showPagination = false
                minRows = 0
                sortable = false
                showPageJump = false
                resizable = false
                getTdProps = ::tdProps
                defaultExpanded = props.defaultExpandedRows
            }
        })
    }
}

private fun tdProps(state: dynamic, rowInfo: RowInfo?, column: dynamic): dynamic {
    if (rowInfo != null && !rowInfo.aggregated) {
        val color = if (rowInfo.original.containsFilterText) "white" else "lightgray"
        return json("style" to json("background" to color))
    }
    return json("style" to json("background" to "#E6FDFF"))
}