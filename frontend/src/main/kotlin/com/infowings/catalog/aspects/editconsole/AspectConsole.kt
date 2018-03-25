package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import kotlinext.js.invoke
import kotlinext.js.require
import react.*

class AspectConsole : RComponent<AspectConsole.Props, RState>() {

    companion object {
        init {
            require("styles/aspect-edit-console.scss") // Styles regarding aspect console
        }
    }

    private fun handleSubmitAspectChanges(aspect: AspectData) {
        props.onAspectUpdate(aspect)
        props.onSubmit() // FIXME: Server call occurs before setState completes
    }

    private fun handleSwitchToAspectProperties(aspect: AspectData) {
        val selectedAspect = props.aspect ?: error("Aspect should be selected in order to save changes")
        props.onAspectUpdate(aspect)
        if (selectedAspect.properties.isNotEmpty()) {
            props.onSelectProperty(selectedAspect.id, 0)
        } else {
            props.onCreateProperty(0)
        }
    }

    private fun handleSwitchToNextProperty(property: AspectPropertyData) {
        val selectedAspect = props.aspect ?: error("Aspect should be selected in order to save changes")
        val selectedPropertyIndex = props.propertyIndex
                ?: error("Aspect property should be selected in order to switch to next property")
        props.onAspectPropertyUpdate(property)
        if (selectedPropertyIndex + 1 > selectedAspect.properties.lastIndex) {
            props.onCreateProperty(selectedPropertyIndex + 1)
        } else {
            props.onSelectProperty(selectedAspect.id, selectedPropertyIndex + 1)
        }
    }

    private fun handleSaveParentAspect(property: AspectPropertyData) {
        props.onAspectPropertyUpdate(property)
        props.onSubmit() // FIXME: Server call occurs before setState completes
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
                        onSubmit = ::handleSubmitAspectChanges
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
                        onSaveParentAspect = ::handleSaveParentAspect
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
        var onAspectUpdate: (AspectData) -> Unit
        var onAspectPropertyUpdate: (AspectPropertyData) -> Unit
        var onSubmit: () -> Unit
    }
}

fun RBuilder.aspectConsole(block: RHandler<AspectConsole.Props>) = child(AspectConsole::class, block)