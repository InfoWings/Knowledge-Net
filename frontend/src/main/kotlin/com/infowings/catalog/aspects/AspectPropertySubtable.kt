package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.i
import react.dom.input

/**
 * Compact column creator for immutable columns (aspect fields of the aspect property)
 */
private fun propertyColumn(accessor: String, headerName: String) = RTableColumnDescriptor {
    this.accessor = accessor
    this.Header = rFunction("PropertyHeader") { +headerName }
}

/**
 * Compact column creator for mutable columns of aspect property (name, power)
 */
private fun mutablePropertyColumn(accessor: String, headerName: String, onPropertyValueChanged: (index: Int, value: String) -> Unit) = RTableColumnDescriptor {
    this.accessor = accessor
    this.Header = rFunction("Mutable Property Header") { +headerName }
    this.Cell = cellComponent(onPropertyValueChanged)
}

/**
 * Creator of the mutable cells (name and power fields of AspectPropertyData)
 */
private fun cellComponent(onPropertyChanged: (index: Int, value: String) -> Unit) = rFunction<RTableRendererProps>("LoggingCell") { rTableRendererProps ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = rTableRendererProps.value?.toString() ?: ""
            onChangeFunction = { onPropertyChanged(rTableRendererProps.index, it.asDynamic().target.value) }
        }
    }
}

data class AspectPropertyRow(
        /**
         * Aspect Property displayed on the row
         */
        val property: AspectPropertyData,
        /**
         * Child aspect associated with aspect property (can be null when editing)
         */
        val aspect: AspectData?
)

class AspectPropertySubtable : RComponent<AspectPropertySubtable.Props, RState>() {

    private fun propertyNameChanger(changedProperty: AspectPropertyData, name: String) = AspectPropertyData(changedProperty.id, name, changedProperty.aspectId, changedProperty.power, changedProperty.version)

    private fun propertyPowerChanger(changedProperty: AspectPropertyData, power: String) = AspectPropertyData(changedProperty.id, changedProperty.name, changedProperty.aspectId, power, changedProperty.version)

    private fun onInputValueChanged(propertyChanger: (changedProperty: AspectPropertyData, value: String) -> AspectPropertyData) = { changedIndex: Int, value: String ->
        props.onPropertyChanged { aspect: AspectData ->
            aspect.copy(properties = aspect.properties.mapIndexed { index, property ->
                if (index == changedIndex) {
                    propertyChanger(property, value)
                } else {
                    property
                }
            })
        }
    }

    /**
     * Callback to call when new property is created
     */
    private fun onNewPropertyCreated() {
        props.onPropertyChanged { aspect: AspectData ->
            aspect.copy(properties = aspect.properties + AspectPropertyData("", "", "", ""))
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
                attrs.onClickFunction = { onNewPropertyCreated() }
            }
        }
    }

    interface Props : RProps {
        /**
         * Array of children aspect properties
         */
        var data: Array<AspectPropertyRow>
        /**
         * Callback to call when property is changed or new property created (callback just marks aspect data as edited)
         */
        var onPropertyChanged: (propertyChanger: (aspect: AspectData) -> AspectData) -> Unit
    }
}