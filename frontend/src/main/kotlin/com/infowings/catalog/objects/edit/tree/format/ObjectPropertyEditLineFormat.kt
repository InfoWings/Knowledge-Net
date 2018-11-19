package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.AspectHint
import com.infowings.catalog.common.AspectHintSource
import com.infowings.catalog.common.AspectTree
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.newValueButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.propertyAspect
import react.RProps
import react.dom.div
import react.rFunction

fun AspectTree.toHint() =
    AspectHint(
        name = name,
        description = "",
        source = AspectHintSource.ASPECT_NAME.toString(),
        refBookItem = null,
        refBookItemDesc = null,
        propertyName = null,
        propertyDesc = null,
        subAspectName = null,
        aspectName = null,
        subjectName = name + " " + subjectName,
        id = id,
        guid = ""
    )


val objectPropertyEditLineFormat = rFunction<ObjectPropertyEditLineFormatProps>("ObjectPropertyEditLineFormat") { props ->
    div(classes = "object-tree-edit__object-property") {
        name(
            className = "object-property__name",
            value = props.name ?: "",
            onChange = props.onNameChanged,
            onCancel = props.onNameChanged,
            disabled = props.disabled
        )
        propertyAspect(
            value = props.aspect?.let { it.toHint() },
            onSelect = { props.onAspectChanged(AspectTree(id = it.id, name = it.name, subjectName = it.subjectName?.drop(it.name.length))) },
            disabled = props.disabled || props.onAddValue != null // If onAddValue exists, it means the property has id.
            // API does not allow editing aspect, so it is better to just disable the option.
        )

        props.onDescriptionChanged?.let {
            if (props.disabled) {
                descriptionComponent(
                    className = "object-input-description",
                    description = props.description
                )
            } else {
                descriptionComponent(
                    className = "object-input-description",
                    description = props.description,
                    onNewDescriptionConfirmed = it,
                    onEditStarted = null
                )
            }
        }
        props.onConfirmCreate?.let {
            submitButtonComponent(it)
        }
        props.onCancel?.let {
            cancelButtonComponent(it)
        }
        props.onRemoveProperty?.let {
            minusButtonComponent(it, true)
        }
        props.onAddValue?.let {
            if (props.editMode) {
                newValueButtonComponent(it)
            }
        }
        props.onCancelProperty?.let {
            cancelButtonComponent(it)
        }
    }
}

interface ObjectPropertyEditLineFormatProps : RProps {
    var name: String?
    var aspect: AspectTree?
    var description: String?
    var onNameChanged: (String) -> Unit
    var onAspectChanged: (AspectTree) -> Unit
    var onDescriptionChanged: ((String) -> Unit)?
    var onConfirmCreate: (() -> Unit)?
    var onCancel: (() -> Unit)?
    var onCancelProperty: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onRemoveProperty: (() -> Unit)?
    var disabled: Boolean
    var editMode: Boolean
}
