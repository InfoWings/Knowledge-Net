package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import com.infowings.catalog.wrappers.table.SubComponentProps
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.i

data class AspectPropertyRow(val property: AspectPropertyData, val aspect: AspectData, val pending: Boolean)

/**
 * Convenient method for creating column with react-select input in aspect property subtable.
 */
private fun selectColumn(accessor: String, headerName: String, onPropertyValueChanged: (index: Int, value: String) -> Unit, onAspectNameChanged: AspectData.(name: String) -> Unit) = RTableColumnDescriptor {
    this.accessor = accessor
    this.Header = rFunction("SelectAspectNameHeader") { +headerName }
    this.Cell = selectComponent(onPropertyValueChanged, onAspectNameChanged)
    this.className = "aspect-cell"
}

/**
 * Convenient method, creator of Cell for selectColumn. Renders AspectSuggestionInput.
 */
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
 * Creator of a sub table for Aspect property row (subtable inside subtable)
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

class AspectPropertySubtable : RComponent<AspectPropertySubtable.Props, AspectPropertySubtable.State>() {

    override fun State.init() {
        pending = HashMap()
    }

    /**
     * Callback is called when any field of [AspectData] is changed.
     * Places [AspectData] inside pending context, if it has not yet been changed or changes [AspectData] inside pending
     * context if it is already there.
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
     * Creator of a handler for aspect properties (AspectData#properties). Index of changed property is retrieved from
     *
     * @param propertyChanger function that creates new AspectPropertyData given the updated [String] value. Usually is
     * a lambda { copy(... = it) }
     * @return change handler that should be called by index and new value. Handler changes AspectData inside pending context
     */
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
     * Callback to call when new property is created. Notifies parent aspect that new property is created.
     * Places the aspect inside pending context or changes aspect inside pending context.
     */
    private fun onNewPropertyCreated(e: Event) {
        props.onPropertyChanged { aspect: AspectData ->
            AspectData(aspect.id, aspect.name, aspect.measure, aspect.domain, aspect.baseType, aspect.properties + AspectPropertyData("", "", "", ""))
        }
    }

    /**
     * Returns [Array] of [AspectPropertyRow] that can be displayed inside react-table. Binds [AspectPropertyData] with
     * [AspectData] by [AspectPropertyData.aspectId], considering pending changes to the [AspectData] that should be
     * displayed
     */
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
     * Callback that discards all changed that were not yet saved to the server.
     * Removes all changes made from the pending context.
     */
    private fun resetAspect(aspectId: String) {
        setState {
            pending.remove(aspectId)
        }
    }

    /**
     * Removes aspectId from pending context and requests aspect update to the server.
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
                            simpleTableColumn("property.name", "Name", aspectPropertyCell(indexedPropertyValueChangedHandler { copy(name = it) })),
                        simpleTableColumn(
                            "property.cardinality",
                            "Cardinality",
                            aspectPropertyCell(indexedPropertyValueChangedHandler { copy(cardinality = it) })
                        ),
                            selectColumn("aspect.name", "Name", indexedPropertyValueChangedHandler { copy(aspectId = it) }, { value -> onAspectPropertyChanged(this, { it.copy(baseType = value) }) }),
                            simpleTableColumn("aspect.measure", "Measure Unit", aspectPropertyAspectCell { value -> onAspectPropertyChanged(this, { it.copy(measure = value) }) }),
                            simpleTableColumn("aspect.domain", "Domain", aspectPropertyAspectCell { value -> onAspectPropertyChanged(this, { it.copy(domain = value) }) }),
                            simpleTableColumn("aspect.baseType", "Base Type", aspectPropertyAspectCell { value -> onAspectPropertyChanged(this, { it.copy(baseType = value) }) }),
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
         * Aspect context, that is just passed to the next subtable to recreate tree structure
         * (get aspects for aspect properties)
         */
        var aspectContext: Map<String, AspectData>
        /**
         * Callback to call when property is changed or new property created (callback just marks aspect data as edited)
         */
        var onPropertyChanged: (propertyChanger: (aspect: AspectData) -> AspectData) -> Unit
        /**
         * Callback on confirmation of AspectData update (sends update to server).
         * Passed from subtable to subtable.
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