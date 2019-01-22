package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.errors.showError
import com.infowings.catalog.objects.edit.create.objectCreateForm
import com.infowings.catalog.utils.ApiException
import react.*

data class SubjectTruncated(
    val id: String,
    val name: String
)

class ObjectCreateModelComponent : RComponent<ObjectCreateModelComponent.Props, ObjectCreateModelComponent.State>() {

    companion object {
        init {
            kotlinext.js.require("styles/object-create-form.scss")
        }
    }

    override fun State.init() {
        name = ""
        subject = null
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        nextProps.lastApiError?.let {
            showError(it)
        }
    }

    private fun updateName(name: String) = setState {
        this.name = name
    }

    private fun updateSubject(subject: SubjectTruncated) = setState {
        this.subject = subject
    }

    private fun confirmCreateObject() {
        val trimmedName = state.name.trim()
        val subjectId = state.subject?.id ?: error("Can not create object without aspect")
        props.api.submitObject(ObjectCreateRequest(trimmedName, null, subjectId))
    }

    override fun RBuilder.render() {
        objectCreateForm {
            attrs {
                name = state.name
                subject = state.subject
                onNameUpdate = this@ObjectCreateModelComponent::updateName
                onSubjectUpdate = this@ObjectCreateModelComponent::updateSubject
                onConfirm = if (state.name.trim() != "" && state.subject != null) {
                    { confirmCreateObject() }
                } else null
            }
        }
    }

    interface Props : RProps {
        var api: ObjectCreateApiModel
        var lastApiError: ApiException?
    }

    interface State : RState {
        var name: String
        var subject: SubjectTruncated?
    }

}

fun RBuilder.objectCreateModel(block: RHandler<ObjectCreateModelComponent.Props>) = child(ObjectCreateModelComponent::class, block)

