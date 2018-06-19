package com.infowings.catalog.subjects

import com.infowings.catalog.common.BadRequest
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.emptySubjectData
import com.infowings.catalog.components.delete.deleteButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.popup.forceRemoveConfirmWindow
import com.infowings.catalog.components.reference.referenceButtonComponent
import com.infowings.catalog.components.searchbar.searchBar
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.utils.ServerException
import com.infowings.catalog.utils.replaceBy
import com.infowings.catalog.wrappers.blueprint.*
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinext.js.require
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.json.JSON
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

    override fun State.init() {
        errorMessages = emptyList()
        data = emptyList()
    }

    override fun componentWillReceiveProps(nextProps: SubjectApiReceiverProps) {
        setState {
            data = nextProps.data.toList()
        }
    }

    private var timer: Int = 0

    private fun updateSubjectName(newName: String, subjectData: SubjectData) {
        console.log(newName)
        console.log(subjectData)
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
        launch {
            try {
                when (subjectData.id) {
                    null -> props.onSubjectsCreate(subjectData)
                    else -> props.onSubjectUpdate(subjectData)
                }
            } catch (exception: BadRequestException) {
                setState {
                    val errorMessage = JSON.parse<BadRequest>(exception.message).message
                    errorMessage?.let { errorMessages += it }
                }
            } catch (exception: ServerException) {
                setState {
                    errorMessages += "Oops, something went wrong, changes weren't saved"
                }
            }
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
                    refreshSubjects = props.refreshSubjects
                }
            }

            state.data.forEach { subjectData ->
                div(classes = "subjects-list--subject-item") {
                    attrs {
                        key = subjectData.id ?: error("Server sent Subject with id == null")
                    }
                    EditableText {
                        attrs {
                            className = "subjects-list--name"
                            value = subjectData.name
                            onConfirm = {
                                updateSubjectName(it, props.data.find { it.id == subjectData.id }!!)
                            }
                            onChange = {
                                setState {
                                    data = data.replaceBy(subjectData.copy(name = it)) {
                                        it.id == subjectData.id
                                    }
                                }
                            }
                            confirmOnEnterKey = true
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
                    referenceButtonComponent(subjectData, props.history)
                }
            }

            div(classes = "subjects-list--subject-item") {
                attrs {
                    key = state.data.size.toString()
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

        Toaster {
            attrs {
                position = Position.TOP_RIGHT
            }
            state.errorMessages.reversed().forEach { errorMessage ->
                Toast {
                    attrs {
                        icon = "warning-sign"
                        intent = Intent.DANGER
                        message = errorMessage.asReactElement()
                        onDismiss = {
                            setState {
                                errorMessages = errorMessages.filterNot { errorMessage == it }
                            }
                        }
                        timeout = 7000
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
        var data: List<SubjectData>
        var errorMessages: List<String>
    }
}
