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
import org.w3c.dom.events.Event

private fun selectColumn(accessor: String, headerName: String, onPropertyValueChanged: (index: Int, value: String) -> Unit, onAspectNameChanged: AspectData.(name: String) -> Unit) = RTableColumnDescriptor {
    this.accessor = accessor
    this.Header = rFunction("SelectAspectNameHeader") { +headerName }
    this.Cell = selectComponent(onPropertyValueChanged, onAspectNameChanged)
    this.className = "aspect-cell"
}

private fun selectComponent(onAspectChanged: (index: Int, value: String) -> Unit, onAspectModified: AspectData.(name: String) -> Unit) = rFunction<RTableRendererProps>("AspectSelectField") { props ->
    child(AspectSuggestingInput::class) {
        attrs {
            associatedAspect = props.original.aspect as AspectData
            onOptionSelected = { onAspectChanged(props.index, it.id!!) }
            onAspectNameChanged = onAspectModified
        }
    }
}

/**
 * Creator of a sub table for Aspect row
 */
private fun propertySubComponent(
        onAspectPropertyChanged: (changedAspect: AspectData, propertyChanger: (aspect: AspectData) -> AspectData) -> Unit,
        context: Map<String, AspectData>,
        onAspectUpdate: (AspectData) -> Unit
): RClass<SubComponentProps> = rFunction("PropertySubComponent") { props ->

    val original = props.original as AspectPropertyRow
    child(AspectPropertySubtable::class) {
        attrs {
            // Does local context always contains
            data = original.aspect.properties.toTypedArray()
            onPropertyChanged = { propertyChanger -> onAspectPropertyChanged(original.aspect, propertyChanger) }
            aspectContext = context
            this.onAspectUpdate = onAspectUpdate
        }
    }
}

data class AspectPropertyRow(val property: AspectPropertyData, val aspect: AspectData, val pending: Boolean)

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

    private fun indexedPropertyValueChangedHandler(propertyChanger: AspectPropertyData.(value: String) -> AspectPropertyData) = { changedIndex: Int, value: String ->
        props.onPropertyChanged {
            it.copy(properties = it.properties.mapIndexed { index, property ->
                if (index == changedIndex)
                    property.propertyChanger(value)
                else property
            })
        }
    }

    /**
     * Callback to call when new property is created
     */
    private fun onNewPropertyCreated(e: Event) {
        props.onPropertyChanged { aspect: AspectData ->
            AspectData(aspect.id, aspect.name, aspect.measure, aspect.domain, aspect.baseType, aspect.properties + AspectPropertyData("", "", "", ""))
        }
    }

    private fun aspectPropertiesToRows(): Array<AspectPropertyRow> {
        return props.data.map {
            val pending = state.pending[it.aspectId]
            if (pending == null) {
                AspectPropertyRow(it, props.aspectContext[it.aspectId] ?: AspectData(null, "", null, null, null), false)
            } else {
                AspectPropertyRow(it, pending, true)
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
        val updatedAspect = state.pending[aspectId]!!
        setState {
            pending.remove(aspectId)
        }
        props.onAspectUpdate(updatedAspect)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-property-table-wrapper") {
            ReactTable {
                attrs {
                    columns = arrayOf(
                            aspectColumn("property.name", "Name", aspectPropertyCell(indexedPropertyValueChangedHandler { copy(name = it) })),
                            aspectColumn("property.power", "Power", aspectPropertyCell(indexedPropertyValueChangedHandler { copy(power = it) })),
                            selectColumn("aspect.name", "Name", indexedPropertyValueChangedHandler { copy(aspectId = it) }, { value -> onAspectPropertyChanged(this, { it.copy(baseType = value) }) }),
                            aspectColumn("aspect.measure", "Measure Unit", aspectPropertyAspectCell { value -> onAspectPropertyChanged(this, { it.copy(measure = value) }) }),
                            aspectColumn("aspect.domain", "Domain", aspectPropertyAspectCell { value -> onAspectPropertyChanged(this, { it.copy(domain = value) }) }),
                            aspectColumn("aspect.baseType", "Base Type", aspectPropertyAspectCell { value -> onAspectPropertyChanged(this, { it.copy(baseType = value) }) }),
                            controlsPropertyColumn(::saveAspect, ::resetAspect)
                    )
                    collapseOnDataChange = false
                    className = "aspect-table"
                    SubComponent = propertySubComponent(::onAspectPropertyChanged, props.aspectContext, props.onAspectUpdate)
                    data = aspectPropertiesToRows()
                    showPagination = false
                    minRows = 1
                    sortable = false
                    showPageJump = false
                    resizable = false
                }
            }
            div(classes = "new-property-button") {
                i(classes = "fas fa-plus") {}
                attrs.onClickFunction = ::onNewPropertyCreated
            }
        }
    }

    interface Props : RProps {
        /**
         * Array of children aspect properties
         */
        var data: Array<AspectPropertyData>
        /**
         *
         */
        var aspectContext: Map<String, AspectData>
        /**
         * Callback to call when property is changed or new property created (callback just marks aspect data as edited)
         */
        var onPropertyChanged: (propertyChanger: (aspect: AspectData) -> AspectData) -> Unit
        /**
         *
         */
        var onAspectUpdate: (AspectData) -> Unit
    }

    interface State : RState {
        /**
         * Map, storing aspects that are updated but not saved yet (to the server)
         */
        var pending: MutableMap<String, AspectData>
    }
}