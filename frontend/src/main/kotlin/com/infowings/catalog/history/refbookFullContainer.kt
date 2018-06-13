package com.infowings.catalog.history

import com.infowings.catalog.common.history.refbook.RefBookHistoryData
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.history.refbook.refBookItemLabel
import com.infowings.catalog.history.refbook.refBookLabel
import react.*
import react.dom.div

class RefBookFullContainer : RComponent<RefBookFullContainer.Props, RState>() {

    companion object {
        init {
            kotlinext.js.require("styles/aspect-tree-view.scss")
        }
    }

    override fun RBuilder.render() {
        div("history-subject-view") {
            treeNode {
                attrs {
                    expanded = true
                    treeNodeContent = buildElement {
                        val item = props.view.item
                        if (item == null)
                            refBookLabel(
                                className = null,
                                name = props.view.header.name,
                                description = props.view.header.description ?: "---",
                                aspectName = props.view.header.aspectName,
                                onClick = { }
                            )
                        else {
                            refBookItemLabel(
                                className = null,
                                name = item.name,
                                description = item.description ?: "---",
                                onClick = { }
                            )
                        }
                    }!!
                }
            }
        }
    }

    interface Props : RProps {
        var onExit: () -> Unit
        var view: RefBookHistoryData.Companion.BriefState
    }
}

fun RBuilder.refbookFullContainer(handler: RHandler<RefBookFullContainer.Props>) =
    child(RefBookFullContainer::class, handler)