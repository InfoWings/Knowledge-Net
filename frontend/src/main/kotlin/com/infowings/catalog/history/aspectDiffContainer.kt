package com.infowings.catalog.history

import com.infowings.catalog.common.FieldDelta
import react.*
import react.dom.div
import react.dom.span

class AspectDiffContainer : RComponent<AspectDiffContainer.Props, RState>() {

    override fun RBuilder.render() {
        div("history-diff") {
            props.changes.forEach {
                div("history-diff--item history-diff--item__${it.color}") {
                    span {
                        +it.fieldName
                    }
                    span {
                        +it.line
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var changes: List<FieldDelta>
    }
}

fun RBuilder.aspectDiffContainer(handler: RHandler<AspectDiffContainer.Props>) =
    child(AspectDiffContainer::class, handler)

private val FieldDelta.color
    get() = when {
        before.isNullOrEmpty() -> "green"
        after.isNullOrEmpty() -> "red"
        else -> ""
    }

private val FieldDelta.line: String
    get() = when {
        before.isNullOrEmpty() -> after!!
        after.isNullOrEmpty() -> before!!
        else -> "$before → $after"
    }