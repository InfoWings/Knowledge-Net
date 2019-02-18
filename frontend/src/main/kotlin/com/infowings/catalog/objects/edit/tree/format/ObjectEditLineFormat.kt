package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.components.delete.deletePopoverWindow
import com.infowings.catalog.components.guid.copyGuidButton
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.SubjectTruncated
import com.infowings.catalog.objects.edit.tree.inputs.objectSubject
import com.infowings.catalog.utils.buildWithProperties
import com.infowings.catalog.wrappers.blueprint.*
import com.infowings.catalog.wrappers.button
import com.infowings.catalog.wrappers.react.asReactElement
import react.*
import react.dom.*

fun RBuilder.objectEditLineFormat(builder: ObjectEditHeader.Props.() -> Unit) = buildWithProperties<ObjectEditHeader.Props, ObjectEditHeader>(builder)

class ObjectEditHeader(props: Props) : RComponent<ObjectEditHeader.Props, ObjectEditHeader.State>(props) {
    private var editedDescription: String = ""
    private var descriptionEdited: Boolean = false


    override fun RBuilder.render() {
        Callout {
            attrs {
                intent = Intent.PRIMARY
                icon = null
            }
            div {
                h2 {
                    EditableText {
                        attrs {
                            defaultValue = props.name ?: ""
                            onChange = { props.onNameChanged(it) }
                            placeholder = "Object name"
                            disabled = props.disabled
                        }
                    }
                    div(classes = "object-tree-edit__header-copy-guid-button") {
                        copyGuidButton(props.guid)
                    }
                }
                EditableText {
                    attrs {
                        multiline = true
                        maxLines = 3
                        defaultValue = props.description ?: ""
                        onChange = {
                            setState {
                                editedDescription = it
                                descriptionEdited = true
                            }
                            props.onDescriptionChanged(it)
                        }
                        placeholder = "Add description"
                        disabled = props.disabled
                    }
                }
                div {
                    div(classes = "object-tree-edit__object") {
                        span(classes = "object-tree-edit__label") {
                            +"Subject:"
                        }
                        objectSubject(
                            value = props.subject,
                            onSelect = props.onSubjectChanged,
                            disabled = props.disabled
                        )
                        props.onUpdateObject?.let {
                            submitButtonComponent(it)
                        }

                        button {
                            onClick = { props.onCreateNewProperty?.invoke() }
                            intent = Intent.SUCCESS
                            icon = "plus"
                            text = "Create property".asReactElement()
                            disabled = props.onCreateNewProperty == null
                        }
                        child(DeleteObjectButton::class) {
                            attrs.onSubmit = props.onDeleteObject
                        }
                    }
                }
            }
        }
    }


    class State() : RState
    class Props(
        var name: String,
        var subject: SubjectTruncated,
        var description: String?,
        var guid: String?,
        var onNameChanged: (String) -> Unit,
        var onSubjectChanged: (SubjectTruncated) -> Unit,
        var onDescriptionChanged: (String) -> Unit,
        var onCreateNewProperty: (() -> Unit)?,
        var onDeleteObject: (() -> Unit)?,
        var onUpdateObject: (() -> Unit)?,
        var onDiscardUpdate: (() -> Unit)?,
        var disabled: Boolean,
        var editMode: Boolean
    ) : RProps
}

class DeleteObjectButton : RComponent<DeleteObjectButton.Props, DeleteObjectButton.State>() {
    override fun State.init() {
        isOpen = false
    }

    override fun RBuilder.render() {
        div(classes = "object-tree-edit__header-delete-object-button") {
            Alert {
                attrs {
                    isOpen = state.isOpen
                    onConfirm = {
                        props.onSubmit?.invoke()
                        setState { isOpen = false }
                    }
                    icon = "trash"
                    intent = Intent.DANGER
                    canEscapeKeyCancel = true
                    canOutsideClickCancel = true
                    cancelButtonText = "Cancel"
                    confirmButtonText = "Delete"
                }
                div("delete-popover") {
                    h5 {
                        +"Confirm deletion"
                    }
                    p {
                        +"Are you sure you want to delete it?"
                    }
                }
            }
            Button {
                attrs {
                    this.className = listOfNotNull(className).joinToString(" ")
                    intent = Intent.DANGER
                    icon = "cross"
                    text = "Delete object".asReactElement()
                    disabled = props.onSubmit == null
                    onClick = {
                        setState {
                            isOpen = true
                        }
                    }
                }
            }

        }
    }

    class Props(var onSubmit: (() -> Unit)?) : RProps
    class State(var isOpen: Boolean) : RState
}

fun RBuilder.deleteObjectButton(onSubmit: (() -> Unit)?, className: String? = null) =
    div(classes = "object-tree-edit__header-delete-object-button") {
        Popover {
            attrs {
                content = buildElement {
                    deletePopoverWindow {
                        attrs {
                            onConfirm = onSubmit ?: {}
                        }
                    }
                }!!
            }
            Button {
                attrs {
                    this.className = listOfNotNull(className).joinToString(" ")
                    intent = Intent.DANGER
                    icon = "cross"
                    text = "Delete object".asReactElement()
                    disabled = onSubmit == null
                }
            }
        }
    }

