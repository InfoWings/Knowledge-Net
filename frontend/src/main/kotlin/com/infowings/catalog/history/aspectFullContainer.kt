package com.infowings.catalog.history

import com.infowings.catalog.aspects.treeview.view.aspectLabel
import com.infowings.catalog.aspects.treeview.view.propertyLabel
import com.infowings.catalog.common.AspectDataView
import com.infowings.catalog.components.description.descriptionComponent
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
                        div("aspect-tree-history") {
                            aspectLabel(
                                className = null,
                                aspectName = props.view.aspectData.name ?: "",
                                aspectMeasure = props.view.aspectData.measure ?: "",
                                aspectDomain = props.view.aspectData.domain ?: "",
                                aspectBaseType = props.view.aspectData.baseType ?: "",
                                aspectRefBookName = props.view.aspectData.refBookName ?: "",
                                aspectSubjectName = props.view.aspectData.subject?.name ?: "Global",
                                isSubjectDeleted = props.view.aspectData.subject?.deleted ?: false,
                                lastChangedTimestamp = props.view.aspectData.lastChangeTimestamp,
                                onClick = { }
                            )
                            descriptionComponent(
                                className = "aspect-tree-view--description-icon",
                                description = props.view.aspectData.description
                            )
                        }
                    }!!
                }
                val propMap = props.view.related.map { it.id to it }.toMap()
                if (propMap.isNotEmpty()) {
                    div("history_properties") {
                        props.view.aspectData.properties.forEach {
                            val aspect = propMap[it.aspectId]
                            div("aspect-tree-history") {
                                propertyLabel(
                                    className = null,
                                    aspectPropertyName = it.name,
                                    aspectPropertyCardinality = it.cardinality,
                                    aspectName = aspect?.name ?: "",
                                    aspectMeasure = aspect?.measure ?: "",
                                    aspectDomain = aspect?.domain ?: "",
                                    aspectRefBookName = aspect?.refBookName ?: "",
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
                                descriptionComponent(
                                    className = "aspect-tree-view--description-icon",
                                    description = it.description
                                )
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