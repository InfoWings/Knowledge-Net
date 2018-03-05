package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.getAllAspects
import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.h1

internal fun referenceBookColumn(accessor: String, header: String, cell: RClass<RTableRendererProps>? = null) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = rFunction("AspectHeader") { +header }
            cell?.let {
                this.Cell = cell
            }
        }

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
                            columns = arrayOf(referenceBookColumn("name", "Name"),
                                    referenceBookColumn("measure", "Measure Unit"),
                                    referenceBookColumn("domain", "Domain"),
                                    referenceBookColumn("baseType", "Base Type"))
                        })
                data = state.data
                showPagination = false
                minRows = 1
                sortable = false
                showPageJump = false
                resizable = false
                collapseOnDataChange = false
                SubComponent = rFunction("PropertySubComponent") { child(ReferenceBookComponent::class) {} }
            }
        }
    }

    interface State : RState {
        var data: Array<out Any>
    }
}

//ReactTable {
//    attrs {
//        columns = arrayOf(
//                aspectColumn("aspect.name", headerComponent("Name"), cellComponent(fieldChangedHandler(AspectData::withName))),
//                aspectColumn("aspect.measure", headerComponent("Measure Unit"), cellComponent(fieldChangedHandler(AspectData::withMeasure))),
//                aspectColumn("aspect.domain", headerComponent("Domain"), cellComponent(fieldChangedHandler(AspectData::withDomain))),
//                aspectColumn("aspect.baseType", headerComponent("Base Type"), cellComponent(fieldChangedHandler(AspectData::withBaseType))),
//                controlsColumn(
//                        if (state.newAspect == null)
//                            addNewAspectHeaderEnabled(::startCreatingNewAspect)
//                        else addNewAspectHeaderDisabled,
//                        ::saveAspect,
//                        ::resetAspect
//                )
//        )
//        data = aspectsToRows()
//        loading = props.loading
//        SubComponent = propertySubComponent(props.aspectsMap, ::onAspectPropertyChanged)
//        showPagination = false
//        minRows = 2
//        sortable = false
//        showPageJump = false
//        resizable = false
//        collapseOnDataChange = false
//    }
//}