package com.infowings.catalog.units

import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import com.infowings.catalog.wrappers.table.treeTable
import react.*
import react.dom.span

fun headerComponent(columnName: String) = rFunction<RTableRendererProps>("UnitHeader") {
    span {
        +columnName
    }
}

fun unitColumn(accessor: String, header: RClass<RTableRendererProps>) =
    RTableColumnDescriptor {
        this.accessor = accessor
        this.Header = header
    }

data class RowData(val measure: String, val name: String, val symbol: String)

class UnitsProps(var data: Array<RowData>) : RProps

class UnitsTable : RComponent<UnitsProps, RState>() {

    override fun RBuilder.render() {
        treeTable(ReactTable)({
            attrs {
                pivotBy = arrayOf("measure")
                columns = arrayOf(
                    unitColumn("measure", headerComponent("Measure")),
                    unitColumn("name", headerComponent("Unit")),
                    unitColumn("symbol", headerComponent("Symbol"))
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