package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.coroutines.experimental.launch
import react.*

/**
 * Business component. Serves as adapter and router to the right implementation of console (for aspect or for aspect
 * property).
 */
class AspectConsole : RComponent<AspectConsole.Props, RState>() {

    companion object {
        init {
            require("styles/aspect-edit-console.scss") // Styles regarding aspect console
        }
    }

    private suspend fun handleSubmitAspectChanges(aspect: AspectData) {
        props.onAspectUpdate(aspect)
        props.onSubmit()
    }

    private suspend fun handleSaveParentAspect(property: AspectPropertyData) {
        props.onAspectPropertyUpdate(property)
        props.onSubmit()
    }

    private fun handleDeleteSelectedAspectProperty() {
        val selectedAspect = props.aspect
        val selectedPropertyIndex = props.propertyIndex
                ?: error("Aspect property should be selected in order to delete property")

        launch {
            props.onAspectPropertyUpdate(selectedAspect.properties[selectedPropertyIndex].copy(deleted = true))

            if (selectedAspect.hasNextAliveProperty(selectedPropertyIndex.inc())) {
                props.onSelectProperty(
                    selectedAspect.id,
                    selectedAspect.getNextAlivePropertyIndex(selectedPropertyIndex.inc())
                )
            } else {
                props.onSelectAspect(selectedAspect.id)
            }
        }
    }

    private fun handleSwitchToAspectProperties(aspect: AspectData) {
        val selectedAspect = props.aspect
        launch {
            props.onAspectUpdate(aspect)
            if (selectedAspect.properties.isNotEmpty()) {
                if (selectedAspect.hasNextAliveProperty(0))
                    props.onSelectProperty(selectedAspect.id, selectedAspect.getNextAlivePropertyIndex(0))
                else
                    props.onCreateProperty(selectedAspect.properties.size)
            } else {
                props.onCreateProperty(0)
            }
        }
    }

    private fun handleSwitchToNextProperty(property: AspectPropertyData) {
        val selectedAspect = props.aspect
        val selectedPropertyIndex = props.propertyIndex
                ?: error("Aspect property should be selected in order to switch to next property")
        launch {
            if (selectedPropertyIndex != selectedAspect.properties.lastIndex || property != AspectPropertyData("", "", "", "")) {
                props.onAspectPropertyUpdate(property)
                val nextPropertyIndex = selectedPropertyIndex.inc()
                if (selectedPropertyIndex >= selectedAspect.properties.lastIndex) {
                    props.onCreateProperty(nextPropertyIndex)
                } else {
                    if (selectedAspect.hasNextAliveProperty(nextPropertyIndex)) {
                        props.onSelectProperty(
                            selectedAspect.id,
                            selectedAspect.getNextAlivePropertyIndex(nextPropertyIndex)
                        )
                    } else {
                        props.onCreateProperty(selectedAspect.properties.size)
                    }
                }
            }
        }
    }

    override fun RBuilder.render() {
        val selectedAspect = props.aspect
        val selectedAspectPropertyIndex = props.propertyIndex
        if (selectedAspectPropertyIndex == null) {
            aspectEditConsole {
                attrs {
                    aspect = selectedAspect
                    onCancel = props.onCancel
                    onSubmit = { handleSubmitAspectChanges(it) }
                    onSwitchToProperties = ::handleSwitchToAspectProperties
                    onDelete = props.onAspectDelete
                }
            }
        } else {
            aspectPropertyEditConsole {
                attrs {
                    parentAspect = selectedAspect
                    aspectPropertyIndex = selectedAspectPropertyIndex
                    childAspect = if (selectedAspect.properties[selectedAspectPropertyIndex].aspectId == "") null
                    else props.aspectContext(selectedAspect.properties[selectedAspectPropertyIndex].aspectId)
                    onCancel = props.onCancel
                    onSwitchToNextProperty = ::handleSwitchToNextProperty
                    onSaveParentAspect = { handleSaveParentAspect(it) }
                    onDelete = ::handleDeleteSelectedAspectProperty
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var propertyIndex: Int?
        var aspectContext: (String) -> AspectData?
        var onSelectProperty: (String?, Int) -> Unit
        var onSelectAspect: (String?) -> Unit
        var onCreateProperty: (Int) -> Unit
        var onCancel: () -> Unit
        var onAspectUpdate: suspend (AspectData) -> Unit
        var onAspectPropertyUpdate: suspend (AspectPropertyData) -> Unit
        var onAspectDelete: suspend (force: Boolean) -> Unit
        var onSubmit: suspend () -> Unit
    }
}

private fun AspectData.hasNextAliveProperty(index: Int) = if (index >= properties.size) false else
    properties.size > index && properties.subList(index, properties.size).indexOfFirst { !it.deleted } != -1

private fun AspectData.getNextAlivePropertyIndex(index: Int) =
    properties.subList(index, properties.size).indexOfFirst { !it.deleted } + index

fun RBuilder.aspectConsole(block: RHandler<AspectConsole.Props>) = child(AspectConsole::class, block)