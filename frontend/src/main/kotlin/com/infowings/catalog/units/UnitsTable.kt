package com.infowings.catalog.units

import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import com.infowings.catalog.wrappers.table.treeTable
import react.*
import react.dom.span

private fun header(columnName: String) = rFunction<RTableRendererProps>("UnitsTableHeader") {
    span {
        +columnName
    }
}

private fun column(accessor: String, header: RClass<RTableRendererProps>) =
    RTableColumnDescriptor {
        this.accessor = accessor
        this.Header = header
    }

data class UnitsTableRowData(val measure: String, val name: String, val symbol: String)

class UnitsTableProperties(var data: Array<UnitsTableRowData>) : RProps

class UnitsTable : RComponent<UnitsTableProperties, RState>() {

    override fun RBuilder.render() {
        treeTable(ReactTable)({
            attrs {
                pivotBy = arrayOf("measure")
                columns = arrayOf(
                    column("measure", header("Measure")),
                    column("name", header("Unit")),
                    column("symbol", header("Symbol"))
                )
                data = props.data
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
            }
        })
    }
}