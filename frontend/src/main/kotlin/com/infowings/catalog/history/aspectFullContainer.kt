package com.infowings.catalog.history

import com.infowings.catalog.aspects.treeview.view.aspectLabel
import com.infowings.catalog.aspects.treeview.view.propertyLabel
import com.infowings.catalog.common.AspectDataView
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.utils.ripIcon
import react.*
import react.dom.div

class AspectFullContainer : RComponent<AspectFullContainer.Props, RState>() {

    companion object {
        init {
            kotlinext.js.require("styles/aspect-tree-view.scss")
        }
    }

    override fun RBuilder.render() {
        div("history-aspect-view") {
            treeNode {
                attrs {
                    expanded = true
                    treeNodeContent = buildElement {
                        aspectLabel(
                            className = null,
                            aspectName = props.view.aspectData.name ?: "",
                            aspectMeasure = props.view.aspectData.measure ?: "",
                            aspectDomain = props.view.aspectData.domain ?: "",
                            aspectBaseType = props.view.aspectData.baseType ?: "",
                            aspectSubjectName = props.view.aspectData.subject?.name ?: "Global",
                            isSubjectDeleted = props.view.aspectData.subject?.deleted ?: false,
                            onClick = { }
                        )
                    }!!
                }
                val propMap = props.view.related.map { it.id to it }.toMap()
                if (propMap.isNotEmpty()) {
                    div("history_properties") {
                        props.view.aspectData.properties.forEach {
                            val aspect = propMap[it.aspectId]
                            div {
                                propertyLabel(
                                    className = null,
                                    aspectPropertyName = it.name,
                                    aspectPropertyCardinality = it.cardinality,
                                    aspectName = aspect?.name ?: "",
                                    aspectMeasure = aspect?.measure ?: "",
                                    aspectDomain = aspect?.domain ?: "",
                                    aspectBaseType = aspect?.baseType ?: "",
                                    aspectSubjectName = aspect?.subject?.name ?: "Global",
                                    isSubjectDeleted = aspect?.subject?.deleted ?: false,
                                    onClick = {}
                                )
                                aspect?.let {
                                    if (it.deleted) {
                                        ripIcon("aspect-tree-view--rip-icon") {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var onExit: () -> Unit
        var view: AspectDataView
    }
}

fun RBuilder.aspectFullContainer(handler: RHandler<AspectFullContainer.Props>) =
    child(AspectFullContainer::class, handler)