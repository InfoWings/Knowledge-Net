package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.common.TreeAspectResponse
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.tree.inputs.ShortAspectDescriptor
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.propertyAspect
import com.infowings.catalog.objects.edit.tree.inputs.propertyCardinality
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
            value = props.aspect?.let { ShortAspectDescriptor(it.id, it.name, null) },
            onSelect = { props.onAspectChanged(TreeAspectResponse(id = it.id, name = it.name)) }
        )
        propertyCardinality(
            value = props.cardinality,
            onChange = props.onCardinalityChanged
        )
        props.onConfirmCreate?.let {
            submitButtonComponent(it)
        }
    }
}

interface ObjectPropertyEditLineFormatProps : RProps {
    var name: String?
    var cardinality: PropertyCardinality?
    var aspect: TreeAspectResponse?
    var onNameChanged: (String) -> Unit
    var onCardinalityChanged: (PropertyCardinality) -> Unit
    var onAspectChanged: (TreeAspectResponse) -> Unit
    var onConfirmCreate: (() -> Unit)?
}
