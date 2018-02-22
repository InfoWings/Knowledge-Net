package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.i
import react.dom.input
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable

private fun propertyColumn(accessor: String, headerName: String) = RTableColumnDescriptor {
    this.accessor = accessor
    this.Header = rFunction("PropertyHeader") { +headerName }
}

private fun mutablePropertyColumn(accessor: String, headerName: String, onPropertyValueChanged: (index: Int, value: String) -> Unit) = RTableColumnDescriptor {
    this.accessor = accessor
    this.Header = rFunction("Mutable Property Header") { +headerName }
    this.Cell = cellComponent(onPropertyValueChanged)
}

private fun cellComponent(onPropertyChanged: (index: Int, value: String) -> Unit) = rFunction<RTableRendererProps>("LoggingCell") { rTableRendererProps ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = rTableRendererProps.value?.toString() ?: ""
            onChangeFunction = { e ->
                onPropertyChanged(rTableRendererProps.index, e.asDynamic().target.value)
            }
        }
    }
}

data class AspectPropertyRow(val property: AspectPropertyData, val aspect: AspectData)

class AspectPropertySubtable : RComponent<AspectPropertySubtable.Props, RState>() {

    private fun propertyNameChanger(changedProperty: AspectPropertyData, name: String) = AspectPropertyData(changedProperty.id, name, changedProperty.aspectId, changedProperty.power)

    private fun propertyPowerChanger(changedProperty: AspectPropertyData, power: String) = AspectPropertyData(changedProperty.id, changedProperty.name, changedProperty.aspectId, power)

    private fun onInputValueChanged(propertyChanger: (changedProperty: AspectPropertyData, value: String) -> AspectPropertyData) = { changedIndex: Int, value: String ->
        props.onPropertyChanged { aspect: AspectData ->
            AspectData(aspect.id, aspect.name, aspect.measure, aspect.domain, aspect.baseType, aspect.properties?.mapIndexed { index, property ->
                if (index == changedIndex) {
                    propertyChanger(property, value)
                } else {
                    property
                }
            }?.toTypedArray())
        }
    }

    private fun onNewPropertyCreated() {
        props.onPropertyChanged { aspect: AspectData ->
            AspectData(aspect.id, aspect.name, aspect.measure, aspect.domain, aspect.baseType, aspect.properties!! + AspectPropertyData("", "", "", ""))
        }
    }

    override fun RBuilder.render() {
        div(classes = "subtable-wrapper") {
            ReactTable {
                attrs {
                    columns = arrayOf(
                            RTableColumnDescriptor {
                                Header = rFunction("AggregateColumnProperty") { +"Property" }
                                columns = arrayOf(
                                        mutablePropertyColumn("property.name", "Name", onInputValueChanged(::propertyNameChanger)),
                                        mutablePropertyColumn("property.power", "Power", onInputValueChanged(::propertyPowerChanger))
                                )
                            },
                            RTableColumnDescriptor {
                                Header = rFunction("AggregateColumnAspect") { +"Aspect" }
                                columns = arrayOf(
                                        propertyColumn("aspect.name", "Name"),
                                        propertyColumn("aspect.measure", "Measure Unit"),
                                        propertyColumn("aspect.domain", "Domain"),
                                        propertyColumn("aspect.baseType", "Base Type")
                                )
                            }
                    )
                    data = props.data
                    showPagination = false
                    minRows = 2
                    sortable = false
                    showPageJump = false
                    resizable = false
                }
            }
            div(classes = "new-property-button") {
                i(classes = "fas fa-plus") {}
                attrs {
                    onClickFunction = { onNewPropertyCreated() }
                }
            }
        }
    }

    interface Props : RProps {
        var data: Array<AspectPropertyRow>
        var onPropertyChanged: (propertyChanger: (aspect: AspectData) -> AspectData) -> Unit
    }
}