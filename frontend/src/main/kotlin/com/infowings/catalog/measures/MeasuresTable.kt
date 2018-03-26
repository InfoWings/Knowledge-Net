package com.infowings.catalog.measures

import com.infowings.catalog.wrappers.table.*
import react.*
import react.dom.span
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

data class UnitsTableRowData(
    val pivotBy: String,
    val name: String,
    val symbol: String,
    val containsFilterText: Boolean
)

class UnitsTableProperties(var data: Array<UnitsTableRowData>) : RProps

class UnitsTable : RComponent<UnitsTableProperties, RState>() {

    override fun RBuilder.render() {
        treeTable(ReactTable)({
            attrs {
                pivotBy = arrayOf("pivotBy")
                columns = arrayOf(
                    column("pivotBy", header("pivotBy")),
                    column("name", header("Unit"), 300.0),
                    column("symbol", header("Symbol"))
                )
                data = props.data
                showPagination = false
                pageSize = props.data.map { it.pivotBy }.count()
                minRows = 0
                sortable = false
                showPageJump = false
                resizable = false
                getTdProps = ::tdProps
            }
        })
    }
}

private fun tdProps(state: dynamic, rowInfo: RowInfo?, column: dynamic): dynamic {
    if (rowInfo != null && !rowInfo.aggregated) {
        val opacity = if (rowInfo.original.containsFilterText) "1" else "0.4"
        return json("style" to json("opacity" to opacity))
    }
    return json("style" to json("background" to "#E6FDFF"))
}