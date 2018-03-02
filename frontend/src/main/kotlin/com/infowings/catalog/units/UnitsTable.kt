package com.infowings.catalog.units

import com.infowings.catalog.common.MeasureGroupMap
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

data class UnitsTableRowData(
    val measureGroupName: String,
    val name: String,
    val symbol: String,
    val containsFilterText: Boolean
)

class UnitsTableProperties(var data: Array<UnitsTableRowData>) : RProps

class UnitsTable : RComponent<UnitsTableProperties, RState>() {

    override fun RBuilder.render() {
        treeTable(ReactTable)({
            attrs {
                pivotBy = arrayOf("measureGroupName")
                columns = arrayOf(
                    column("measureGroupName", header("Measure")),
                    column("name", header("Unit"), 300.0),
                    column("symbol", header("Symbol"))
                )
                data = props.data
                showPagination = false
                pageSize = props.data.map { it.measureGroupName }.count()
                minRows = 0
                sortable = false
                showPageJump = false
                resizable = false
                getTdProps = ::tdProps
                defaultExpanded = defineExpandedRows(props.data)
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

private fun defineExpandedRows(data: Array<UnitsTableRowData>): Json {
    val rowsGroupCount = data.map { it.measureGroupName }.distinct().count()

    if (rowsGroupCount == MeasureGroupMap.count()) return json()

    val jsonList = (0 until rowsGroupCount).map { json(it.toString() to true) }

    val resultJson = json()
    for (json in jsonList) {
        resultJson.add(json)
    }
    return resultJson
}