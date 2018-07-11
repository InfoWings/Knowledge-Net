package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.ObjectPropertyValueEditModel
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.propertyValue
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectPropertyValueEditLineFormat = rFunction<ObjectPropertyValueEditLineFormatProps>("ObjectPropertyValueEditLineFormat") { props ->
    div(classes = "object-tree-edit__property-value") {
        name(
            className = "property-value__property-name",
            value = props.propertyName ?: "",
            onChange = props.onPropertyNameUpdate,
            onCancel = props.onPropertyNameUpdate
        )
        span(classes = "property-value__aspect") {
            +props.aspectName
        }
        span(classes = "property-value__aspect-subject") {
            +"("
            +"Global"
            +")"
        }
        propertyValue(
            baseType = props.aspectBaseType,
            referenceBookId = props.referenceBookId,
            value = props.value,
            onChange = props.onValueUpdate
        )
        props.onSaveValue?.let {
            submitButtonComponent(it, "pt-small")
        }
    }
}

interface ObjectPropertyValueEditLineFormatProps : RProps {
    var propertyName: String?
    var aspectName: String
    var aspectBaseType: BaseType
    var referenceBookId: String?
    var value: ObjectValueData?
    var onPropertyNameUpdate: (String) -> Unit
    var onValueUpdate: (ObjectValueData) -> Unit
    var onSaveValue: (() -> Unit)?
}
