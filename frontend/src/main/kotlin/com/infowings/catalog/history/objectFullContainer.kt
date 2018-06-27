package com.infowings.catalog.history

import com.infowings.catalog.common.history.objekt.ObjectHistoryData
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.history.objekt.objectLabel
import com.infowings.catalog.history.objekt.objectPropertyLabel
import com.infowings.catalog.history.objekt.objectValueLabel
import react.*
import react.dom.div

class ObjectFullContainer : RComponent<ObjectFullContainer.Props, RState>() {

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
                        val property = props.view.property
                        val propertyValue = props.view.value
                        when {
                            (property == null) ->
                                objectLabel(
                                    className = null,
                                    name = props.view.objekt.name,
                                    description = props.view.objekt.description ?: "---",
                                    subjectName = props.view.objekt.subjectName,
                                    onClick = { }
                                )
                            (propertyValue == null) ->
                                objectPropertyLabel(
                                    className = null,
                                    name = property.name,
                                    cardinality = property.cardinality,
                                    aspectName = property.aspectName,
                                    onClick = { }
                                )
                            else ->
                                objectValueLabel(
                                    className = null,
                                    repr = propertyValue.repr,
                                    typeTag = propertyValue.typeTag,
                                    aspectPropertyName = propertyValue.aspectPropertyName ?: "NONE",
                                    measureName = propertyValue.measureName ?: "NONE",
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
        var view: ObjectHistoryData.Companion.BriefState
    }
}

fun RBuilder.objectFullContainer(handler: RHandler<ObjectFullContainer.Props>) =
    child(ObjectFullContainer::class, handler)