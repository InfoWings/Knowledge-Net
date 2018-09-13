package com.infowings.catalog.objects

import com.infowings.catalog.common.guid.BriefObjectViewResponse
import com.infowings.catalog.common.guid.BriefValueViewResponse
import com.infowings.catalog.common.toData
import com.infowings.catalog.objects.view.tree.format.valueFormat
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

data class ObjectBriefInfo(val data: BriefObjectViewResponse) : EntityBriefInfo() {

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
    }
}

data class ValueBriefInfo(val data: BriefValueViewResponse) : EntityBriefInfo() {
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
        span(classes = "entity-brief-info__value") {
            valueFormat(data.value.toData())
        }
        data.measure?.let {
            span(classes = "entity-brief-info__value-measure") {
                +it
            }
        }
    }
}