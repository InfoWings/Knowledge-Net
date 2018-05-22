package com.infowings.catalog.history

import com.infowings.catalog.common.Delta
import react.*
import react.dom.div
import react.dom.span

class AspectDiffContainer : RComponent<AspectDiffContainer.Props, RState>() {

    override fun RBuilder.render() {
        div("history-diff") {
            props.changes.forEach {
                div("history-diff--item history-diff--item__${it.color}") {
                    span {
                        +it.field
                    }
                    span {
                        +it.line
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var changes: List<Delta>
    }
}

fun RBuilder.aspectDiffContainer(handler: RHandler<AspectDiffContainer.Props>) =
    child(AspectDiffContainer::class, handler)

private val Delta.color
    get() = when {
        before.isNullOrEmpty() -> "green"
        after.isNullOrEmpty() -> "red"
        else -> ""
    }

private val Delta.line: String
    get() = when {
        before.isNullOrEmpty() -> after!!
        after.isNullOrEmpty() -> before!!
        else -> "$before → $after"
    }