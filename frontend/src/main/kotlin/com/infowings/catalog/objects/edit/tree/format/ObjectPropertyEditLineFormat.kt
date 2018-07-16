package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.TreeAspectResponse
import com.infowings.catalog.components.buttons.newValueButtonComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.tree.inputs.ShortAspectDescriptor
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.propertyAspect
import react.RProps
import react.dom.div
import react.rFunction

val objectPropertyEditLineFormat = rFunction<ObjectPropertyEditLineFormatProps>("ObjectPropertyEditLineFormat") { props ->
    div(classes = "object-tree-edit__object-property") {
        name(
            className = "object-property__name",
            value = props.name ?: "",
            onChange = props.onNameChanged,
            onCancel = props.onNameChanged
        )
        propertyAspect(
            value = props.aspect?.let { ShortAspectDescriptor(it.id, it.name, it.subjectName) },
            onSelect = { props.onAspectChanged(TreeAspectResponse(id = it.id, name = it.name)) }
        )
        props.onConfirmCreate?.let {
            submitButtonComponent(it)
        }
        props.onAddValue?.let {
            newValueButtonComponent(it)
        }
    }
}

interface ObjectPropertyEditLineFormatProps : RProps {
    var name: String?
    var aspect: TreeAspectResponse?
    var onNameChanged: (String) -> Unit
    var onAspectChanged: (TreeAspectResponse) -> Unit
    var onConfirmCreate: (() -> Unit)?
    var onAddValue: (() -> Unit)?
}
