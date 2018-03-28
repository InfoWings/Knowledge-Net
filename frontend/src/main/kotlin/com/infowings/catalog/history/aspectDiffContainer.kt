package com.infowings.catalog.history

import com.infowings.catalog.aspects.editconsole.popup.popup
import com.infowings.catalog.common.Delta
import react.*
import react.dom.div
import react.dom.li
import react.dom.span
import react.dom.ul

class AspectDiffContainer : RComponent<AspectDiffContainer.Props, RState>() {

    override fun RBuilder.render() {
        div("aspect-popup-container") {
            popup {
                attrs.closePopup = { props.onExit() }

                ul("list-group") {
                    props.changes.forEach {
                        li("diff-group list-group-item diff-${it.color}") {
                            span("field") {
                                +it.field
                            }
                            span {
                                +it.line
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var onExit: () -> Unit
        var changes: List<Delta>
    }
}

fun RBuilder.aspectDiffContainer(handler: RHandler<AspectDiffContainer.Props>) =
    child(AspectDiffContainer::class, handler)

private val Delta.color
    get() = when {
        before == null -> "green"
        after == null -> "red"
        else -> ""
    }

private val Delta.line: String
    get() = when {
        before == null -> after!!
        after == null -> before!!
        else -> "$before → $after"
    }