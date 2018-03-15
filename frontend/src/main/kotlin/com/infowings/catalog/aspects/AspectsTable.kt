package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import com.infowings.catalog.wrappers.table.*

data class AspectRow(val aspect: AspectData, val pending: Boolean)

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
        context: Map<String, AspectData>,
        onAspectUpdate: (AspectData) -> Unit
): RClass<SubComponentProps> = rFunction("PropertySubComponent") { props ->

    val original = props.original as AspectRow
    child(AspectPropertySubtable::class) {
        attrs {
            data = original.aspect.properties.toTypedArray()
            onPropertyChanged = { propertyChanger -> onAspectPropertyChanged(original.aspect, propertyChanger) }
            aspectContext = context
            this.onAspectUpdate = onAspectUpdate
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
     * Callback is called when one of [AspectData] fields is changed
     * If aspect is new and has not yet been saved, changes it.
     * If aspect is already exists but have not yet been modified, places aspect inside pending context
     * If aspect has already been modified, changes it inside pending context
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
     * Callback that discards all changed that were not yet saved to the server.
     * Removes aspect from pending context, if aspect already exists, removes new aspect otherwise.
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
     * Confirmation callback that requests aspect save if id == null (aspect is new), or requests aspect update
     * if id != null (aspect already exists but has been modified)
     */
    private fun saveAspect(aspectId: String?) {
        if (aspectId == null) {
            val savedAspect = state.newAspect!!
            setState {
                newAspect = null
            }
            props.onAspectCreate(savedAspect)
        } else {
            val updatedAspect = state.pending[aspectId]!!
            setState {
                pending.remove(aspectId)
            }
            props.onAspectUpdate(updatedAspect)
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
                        simpleTableColumn("aspect.name", "Name", aspectCell { value -> onAspectPropertyChanged(this, { it.copy(name = value) }) }),
                        simpleTableColumn("aspect.measure", "Measure Unit", measurementUnitAspectCell { value -> onAspectPropertyChanged(this, { it.copy(measure = value) }) }),
                        simpleTableColumn("aspect.domain", "Domain", aspectCell { value -> onAspectPropertyChanged(this, { it.copy(domain = value) }) }),
                        simpleTableColumn("aspect.baseType", "Base Type", aspectCell { value -> onAspectPropertyChanged(this, { it.copy(baseType = value) }) }),
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
                SubComponent = propertySubComponent(::onAspectPropertyChanged, props.aspectContext, props.onAspectUpdate)
                showPagination = false
                minRows = 0
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