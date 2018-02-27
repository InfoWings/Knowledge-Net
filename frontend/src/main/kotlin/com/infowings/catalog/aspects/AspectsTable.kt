package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.SuggestingInput
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import com.infowings.catalog.wrappers.table.*

/**
 * Compact method for creating header for table
 */
private fun headerComponent(columnName: String) = rFunction<RTableRendererProps>("AspectHeader") {
    span {
        +columnName
    }
}

/**
 * Compact method for creating table Cell renderer (mutable cells for aspect fields)
 */
private fun cellComponent(onFieldChanged: (data: AspectData, value: String) -> Unit) = rFunction<RTableRendererProps>("AspectField") { props ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = props.value?.toString() ?: ""
            onChangeFunction = { onFieldChanged(props.original.aspect as AspectData, it.asDynamic().target.value) }
        }
    }
}

private fun selectComponent(onFieldChanged: (data: AspectData, value: String) -> Unit) = rFunction<RTableRendererProps>("AspectSelectField") { props ->
    child(SuggestingInput::class) {
        attrs {
            initialValue = props.value?.toString() ?: ""
            onOptionSelected = { onFieldChanged(props.original.aspect as AspectData, it) }
        }
    }
}

/**
 * Compact method for creating aspect column
 */
private fun aspectColumn(accessor: String, header: RClass<RTableRendererProps>, cell: RClass<RTableRendererProps>? = null) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = header
            className = "aspect-cell"
            cell?.let {
                this.Cell = cell
            }
        }

private data class AspectRow(val aspect: AspectData, val pending: Boolean)


/**
 * Component that represents green "+" sign when there is no new aspect being edited.
 * Receives callback on click.
 */
private fun addNewAspectHeaderEnabled(onCreateNewAspect: () -> Unit): RClass<RTableRendererProps> = rFunction("CheckboxHeaderEnabled") {
    div(classes = "create-new-aspect-container") {
        i(classes = "fas fa-plus") {}
        attrs.onClickFunction = { onCreateNewAspect() }
    }
}

/**
 * Component that represents grey "+" sign when new aspect is already being edited but has not yet been saved.
 */
private val addNewAspectHeaderDisabled: RClass<RTableRendererProps> = rFunction("CheckboxHeaderDisabled") {
    div(classes = "create-new-aspect-container create-new-aspect-container--disabled") {
        i(classes = "fas fa-plus") {}
    }
}

/**
 * Creator of a sub table for Aspect row
 */
private fun propertySubComponent(
        aspectsMap: Map<String, AspectData>,
        onAspectPropertyChanged: (changedAspect: AspectData, propertyChanger: (aspect: AspectData) -> AspectData) -> Unit
): RClass<SubComponentProps> = rFunction("PropertySubComponent") { props ->

    val original = props.original as AspectRow
    child(AspectPropertySubtable::class) {
        attrs {
            data = original.aspect.properties.map { AspectPropertyRow(it, aspectsMap[it.aspectId]) }.toTypedArray()
            onPropertyChanged = { propertyChanger -> onAspectPropertyChanged(original.aspect, propertyChanger) }
        }
    }
}

/**
 * Use as: child(AspectsTable::class) {}
 */
class AspectsTable(props: AspectApiReceiverProps) : RComponent<AspectApiReceiverProps, AspectsTable.State>(props) {

    override fun State.init(props: AspectApiReceiverProps) {
        pending = HashMap()
        newAspect = null
    }

    /**
     * Callback is called when one of the AspectData#properties is changed
     */
    private fun onAspectPropertyChanged(changed: AspectData, propertyChanger: (aspect: AspectData) -> AspectData) {
        setState {
            if (changed.id == null) {
                newAspect = propertyChanger(changed)
            } else {
                val aspect = pending[changed.id!!]
                pending[changed.id!!] = if (aspect != null) {
                    propertyChanger(aspect)
                } else {
                    propertyChanger(changed)
                }
            }
        }
    }

    /**
     * Callback creator. Produced callback is called when field (name, measure, domain, baseType) is changed
     */
    private fun fieldChangedHandler(fieldChanger: AspectData.(value: String) -> AspectData) = { aspect: AspectData, value: String ->

        onAspectPropertyChanged(aspect, { it.fieldChanger(value) })
    }

    /**
     * Callback that discards all changed that were not yet saved to the server
     */
    private fun resetAspect(aspectId: String?) {
        setState {
            if (aspectId == null) {
                newAspect = null
            } else {
                pending.remove(aspectId)
            }
        }
    }

    /**
     * Request aspect save if aspect is new (if id == null) (done)
     *   and requests aspect update if id is not null (not done)
     */
    private fun saveAspect(aspectId: String?) {
        if (aspectId == null) {
            val savedAspect = state.newAspect!!
            setState {
                newAspect = null
            }
            props.onAspectCreate(savedAspect)
        }
    }

    /**
     * Callback called when "+" sign is pressed. Creates in memory new aspect with `null` id.
     */
    private fun startCreatingNewAspect() {
        setState {
            newAspect = AspectData(null, "", null, null, null)
        }
    }

    /**
     * Maps aspects to the table rows (considering changed aspects that has not yet been saved)
     */
    private fun aspectsToRows(): Array<AspectRow> {
        val currentAspectRows = props.data.map {
            val pending = state.pending[it.id]
            if (pending == null) {
                AspectRow(it, false)
            } else {
                AspectRow(pending, true)
            }
        }.toTypedArray()
        return if (state.newAspect == null) {
            currentAspectRows
        } else {
            currentAspectRows + AspectRow(state.newAspect!!, true)
        }
    }

    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                        aspectColumn("aspect.name", headerComponent("Name"), cellComponent(fieldChangedHandler(AspectData::withName))),
                        aspectColumn("aspect.measure", headerComponent("Measure Unit"), selectComponent(fieldChangedHandler(AspectData::withMeasure))),
                        aspectColumn("aspect.domain", headerComponent("Domain"), cellComponent(fieldChangedHandler(AspectData::withDomain))),
                        aspectColumn("aspect.baseType", headerComponent("Base Type"), cellComponent(fieldChangedHandler(AspectData::withBaseType))),
                        controlsColumn(
                                if (state.newAspect == null)
                                    addNewAspectHeaderEnabled(::startCreatingNewAspect)
                                else addNewAspectHeaderDisabled,
                                ::saveAspect,
                                ::resetAspect
                        )
                )
                className = "aspect-table"
                data = aspectsToRows()
                loading = props.loading
                SubComponent = propertySubComponent(props.aspectsMap, ::onAspectPropertyChanged)
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
                resizable = false
                collapseOnDataChange = false
            }
        }
    }

    interface State : RState {

        /**
         * Map, storing aspects that are updated but not saved yet (to the server)
         */
        var pending: MutableMap<String, AspectData?>

        /**
         * New aspect that has not yet been saved to the server
         */
        var newAspect: AspectData?
    }
}