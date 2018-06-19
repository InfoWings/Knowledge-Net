package com.infowings.catalog.history

import com.infowings.catalog.common.SnapshotData
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.history.subject.subjectLabel
import react.*
import react.dom.div

class SubjectFullContainer : RComponent<SubjectFullContainer.Props, RState>() {

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
                        subjectLabel(
                            className = null,
                            name = props.view.data["name"] ?: "",
                            description = props.view.data["description"] ?: "",
                            onClick = { }
                        )
                    }!!
                }
            }
        }
    }

    interface Props : RProps {
        var onExit: () -> Unit
        var view: SnapshotData
    }
}

fun RBuilder.subjectFullContainer(handler: RHandler<SubjectFullContainer.Props>) =
    child(SubjectFullContainer::class, handler)