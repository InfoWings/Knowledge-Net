package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.components.SuggestingInput
import com.infowings.catalog.components.aspectOption
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.i
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import com.infowings.catalog.wrappers.table.SubComponentProps

private fun selectColumn(accessor: String, headerName: String, onPropertyValueChanged: (index: Int, value: AspectData) -> Unit, aspectOptions: Array<AspectData>) = RTableColumnDescriptor {
    this.accessor = accessor
    this.Header = rFunction("SelectAspectNameHeader") { +headerName }
    this.Cell = selectComponent(onPropertyValueChanged, aspectOptions)
    this.className = "aspect-cell"
}

private fun selectComponent(onAspectChanged: (index: Int, value: AspectData) -> Unit, aspectOptions: Array<AspectData>) = rFunction<RTableRendererProps>("AspectSelectField") { props ->
    child(SuggestingInput::class) {
        attrs {
            options = aspectOptions.map { aspectOption(it, it.name) }.toTypedArray()
            initialValue = props.value?.toString() ?: ""
            onOptionSelected = { onAspectChanged(props.index, it) }
        }
    }
}

/**
 * Creator of a sub table for Aspect row
 */
private fun propertySubComponent(
        onAspectPropertyChanged: (changedAspect: AspectData, propertyChanger: (aspect: AspectData) -> AspectData) -> Unit,
        aspectOptions: Array<AspectData>
): RClass<SubComponentProps> = rFunction("PropertySubComponent") { props ->

    val original = props.original as AspectPropertyRow
    child(AspectPropertySubtable::class) {
        attrs {
            data = original.property.aspect.properties.toTypedArray()
            onPropertyChanged = { propertyChanger -> onAspectPropertyChanged(original.property.aspect, propertyChanger) }
            options = aspectOptions
        }
    }
}

data class AspectPropertyRow(val property: AspectPropertyData, val pending: Boolean)

class AspectPropertySubtable : RComponent<AspectPropertySubtable.Props, AspectPropertySubtable.State>() {

    override fun State.init() {
        pending = HashMap()
    }

    /**
     * Callback is called when one of the AspectData#properties is changed
     */
    private fun onAspectPropertyChanged(changed: AspectData, propertyChanger: (aspect: AspectData) -> AspectData) {
        setState {
            val aspect = pending[changed.id!!]
            pending[changed.id!!] = if (aspect != null) {
                propertyChanger(aspect)
            } else {
                propertyChanger(changed)
            }
        }
    }

    /**
     * Callback creator. Produced callback is called when field (name, measure, domain, baseType) is changed
     */
    private fun fieldChangedHandler(fieldChanger: AspectData.(value: String) -> AspectData) = { aspect: AspectData, value: String ->

        onAspectPropertyChanged(aspect, { it.fieldChanger(value) })
    }

    private fun onInputValueChanged(propertyChanger: AspectPropertyData.(value: String) -> AspectPropertyData) = { changedIndex: Int, value: String ->
        props.onPropertyChanged { aspect: AspectData ->
            AspectData(aspect.id, aspect.name, aspect.measure, aspect.domain, aspect.baseType, aspect.properties.mapIndexed { index, property ->
                if (index == changedIndex) {
                    property.propertyChanger(value)
                } else {
                    property
                }
            })
        }
    }

    private fun onAspectSelectValueChanged(propertyChanger: AspectPropertyData.(value: AspectData) -> AspectPropertyData) = { changedIndex: Int, value: AspectData ->
        props.onPropertyChanged { aspect: AspectData ->
            AspectData(aspect.id, aspect.name, aspect.measure, aspect.domain, aspect.baseType, aspect.properties.mapIndexed { index, property ->
                if (index == changedIndex) {
                    property.propertyChanger(value)
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
            AspectData(aspect.id, aspect.name, aspect.measure, aspect.domain, aspect.baseType, aspect.properties + AspectPropertyData("", "", AspectData(null, "", null, null, null), ""))
        }
    }

    private fun aspectPropertiesToRows(): Array<AspectPropertyRow> {
        return props.data.map {
            val pending = state.pending[it.aspect.id]
            if (pending == null) {
                AspectPropertyRow(it, false)
            } else {
                AspectPropertyRow(it.withAspect(pending), true)
            }
        }.toTypedArray()
    }

    /**
     * Callback that discards all changed that were not yet saved to the server
     */
    private fun resetAspect(aspectId: String) {
        setState {
            pending.remove(aspectId)
        }
    }

    /**
     * Request aspect save if aspect is new (if id == null) (done)
     *   and requests aspect update if id is not null (not done)
     */
    private fun saveAspect(aspectId: String) {
        TODO("SERVER API IS NOT IMPLEMENTED YET")
    }

    override fun RBuilder.render() {
        div(classes = "aspect-property-table-wrapper") {
            ReactTable {
                attrs {
                    columns = arrayOf(
                            aspectColumn("property.name", "Name", aspectPropertyCell(onInputValueChanged(AspectPropertyData::withName))),
                            aspectColumn("property.power", "Power", aspectPropertyCell(onInputValueChanged(AspectPropertyData::withPower))),
                            selectColumn("property.aspect.name", "Name", onAspectSelectValueChanged(AspectPropertyData::withAspect), props.options),
                            aspectColumn("property.aspect.measure", "Measure Unit", aspectPropertyAspectCell(fieldChangedHandler(AspectData::withMeasure))),
                            aspectColumn("property.aspect.domain", "Domain", aspectPropertyAspectCell(fieldChangedHandler(AspectData::withDomain))),
                            aspectColumn("property.aspect.baseType", "Base Type", aspectPropertyAspectCell(fieldChangedHandler(AspectData::withBaseType))),
                            controlsPropertyColumn(::saveAspect, ::resetAspect)
                    )
                    collapseOnDataChange = false
                    className = "aspect-table"
                    SubComponent = propertySubComponent(::onAspectPropertyChanged, props.options)
                    data = aspectPropertiesToRows()
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
         * Array of all aspects (Temporary) (ACHTUNG: REMOVE IF YOU SEE IT)
         */
        var options: Array<AspectData>
        /**
         * Array of children aspect properties
         */
        var data: Array<AspectPropertyData>
        /**
         * Callback to call when property is changed or new property created (callback just marks aspect data as edited)
         */
        var onPropertyChanged: (propertyChanger: (aspect: AspectData) -> AspectData) -> Unit
    }

    interface State : RState {
        /**
         * Map, storing aspects that are updated but not saved yet (to the server)
         */
        var pending: MutableMap<String, AspectData>
    }
}