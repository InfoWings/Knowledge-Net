package com.infowings.catalog.aspects.treeview.view

import com.infowings.catalog.utils.ripIcon
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div
import react.dom.span
import kotlin.js.Date

fun RBuilder.aspectLabel(
    className: String?,
    aspectName: String,
    aspectMeasure: String,
    aspectDomain: String,
    aspectBaseType: String,
    aspectRefBookName: String,
    aspectSubjectName: String,
    isSubjectDeleted: Boolean,
    lastChangedTimestamp: Long?,
    onClick: () -> Unit
) = div(classes = "aspect-tree-view--label${className?.let { " $it" } ?: ""}") {
    attrs {
        onClickFunction = {
            it.stopPropagation()
            it.preventDefault()
            onClick()
        }
    }
    span(classes = "text-bold") {
        +aspectName
    }
    +":"
    span(classes = "text-grey") {
        +aspectMeasure
    }
    +":"
    span(classes = "text-grey") {
        +if (aspectRefBookName.isNotEmpty()) aspectRefBookName else aspectDomain
    }
    +":"
    span(classes = "text-grey") {
        +aspectBaseType
    }
    +"( Subject: "
    span(classes = "text-grey") {
        +aspectSubjectName
    }
    if (isSubjectDeleted) {
        ripIcon("aspect-tree-view--rip-icon") {}
    }
    +" )"
    lastChangedTimestamp?.let {
        span(classes = "text-grey") {
            val date = Date(it * 1000)
            +"${date.getDateWithPadding()}.${date.getMonthWithPadding()} at ${date.getHoursWithPadding()}:${date.getMinutesWithPadding()}"
        }
    }
}

private fun Date.getDateWithPadding() = this.getDate().toString().padStart(2, '0')
private fun Date.getMonthWithPadding() = this.getMonth().toString().padStart(2, '0')
private fun Date.getHoursWithPadding() = this.getHours().toString().padStart(2, '0')
private fun Date.getMinutesWithPadding() = this.getMinutes().toString().padStart(2, '0')

fun RBuilder.placeholderAspectLabel(className: String?) =
    div(classes = "aspect-tree-view--label${className?.let { " $it" } ?: ""}") {
        span(classes = "aspect-tree-view--empty") {
            +"(Enter New Aspect)"
        }
    }