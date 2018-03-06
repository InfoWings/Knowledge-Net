package com.infowings.catalog.subjects

import com.infowings.catalog.common.column
import com.infowings.catalog.common.header
import com.infowings.catalog.wrappers.table.RTableProps
import com.infowings.catalog.wrappers.table.ReactTable
import react.RBuilder
import react.RComponent

data class SubjectViewData(val name: String, val aspectNames: List<String>)

class SubjectsTable : RComponent<RTableProps, SubjectApiMiddleware.State>() {
    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                    column("name", header("name")),
                    column("aspectNames", header("aspectNames"))
                )
                console.log("state.data: ", state.data)
                data = props.data
            }
        }
    }

}