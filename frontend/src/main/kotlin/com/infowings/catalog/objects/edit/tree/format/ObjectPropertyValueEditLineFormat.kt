package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.plusButtonComponent
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
            +(props.subjectName ?: "Global")
            +")"
        }
        if (props.value != ObjectValueData.NullValue) {
            propertyValue(
                baseType = props.aspectBaseType,
                referenceBookId = props.referenceBookId,
                value = props.value,
                onChange = props.onValueUpdate
            )
        }
        props.onAddValue?.let {
            plusButtonComponent(it, "pt-small")
        }
        props.onRemoveValue?.let {
            minusButtonComponent(it, "pt-small")
        }
        props.onSaveValue?.let {
            submitButtonComponent(it, "pt-small")
        }
        props.onCancelValue?.let {
            cancelButtonComponent(it, "pt-small")
        }

    }
}

interface ObjectPropertyValueEditLineFormatProps : RProps {
    var propertyName: String?
    var aspectName: String
    var aspectBaseType: BaseType
    var subjectName: String?
    var referenceBookId: String?
    var value: ObjectValueData?
    var onPropertyNameUpdate: (String) -> Unit
    var onValueUpdate: (ObjectValueData) -> Unit
    var onSaveValue: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onCancelValue: (() -> Unit)?
    var onRemoveValue: (() -> Unit)?
}
