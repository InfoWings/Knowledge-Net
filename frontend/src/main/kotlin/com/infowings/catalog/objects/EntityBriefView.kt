package com.infowings.catalog.objects

import com.infowings.catalog.common.guid.BriefObjectView
import com.infowings.catalog.common.guid.BriefObjectViewResponse
import com.infowings.catalog.common.guid.BriefValueViewResponse
import com.infowings.catalog.common.toData
import com.infowings.catalog.objects.view.tree.format.valueFormat
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.wrappers.History
import com.infowings.catalog.wrappers.reactRouter
import react.RBuilder
import react.dom.span

sealed class EntityBriefInfo {

    companion object {
        init {
            kotlinext.js.require("styles/entity-brief-view.scss")
        }
    }

    fun render(builder: RBuilder) = builder.render()
    abstract fun RBuilder.render()
}

const val OBJECT_ENTITY_TYPE_PLACEHOLDER = "Object:"
const val VALUE_ENTITY_TYPE_PLACEHOLDER = "Value:"

data class ObjectBriefInfo(val data: BriefObjectView, val history: History, val editMode: Boolean) : EntityBriefInfo() {

    override fun RBuilder.render() {
        span(classes = "entity-brief-info__entity-type") {
            +OBJECT_ENTITY_TYPE_PLACEHOLDER
        }
        span(classes = "entity-brief-info__object-name") {
            +data.name
        }
        span(classes = "entity-brief-info__object-subject") {
            +"(${data.subjectName ?: "Global"})"
        }

        if (!editMode) {
            reactRouter.Link {
                attrs {
                    className = "object-line__edit-link pt-button pt-intent-primary pt-minimal pt-icon-step-forward pt-small"
                    role = "button"
                    to = "/objects/view/${encodeURIComponent(data.id)}"
                }
            }
        }
    }
}

data class ValueBriefInfo(val data: BriefValueViewResponse, val history: History, val editMode: Boolean) : EntityBriefInfo() {
    override fun RBuilder.render() {
        span(classes = "entity-brief-info__entity-type") {
            +VALUE_ENTITY_TYPE_PLACEHOLDER
        }

        data.propertyName?.let {
            span(classes = "entity-brief-info__value-property-name") {
                +it
            }
        }
        span(classes = "entity-brief-info__value-aspect-name") {
            +data.aspectName
        }
        data.measure?.let {
            span(classes = "entity-brief-info__value-measure") {
                +it
            }
        }

        span(classes = "entity-brief-info__value") {
            if (!editMode) {
                reactRouter.Link {
                    attrs {
                        className = "object-line__edit-link pt-button pt-intent-primary pt-minimal pt-icon-step-forward pt-small"
                        role = "button"
                        to = "/objects/viewm/${encodeURIComponent(data.objectId)}/${encodeURIComponent(data.guid?:"???")}"
                    }
                }
            }
            valueFormat(data.value.toData(), history)
        }
    }
}