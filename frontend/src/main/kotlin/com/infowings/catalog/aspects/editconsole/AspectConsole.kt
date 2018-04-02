package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.AspectsModel
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.emptyAspectPropertyData
import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.coroutines.experimental.launch
import react.*

interface AspectEditConsoleModel {
    /**
     * Save [AspectData] that is currently being edited and then submitting changes to the server
     */
    suspend fun submitAspect(aspect: AspectData)

    /**
     * Save [AspectData] that is currently being edited and then switch to editing first visible (not
     * deleted) [AspectPropertyData]
     */
    fun switchToProperties(aspect: AspectData)

    /**
     * Discard all saved but not yet updated changes
     */
    fun discardChanges()

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
    suspend fun submitParentAspect(property: AspectPropertyData)

    /**
     * Save [AspectPropertyData] that is currently being edited and then switch to editing next available
     * [AspectPropertyData] if such exist or to parent [AspectData] otherwise
     */
    fun switchToNextProperty(property: AspectPropertyData)

    /**
     * Discard all saved but not yet updated changes
     */
    fun discardChanges()

    /**
     * Save currently edited [AspectPropertyData] as deleted and switch to editing next available [AspectPropertyData]
     * if such exist or to parent [AspectData] otherwise
     */
    fun deleteProperty()
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

    override suspend fun submitAspect(aspect: AspectData) {
        props.aspectsModel.updateAspect(aspect)
        props.aspectsModel.submitAspect()
    }

    override suspend fun submitParentAspect(property: AspectPropertyData) {
        props.aspectsModel.updateProperty(property)
        props.aspectsModel.submitAspect()
    }

    override fun switchToProperties(aspect: AspectData) {
        val selectedAspect = props.aspect
        launch {
            props.aspectsModel.updateAspect(aspect)
            if (selectedAspect.properties.isNotEmpty()) {
                if (selectedAspect.hasNextAliveProperty(0))
                    props.aspectsModel.selectAspectProperty(
                        selectedAspect.id,
                        selectedAspect.getNextAlivePropertyIndex(0)
                    )
                else
                    props.aspectsModel.createProperty(selectedAspect.properties.size)
            } else {
                props.aspectsModel.createProperty(0)
            }
        }
    }

    override fun switchToNextProperty(property: AspectPropertyData) {
        val selectedAspect = props.aspect
        val selectedPropertyIndex = props.propertyIndex
                ?: error("Aspect property should be selected in order to switch to next property")
        launch {
            if (selectedPropertyIndex != selectedAspect.properties.lastIndex || property != emptyAspectPropertyData) {
                props.aspectsModel.updateProperty(property)
                val nextPropertyIndex = selectedPropertyIndex.inc()
                if (selectedPropertyIndex >= selectedAspect.properties.lastIndex) {
                    props.aspectsModel.createProperty(nextPropertyIndex)
                } else {
                    if (selectedAspect.hasNextAliveProperty(nextPropertyIndex)) {
                        props.aspectsModel.selectAspectProperty(
                            selectedAspect.id,
                            selectedAspect.getNextAlivePropertyIndex(nextPropertyIndex)
                        )
                    } else {
                        props.aspectsModel.createProperty(selectedAspect.properties.size)
                    }
                }
            }
        }
    }

    override suspend fun deleteAspect(force: Boolean) {
        props.aspectsModel.deleteAspect(force)
    }

    override fun deleteProperty() {
        val selectedAspect = props.aspect
        val selectedPropertyIndex = props.propertyIndex
                ?: error("Aspect property should be selected in order to delete property")

        launch {
            props.aspectsModel.updateProperty(selectedAspect.properties[selectedPropertyIndex].copy(deleted = true))

            if (selectedAspect.hasNextAliveProperty(selectedPropertyIndex.inc())) {
                props.aspectsModel.selectAspectProperty(
                    selectedAspect.id,
                    selectedAspect.getNextAlivePropertyIndex(selectedPropertyIndex.inc())
                )
            } else {
                props.aspectsModel.selectAspect(selectedAspect.id)
            }
        }
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
                    editConsoleModel = this@AspectConsole
                }
            }
        } else {
            aspectPropertyEditConsole {
                attrs {
                    parentAspect = selectedAspect
                    aspectPropertyIndex = selectedAspectPropertyIndex
                    childAspect = if (selectedAspect.properties[selectedAspectPropertyIndex].aspectId == "") null
                    else props.aspectContext(selectedAspect.properties[selectedAspectPropertyIndex].aspectId)
                    propertyEditConsoleModel = this@AspectConsole
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var propertyIndex: Int?
        var aspectContext: (String) -> AspectData?
        var aspectsModel: AspectsModel
    }
}

private fun AspectData.hasNextAliveProperty(index: Int) = if (index >= properties.size) false else
    properties.size > index && properties.subList(index, properties.size).indexOfFirst { !it.deleted } != -1

private fun AspectData.getNextAlivePropertyIndex(index: Int) =
    properties.subList(index, properties.size).indexOfFirst { !it.deleted } + index

fun RBuilder.aspectConsole(block: RHandler<AspectConsole.Props>) = child(AspectConsole::class, block)