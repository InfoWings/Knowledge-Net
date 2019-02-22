package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.components.buttons.cancelButtonComponent
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

class ObjectEditHeader : RComponent<ObjectEditHeader.Props, RState>() {
    override fun RBuilder.render() {
        Callout {
            attrs {
                intent = Intent.PRIMARY
                icon = null
            }
            div {
                h2 {
                    editableText {
                        initialText = props.name
                        onChange = props.onNameChanged
                        placeholder = "Object name"
                        disabled = props.disabled
                    }
                    div(classes = "object-tree-edit__header-copy-guid-button") {
                        copyGuidButton(props.guid)
                    }
                }
                editableText {
                    multiline = true
                    initialText = props.description ?: ""
                    onChange = props.onDescriptionChanged
                    placeholder = "Add description"
                    disabled = props.disabled

                }
                div {
                    div(classes = "object-tree-edit__object") {
                        span(classes = "object-tree-edit__label") {
                            +"Subject:"
                        }
                        println("Subject: ${props.subject}")
                        objectSubject(
                            value = props.subject,
                            onSelect = props.onSubjectChanged,
                            disabled = props.disabled
                        )
                        props.onUpdateObject?.let {
                            submitButtonComponent(it)
                        }
                        props.onDiscardUpdate?.let {
                            cancelButtonComponent(it)
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

fun RBuilder.editableText(builder: EditableTextStateful.Props.() -> Unit) =
    buildWithProperties<EditableTextStateful.Props, EditableTextStateful>(builder)

class EditableTextStateful(props: Props) : RComponent<EditableTextStateful.Props, EditableTextStateful.State>(props) {
    override fun RBuilder.render() {
        EditableText {
            attrs {
                multiline = props.multiline
                maxLines = 3
                value = state.value
                onChange = {
                    props.onChange(it)
                    setState { value = it }
                }
                props.placeholder?.let {
                    placeholder = it
                }
                disabled = props.disabled

            }
        }
    }

    override fun State.init(props: Props) {
        value = props.initialText
    }

    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
        if (props.initialText != prevProps.initialText) {
            setState { value = props.initialText }
        }
    }

    class State(
        var value: String
    ) : RState

    class Props(
        var initialText: String,
        var multiline: Boolean,
        var onChange: (String) -> Unit,
        var placeholder: String?,
        var disabled: Boolean
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

