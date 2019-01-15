package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.model.AspectsModel
import com.infowings.catalog.common.*
import kotlinext.js.require
import react.*

interface AspectEditConsoleModel {
    /**
     * Submit all made changes to server.
     */
    suspend fun submitAspect()

    /**
     * Select the first available property of selected aspect, if such exists, or create new one
     */
    suspend fun switchToProperties()

    /**
     * Discard all saved but not yet updated changes
     */
    fun discardChanges()

    /**
     * Temporarily save changes in memory.
     */
    fun updateAspect(aspectData: AspectData)

    /**
     * Request delete of [AspectData] that is currently being edited
     */
    suspend fun deleteAspect(force: Boolean)
}

interface AspectPropertyEditConsoleModel {
    /**
     * Save [AspectPropertyData] that is currently being edited and then submit all changes made to parent [AspectData]
     * to the server
     */
    suspend fun submitParentAspect()

    /**
     * Save [AspectPropertyData] that is currently being edited and then switch to editing next available
     * [AspectPropertyData] if such exist or to parent [AspectData] otherwise
     */
    suspend fun switchToNextProperty()

    /**
     * Discard all saved but not yet updated changes
     */
    fun discardChanges()

    /**
     * Temporarily save changed aspect property in memory
     */
    fun updateAspectProperty(property: AspectPropertyData)

    /**
     * Save currently edited [AspectPropertyData] as deleted and switch to editing next available [AspectPropertyData]
     * if such exist or to parent [AspectData] otherwise
     */
    suspend fun deleteProperty(force: Boolean)
}

/**
 * Business component. Serves as adapter and router to the right implementation of console (for aspect or for aspect
 * property).
 */
class AspectConsole : RComponent<AspectConsole.Props, RState>(), AspectEditConsoleModel,
    AspectPropertyEditConsoleModel {

    companion object {
        init {
            require("styles/aspect-edit-console.scss") // Styles regarding aspect console
        }
    }

    override suspend fun submitAspect() {
        props.aspectsModel.submitAspect()
    }

    override suspend fun submitParentAspect() {
        props.aspectsModel.submitAspect()
    }

    override suspend fun switchToProperties() {
        val selectedAspect = props.aspect
        if (selectedAspect != props.aspectContext[selectedAspect.id]) {
            props.aspectsModel.submitAspect()
        }
        if (selectedAspect.properties.isNotEmpty()) {
            props.aspectsModel.selectProperty(0)
        } else {
            props.aspectsModel.createProperty(0)
        }
    }

    override fun updateAspect(aspectData: AspectData) {
        props.aspectsModel.updateAspect(aspectData)
    }

    override fun updateAspectProperty(property: AspectPropertyData) {
        props.aspectsModel.updateProperty(property)
    }

    override suspend fun switchToNextProperty() {
        val selectedAspect = props.aspect
        val selectedPropertyIndex = props.propertyIndex
                ?: error("Aspect property should be selected in order to switch to next property")
        val property = selectedAspect.properties[selectedPropertyIndex]
        if (selectedPropertyIndex != selectedAspect.properties.lastIndex || property != emptyAspectPropertyData) {
            if (selectedAspect != props.aspectContext[selectedAspect.id]) {
                props.aspectsModel.submitAspect()
            }
            val nextPropertyIndex = selectedPropertyIndex.inc()
            if (selectedPropertyIndex >= selectedAspect.properties.lastIndex) {
                props.aspectsModel.createProperty(nextPropertyIndex)
            } else {
                props.aspectsModel.selectProperty(nextPropertyIndex)
            }
        }
    }

    override suspend fun deleteAspect(force: Boolean) {
        props.aspectsModel.deleteAspect(force)
    }

    override suspend fun deleteProperty(force: Boolean) {
        props.aspectsModel.deleteAspectProperty(force)
    }

    override fun discardChanges() {
        props.aspectsModel.discardSelect()
    }

    override fun RBuilder.render() {
        val selectedAspect = props.aspect
        val selectedAspectPropertyIndex = props.propertyIndex
        if (selectedAspectPropertyIndex == null) {
            aspectEditConsole {
                attrs {
                    aspect = selectedAspect
                    aspectIsUpdated = props.aspectIsUpdated
                    editConsoleModel = this@AspectConsole
                }
            }
        } else {
            aspectPropertyEditConsole {
                attrs {
                    parentAspect = selectedAspect
                    aspectPropertyIndex = selectedAspectPropertyIndex
                    childAspect = if (selectedAspect.properties[selectedAspectPropertyIndex].aspectId == "") null
                    else props.aspectContext[selectedAspect.properties[selectedAspectPropertyIndex].aspectId]
                    propertyEditConsoleModel = this@AspectConsole
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var aspectIsUpdated: Boolean
        var propertyIndex: Int?
        var aspectContext: Map<String, AspectData>
        var aspectsModel: AspectsModel
    }
}

fun RBuilder.aspectConsole(block: RHandler<AspectConsole.Props>) = child(AspectConsole::class, block)