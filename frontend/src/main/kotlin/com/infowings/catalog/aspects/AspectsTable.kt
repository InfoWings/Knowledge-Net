package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import com.infowings.catalog.wrappers.table.*

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
        onAspectPropertyChanged: (changedAspect: AspectData, propertyChanger: (aspect: AspectData) -> AspectData) -> Unit,
        context: Map<String, AspectData>
): RClass<SubComponentProps> = rFunction("PropertySubComponent") { props ->

    val original = props.original as AspectRow
    child(AspectPropertySubtable::class) {
        attrs {
            data = original.aspect.properties.toTypedArray()
            onPropertyChanged = { propertyChanger -> onAspectPropertyChanged(original.aspect, propertyChanger) }
            aspectContext = context
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
                        aspectColumn("aspect.name", "Name", aspectCell(fieldChangedHandler(AspectData::withName))),
                        aspectColumn("aspect.measure", "Measure Unit", aspectCell(fieldChangedHandler(AspectData::withMeasure))),
                        aspectColumn("aspect.domain", "Domain", aspectCell(fieldChangedHandler(AspectData::withDomain))),
                        aspectColumn("aspect.baseType", "Base Type", aspectCell(fieldChangedHandler(AspectData::withBaseType))),
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
                SubComponent = propertySubComponent(::onAspectPropertyChanged, props.aspectContext)
                showPagination = false
                minRows = 1
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