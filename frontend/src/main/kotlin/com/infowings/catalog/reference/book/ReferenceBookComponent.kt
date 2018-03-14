package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.simpleTableColumn
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.wrappers.table.ReactTable
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div

class ReferenceBookComponent : RComponent<ReferenceBookComponent.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "subtable-wrapper") {
            ReactTable {
                attrs {
                    columns = arrayOf(simpleTableColumn("name", "Reference book"))
                    data = arrayOf()
                    showPagination = false
                    minRows = 2
                    sortable = false
                    showPageJump = false
                    resizable = false
                    collapseOnDataChange = false
                }
            }
        }
    }

    interface Props : RProps {
        val referenceBookItem: ReferenceBookItem
    }
}