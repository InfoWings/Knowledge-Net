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

    private fun handleSwitchToAspectProperties(aspect: AspectData) {
        val selectedAspect = props.aspect ?: error("Aspect should be selected in order to save changes")
        launch {
            props.onAspectUpdate(aspect)
            if (selectedAspect.properties.isNotEmpty()) {
                props.onSelectProperty(selectedAspect.id, 0)
            } else {
                props.onCreateProperty(0)
            }
        }
    }

    private fun handleSwitchToNextProperty(property: AspectPropertyData) {
        val selectedAspect = props.aspect ?: error("Aspect should be selected in order to save changes")
        val selectedPropertyIndex = props.propertyIndex
                ?: error("Aspect property should be selected in order to switch to next property")
        launch {
            if (selectedPropertyIndex != selectedAspect.properties.lastIndex || property != AspectPropertyData("", "", "", "")) {
                props.onAspectPropertyUpdate(property)
                if (selectedPropertyIndex + 1 > selectedAspect.properties.lastIndex) {
                    props.onCreateProperty(selectedPropertyIndex + 1)
                } else {
                    props.onSelectProperty(selectedAspect.id, selectedPropertyIndex + 1)
                }
            }
        }
    }

    override fun RBuilder.render() {
        val selectedAspect = props.aspect
        val selectedAspectPropertyIndex = props.propertyIndex
        when {
            selectedAspect != null && selectedAspectPropertyIndex == null ->
                aspectEditConsole {
                    attrs {
                        aspect = selectedAspect
                        onCancel = props.onCancel
                        onSubmit = { handleSubmitAspectChanges(it) }
                        onSwitchToProperties = ::handleSwitchToAspectProperties
                    }
                }
            selectedAspect != null && selectedAspectPropertyIndex != null ->
                aspectPropertyEditConsole {
                    attrs {
                        parentAspect = selectedAspect
                        aspectPropertyIndex = selectedAspectPropertyIndex
                        childAspect = if (selectedAspect.properties[selectedAspectPropertyIndex].aspectId == "") null
                        else props.aspectContext(selectedAspect.properties[selectedAspectPropertyIndex].aspectId)
                        onCancel = props.onCancel
                        onSwitchToNextProperty = ::handleSwitchToNextProperty
                        onSaveParentAspect = { handleSaveParentAspect(it) }
                    }
                }
        }
    }

    interface Props : RProps {
        var aspect: AspectData?
        var propertyIndex: Int?
        var aspectContext: (String) -> AspectData?
        var onSelectProperty: (String?, Int) -> Unit
        var onCreateProperty: (Int) -> Unit
        var onCancel: () -> Unit
        var onAspectUpdate: suspend (AspectData) -> Unit
        var onAspectPropertyUpdate: suspend (AspectPropertyData) -> Unit
        var onSubmit: suspend () -> Unit
    }
}

fun RBuilder.aspectConsole(block: RHandler<AspectConsole.Props>) = child(AspectConsole::class, block)