package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.simpleTableColumn
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div

class ReferenceBookComponent : RComponent<ReferenceBookComponent.Props, ReferenceBookComponent.State>() {

    override fun State.init() {
        data = emptyArray()
    }

    override fun componentDidMount() {
        launch {
            val relatedReferenceBooks = getAll()
                .filter { it.aspectId == props.aspectId }
                .toTypedArray()

            setState {
                data = relatedReferenceBooks
            }
        }
    }

    override fun RBuilder.render() {
        div(classes = "subtable-wrapper") {
            ReactTable {
                attrs {
                    columns = arrayOf(simpleTableColumn("name", "Reference book"))
                    data = state.data
                    showPagination = false
                    minRows = 2
                    sortable = false
                    showPageJump = false
                    resizable = false
                    collapseOnDataChange = false
                    SubComponent = rFunction("Reference Book Items") {props ->
                        child(ReferenceBookItemComponent::class) {
                            attrs {
                                data = (props.original as ReferenceBook).children.toTypedArray()
                            }
                        }
                    }
                }
            }
        }
    }

    interface State : RState {
        var data: Array<out ReferenceBook>
    }

    interface Props : RProps {
        var aspectId: String?
    }
}