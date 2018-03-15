package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.simpleTableColumn
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.wrappers.table.ReactTable
import react.*
import react.dom.div

class ReferenceBookItemComponent : RComponent<ReferenceBookItemComponent.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "subtable-wrapper") {
            ReactTable {
                attrs {
                    columns = arrayOf(
                        simpleTableColumn("id", "Id"),
                        simpleTableColumn("value", "Value")
                    )
                    data = props.data
                    showPagination = false
                    minRows = 2
                    sortable = false
                    showPageJump = false
                    resizable = false
                    collapseOnDataChange = false
                    SubComponent = rFunction("Reference Book Item") {props ->
                        child(ReferenceBookItemComponent::class) {
                            attrs {
                                data = (props.original as ReferenceBookItem).children.toTypedArray()
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var data: Array<out ReferenceBookItem>
    }
}