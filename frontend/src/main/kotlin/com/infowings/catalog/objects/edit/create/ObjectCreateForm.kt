package com.infowings.catalog.objects.edit.create

import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.SubjectTruncated
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.objectSubject
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectCreateForm = rFunction<ObjectCreateFormProps>("ObjectCreateForm") { props ->
    div("object-create-form") {
        name(
            value = props.name,
            onChange = props.onNameUpdate,
            onCancel = props.onNameUpdate
        )
        span(classes = "object-create-form__label") {
            +"( Subject:"
        }
        objectSubject(
            value = props.subject,
            onSelect = props.onSubjectUpdate
        )
        span(classes = "object-create-form__label") {
            +")"
        }
        props.onConfirm?.let {
            submitButtonComponent(it)
        }
    }
}

interface ObjectCreateFormProps : RProps {
    var name: String
    var subject: SubjectTruncated?
    var onNameUpdate: (String) -> Unit
    var onSubjectUpdate: (SubjectTruncated) -> Unit
    var onConfirm: (() -> Unit)?
}
