package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectEditViewModel
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.edit.*
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
                            onNameChanged = onNameChanged()
                            subject = props.objectTree.subject
                            onSubjectChanged = onSubjectChanged()
                            description = props.objectTree.description
                            onDescriptionChanged = onDescriptionChanged()
                            guid = props.objectTree.guid
                            onCreateNewProperty = onCreateNewProperty(editContextModel)
                            onDeleteObject = onDeleteObject(editContextModel)
                            onUpdateObject = onUpdateObject(editContextModel)
                            onDiscardUpdate = onDiscardUpdate(editContextModel)
                            disabled = !props.editMode || (editContextModel != null && editContextModel != EditExistingContextModel(props.objectTree.id))
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

    // probably there are bugs in each functions, lambda and if statement works async here
    private fun onDiscardUpdate(editContextModel: EditContextModel?): (() -> Unit)? {
        return if (editContextModel == EditExistingContextModel(props.objectTree.id)) {
            {
                props.editModel.update {
                    println("Subject 1: ${props.objectTree.subject}")
                    println("Subject 1: ${props.apiModel.name}")
                    println("Subject 1: ${props.apiModel.name}")
                    name = props.apiModel.name
                    description = props.apiModel.description
                    subject = SubjectTruncated(props.apiModel.subjectId, props.apiModel.subjectName)
                }
                props.editContext.setContext(null)
            }
        } else null
    }

    private fun onUpdateObject(editContextModel: EditContextModel?): (() -> Unit)? {
        return if (editContextModel == EditExistingContextModel(props.objectTree.id)) {
            {
                props.editModel.updateObject()
                props.editContext.setContext(null)
            }
        } else null
    }

    private fun onDeleteObject(editContextModel: EditContextModel?): (() -> Unit)? {
        return if (editContextModel == null) {
            { props.editModel.deleteObject() }
        } else null
    }

    private fun onCreateNewProperty(editContextModel: EditContextModel?): (() -> Unit)? {
        return if (editContextModel == null) {
            {
                props.editContext.setContext(EditNewChildContextModel)
                props.editModel.update {
                    properties.add(ObjectPropertyEditModel())
                }
            }
        } else null
    }

    private fun onDescriptionChanged(): (String) -> Unit {
        return if (props.editContext.currentContext == null) {
            editObjectDescription()
        } else {
            {
                props.editModel.update {
                    description = it
                }
            }
        }
    }

    private fun onSubjectChanged(): (SubjectTruncated) -> Unit {
        return if (props.editContext.currentContext == null) {
            {
                println("Subject 2: ${props.objectTree.subject}")
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
    }

    private fun onNameChanged(): (String) -> Unit {
        return if (props.editContext.currentContext == null) {
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

