package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectEditModel
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.edit.ObjectTreeEditModel
import com.infowings.catalog.objects.edit.tree.format.objectEditLineFormat
import react.*
import react.dom.div

class ObjectTreeEdit : RComponent<ObjectTreeEdit.Props, RState>() {

    companion object {
        init {
            kotlinext.js.require("styles/object-tree-edit.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "object-tree-edit") {
            controlledTreeNode {
                attrs {
                    expanded = props.objectTree.expanded
                    onExpanded = {
                        props.editModel.onUpdate {
                            expanded = it
                        }
                    }
                    treeNodeContent = buildElement {
                        objectEditLineFormat {
                            attrs {
                                name = props.objectTree.name
                                onNameChanged = {
                                    props.editModel.onUpdate {
                                        name = it
                                    }
                                }
                                subject = props.objectTree.subject
                                onSubjectChanged = {
                                    props.editModel.onUpdate {
                                        subject = it
                                    }
                                }
                                description = props.objectTree.description
                                onDescriptionChanged = {
                                    props.editModel.onUpdate {
                                        description = it
                                    }
                                }
                                canCreateNewProperty = props.objectTree.properties.isEmpty() || props.objectTree.properties.last().id != null
                                onCreateNewProperty = {
                                    props.editModel.onUpdate {
                                        properties.add(ObjectPropertyEditModel())
                                    }
                                }
                            }
                        }
                    }!!
                }
                objectPropertiesEditList(
                    properties = props.objectTree.properties,
                    onCreateProperty = props.editModel::onCreateProperty,
                    updater = { index, block ->
                        props.editModel.onUpdate {
                            properties[index].block()
                        }
                    }
                )
            }
        }
    }

    interface Props : RProps {
        var editModel: ObjectTreeEditModel
        var objectTree: ObjectEditModel
    }
}

fun RBuilder.objectEditTree(block: RHandler<ObjectTreeEdit.Props>) = child(ObjectTreeEdit::class, block)

