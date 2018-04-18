package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.emptySubjectData
import com.infowings.catalog.components.delete.deleteButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.popup.forceRemoveConfirmWindow
import com.infowings.catalog.components.searchbar.searchBar
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.wrappers.blueprint.EditableText
import kotlinext.js.require
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.div
import react.setState
import kotlin.browser.window

class SubjectsListComponent : RComponent<SubjectApiReceiverProps, SubjectsListComponent.State>() {

    companion object {
        init {
            require("styles/subjects-list.scss")
        }
    }

    private var timer: Int = 0

    private fun updateSubjectName(newName: String, subjectData: SubjectData) {
        if (subjectData.name != newName && newName.isNotBlank()) {
            submitSubject(subjectData.copy(name = newName))
        }
    }

    private fun updateSubjectDescription(newDescription: String, subjectData: SubjectData) {
        if (subjectData.description != newDescription) {
            submitSubject(subjectData.copy(description = newDescription))
        }
    }

    private fun submitSubject(subjectData: SubjectData) {
        when (subjectData.id) {
            null -> props.onSubjectsCreate(subjectData)
            else -> props.onSubjectUpdate(subjectData)
        }
    }

    private fun forceDeleteSubject() {
        val deletedSubject =
            state.linkedEntitiesSubject ?: error("Subject with linked entities must exist in order to force delete")
        launch {
            props.onSubjectDelete(deletedSubject, true)
        }
        setState {
            linkedEntitiesSubject = null
        }
    }

    private fun tryDeleteSubject(subjectData: SubjectData) {
        launch {
            try {
                props.onSubjectDelete(subjectData, false)
            } catch (exception: BadRequestException) {
                setState {
                    linkedEntitiesSubject = subjectData
                }
            }
        }
    }

    override fun RBuilder.render() {

        div(classes = "subjects-list") {

            searchBar {
                attrs {
                    className = "subjects-list--search-bar"
                    filterText = state.filterText
                    onFilterTextChange = { text ->
                        window.clearTimeout(timer)
                        timer = window.setTimeout({ props.onFetchData(mapOf("name" to text)) }, 200)
                    }
                }
            }

            props.data.forEach { subjectData ->
                div(classes = "subjects-list--subject-item") {
                    attrs {
                        key = subjectData.id ?: error("Server sent Subject with id == null")
                    }
                    EditableText {
                        attrs {
                            className = "subjects-list--name"
                            defaultValue = subjectData.name
                            onConfirm = { updateSubjectName(it, subjectData) }
                        }
                    }
                    descriptionComponent(
                        className = "subjects-list--description",
                        description = subjectData.description,
                        onNewDescriptionConfirmed = { updateSubjectDescription(it, subjectData) },
                        onEditStarted = null
                    )
                    deleteButtonComponent(
                        onDeleteClick = { tryDeleteSubject(subjectData) },
                        entityName = "subject"
                    )
                }
            }

            div(classes = "subjects-list--subject-item") {
                attrs {
                    key = props.data.size.toString()
                }
                EditableText {
                    attrs {
                        className = "subjects-list--name"
                        defaultValue = ""
                        placeholder = "Click to enter new subject"
                        onConfirm = { updateSubjectName(it, emptySubjectData) }
                    }
                }
            }
        }
        forceRemoveConfirmWindow {
            attrs {
                isOpen = state.linkedEntitiesSubject != null
                message = "Subject has linked entities"
                onCancel = { setState { linkedEntitiesSubject = null } }
                onConfirm = this@SubjectsListComponent::forceDeleteSubject
            }
        }
    }

    interface State : RState {
        var filterText: String
        var linkedEntitiesSubject: SubjectData?
    }
}
