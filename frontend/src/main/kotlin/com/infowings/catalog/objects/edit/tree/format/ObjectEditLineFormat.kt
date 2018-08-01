package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.components.additem.addPropertyButton
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.SubjectTruncated
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.objectSubject
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectEditLineFormat = rFunction<ObjectEditLineFormatProps>("ObjectEditLineFormat") { props ->
    div(classes = "object-tree-edit__object") {
        name(
            value = props.name,
            onChange = props.onNameChanged,
            onCancel = props.onNameChanged
        )
        span(classes = "object-tree-edit__label") {
            +"( Subject:"
        }
        objectSubject(
            value = props.subject,
            onSelect = props.onSubjectChanged
        )
        span(classes = "object-tree-edit__label") {
            +")"
        }
        descriptionComponent(
            className = "object-input-description",
            description = props.description,
            onNewDescriptionConfirmed = props.onDescriptionChanged,
            onEditStarted = null
        )
        props.onUpdateObject?.let {
            submitButtonComponent(it)
        }
        props.onDiscardUpdate?.let {
            cancelButtonComponent(it)
        }
        if (props.canCreateNewProperty) {
            addPropertyButton(onClick = props.onCreateNewProperty)
        }
        minusButtonComponent(props.onDeleteObject, true)
    }
}

interface ObjectEditLineFormatProps : RProps {
    var name: String
    var subject: SubjectTruncated
    var description: String?
    var onNameChanged: (String) -> Unit
    var onSubjectChanged: (SubjectTruncated) -> Unit
    var onDescriptionChanged: (String) -> Unit
    var canCreateNewProperty: Boolean
    var onCreateNewProperty: () -> Unit
    var onDeleteObject: () -> Unit
    var onUpdateObject: (() -> Unit)?
    var onDiscardUpdate: (() -> Unit)?
}
