package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectEditViewModel
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.edit.EditContext
import com.infowings.catalog.objects.edit.EditExistingContextModel
import com.infowings.catalog.objects.edit.EditNewChildContextModel
import com.infowings.catalog.objects.edit.ObjectTreeEditModel
import com.infowings.catalog.objects.edit.tree.format.objectEditLineFormat
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import kotlinx.coroutines.Job
import react.*
import react.dom.div

class ObjectTreeEdit : RComponent<ObjectTreeEdit.Props, RState>(), JobCoroutineScope by JobSimpleCoroutineScope() {
    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

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
                    expanded = true
                    hideExpandButton = true
                    treeNodeContent = buildElement {
                        objectEditLineFormat {
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
                                editObjectDescription()
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

    private fun editObjectDescription(): (String) -> Unit {
        return {
            props.editContext.setContext(EditExistingContextModel(props.objectTree.id))
            props.editModel.update { description = it }
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

