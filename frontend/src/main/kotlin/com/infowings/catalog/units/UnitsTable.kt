package com.infowings.catalog.units

import com.infowings.catalog.common.column
import com.infowings.catalog.common.header
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.ReactTable
import com.infowings.catalog.wrappers.table.treeTable
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState


data class UnitsTableRowData(val measure: String, val name: String, val symbol: String)

class UnitsTableProperties(var data: Array<UnitsTableRowData>) : RProps

class UnitsTable : RComponent<UnitsTableProperties, RState>() {

    override fun RBuilder.render() {
        treeTable(ReactTable)({
            attrs {
                pivotBy = arrayOf("measure")
                columns = arrayOf(
                    column("measure", header("Measure")),
                    RTableColumnDescriptor {
                        this.accessor = "name"
                        this.Header = header("Unit")
                        this.width = 300.0
                    },
                    column("symbol", header("Symbol"))
                )
                data = props.data
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
                resizable = false
            }
        })
    }
}