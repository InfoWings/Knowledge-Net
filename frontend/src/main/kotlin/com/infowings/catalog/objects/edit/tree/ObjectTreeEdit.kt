package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectEditViewModel
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.edit.*
import com.infowings.catalog.objects.edit.tree.format.objectEditLineFormat
import react.*
import react.dom.div

class ObjectTreeEdit : RComponent<ObjectTreeEdit.Props, RState>() {

    companion object {
        init {
            kotlinext.js.require("styles/object-tree-edit.scss")
            kotlinext.js.require("styles/delete-button.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "object-tree-edit") {
            controlledTreeNode {
                attrs {
                    expanded = props.objectTree.expanded
                    onExpanded = {
                        props.editModel.update {
                            expanded = it
                        }
                    }
                    treeNodeContent = buildElement {
                        objectEditLineFormat {
                            attrs {
                                val editContextModel = props.editContext.currentContext
                                name = props.objectTree.name
                                onNameChanged = if (props.editContext.currentContext == null) {
                                    {
                                        props.editContext.setContext(EditExistingContextModel(props.objectTree.id))
                                        props.editModel.update {
                                            name = it
                                        }
                                    }
                                } else {
                                    {
                                        props.editModel.update {
                                            name = it
                                        }
                                    }
                                }
                                subject = props.objectTree.subject
                                onSubjectChanged = if (props.editContext.currentContext == null) {
                                    {
                                        props.editContext.setContext(EditExistingContextModel(props.objectTree.id))
                                        props.editModel.update {
                                            subject = it
                                        }
                                    }
                                } else {
                                    {
                                        props.editModel.update {
                                            subject = it
                                        }
                                    }
                                }
                                description = props.objectTree.description
                                onDescriptionChanged = if (props.editContext.currentContext == null) {
                                    {
                                        props.editContext.setContext(EditExistingContextModel(props.objectTree.id))
                                        props.editModel.update {
                                            description = it
                                        }
                                    }
                                } else {
                                    {
                                        props.editModel.update {
                                            description = it
                                        }
                                    }
                                }
                                guid = props.objectTree.guid
                                onCreateNewProperty = if (editContextModel == null) {
                                    {
                                        props.editContext.setContext(EditNewChildContextModel)
                                        props.editModel.update {
                                            properties.add(ObjectPropertyEditModel())
                                        }
                                    }
                                } else null
                                onDeleteObject = if (editContextModel == null) {
                                    { props.editModel.deleteObject() }
                                } else null
                                onUpdateObject = if (editContextModel == EditExistingContextModel(props.objectTree.id)) {
                                    {
                                        props.editModel.updateObject()
                                        props.editContext.setContext(null)
                                    }
                                } else null
                                onDiscardUpdate = if (editContextModel == EditExistingContextModel(props.objectTree.id)) {
                                    {
                                        props.editModel.update {
                                            name = props.apiModel.name
                                            description = props.apiModel.description
                                        }
                                        props.editContext.setContext(null)
                                    }
                                } else null
                                disabled = !props.editMode ||
                                        (editContextModel != null && editContextModel != EditExistingContextModel(props.objectTree.id))
                                editMode = props.editMode
                            }
                        }
                    }!!
                }
                objectPropertiesEditList(
                    editContext = props.editContext,
                    properties = props.objectTree.properties,
                    editModel = props.editModel,
                    apiModelProperties = props.apiModel.properties,
                    updater = { index, block ->
                        props.editModel.update {
                            properties[index].block()
                        }
                    },
                    remover = { index ->
                        props.editModel.update {
                            properties.removeAt(index)
                        }
                    },
                    newEditMode = props.editMode,
                    newHighlightedGuid = props.highlightedGuid
                )
            }
        }
    }

    interface Props : RProps {
        var editModel: ObjectTreeEditModel
        var objectTree: ObjectEditViewModel
        var apiModel: ObjectEditDetailsResponse
        var editContext: EditContext
        var editMode: Boolean
        var highlightedGuid: String?
    }
}

fun RBuilder.objectEditTree(block: RHandler<ObjectTreeEdit.Props>) = child(ObjectTreeEdit::class, block)

