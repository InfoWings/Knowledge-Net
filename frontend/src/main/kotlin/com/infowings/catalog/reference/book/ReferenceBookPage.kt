package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.getAllAspects
import com.infowings.catalog.aspects.simpleTableColumn
import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.h1

class ReferenceBookPage : RComponent<RouteSuppliedProps, ReferenceBookPage.State>() {

    override fun State.init() {
        data = emptyArray()
    }

    override fun componentDidMount() {
        launch {
            val aspects = getAllAspects()
            setState {
                data = aspects.aspects.toTypedArray()
            }
        }
    }

    override fun RBuilder.render() {

        child(Header::class) {
            attrs { location = props.location.pathname }
        }

        h1 { +"Reference book page" }

        ReactTable {
            attrs {
                columns = arrayOf(
                        RTableColumnDescriptor {
                            Header = rFunction("AggregateColumnAspect") { +"Aspect" }
                            columns = arrayOf(
                                    simpleTableColumn("name", "Name"),
                                    simpleTableColumn("measure", "Measure Unit"),
                                    simpleTableColumn("domain", "Domain"),
                                    simpleTableColumn("baseType", "Base Type"))
                        })
                data = state.data
                showPagination = false
                minRows = 1
                sortable = false
                showPageJump = false
                resizable = false
                collapseOnDataChange = false
                SubComponent = rFunction("PropertySubComponent") {
                    child(ReferenceBookComponent::class) {
                        attrs {

                        }
                    }
                }
            }
        }
    }

    interface State : RState {
        var data: Array<out Any>
    }
}