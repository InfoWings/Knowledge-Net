package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.aspects.AspectsModel
import com.infowings.catalog.aspects.treeview.view.newAspectButton
import com.infowings.catalog.aspects.treeview.view.placeholderAspectLabel
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.wrappers.blueprint.Alert
import kotlinext.js.require
import react.*
import react.dom.div
import react.dom.h3
import react.dom.p


/**
 * View Component. Draws List of [treeNode] for each [AspectData] in [AspectTreeView.Props.aspects] list
 */
class AspectTreeView : RComponent<AspectTreeView.Props, AspectTreeView.State>() {

    companion object {
        init {
            require("styles/aspect-tree-view.scss")
        }
    }

    override fun State.init() {
        unsafeSelection = false
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view") {
            props.aspects.filter { !it.deleted }.map { aspect ->
                child(AspectNodeExpandedStateWrapper::class) {
                    attrs {
                        key = aspect.id ?: ""
                        this.aspect = aspect
                        selectedAspectId = props.selectedAspectId
                        selectedPropertyIndex = props.selectedPropertyIndex
                        aspectsModel = props.aspectsModel
                        aspectContext = props.aspectContext
                    }
                }
            }
            when (props.selectedAspectId) {
                null -> div(classes = "aspect-tree-view--new-aspect") {
                    placeholderAspectLabel("aspect-tree-view--label__selected")
                }
                else -> newAspectButton {
                    props.aspectsModel.selectAspect(null)
                }
            }
        }
        Alert {
            attrs {
                isOpen = state.unsafeSelection
                onConfirm = {
                    it.preventDefault()
                    it.stopPropagation()
                    setState {
                        unsafeSelection = false
                    }
                }
            }
            div {
                h3 { +"Unsaved changes." }
                p { +"You was editing aspect, but don't save and don't reject it" }
                p { +"Click to one of corresponding buttons in the bottom of the page" }
            }
        }
    }

    interface State : RState {
        var unsafeSelection: Boolean
    }

    interface Props : RProps {
        var aspects: List<AspectData>
        var selectedAspectId: String?
        var selectedPropertyIndex: Int?
        var aspectContext: Map<String, AspectData>
        var aspectsModel: AspectsModel
    }
}

/**
 * Wrapper component that incapsulates and manages state of expanded aspect tree.
 */
class AspectNodeExpandedStateWrapper :
    RComponent<AspectNodeExpandedStateWrapper.Props, AspectNodeExpandedStateWrapper.State>() {

    override fun State.init() {
        expandedSubtree = false
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        setState {
            expandedSubtree = false
        }
    }

    private fun handleExpandAllStructure() {
        setState {
            expandedSubtree = true
        }
    }

    private fun handleExpandStateChanged() {
        setState {
            expandedSubtree = false
        }
    }

    override fun RBuilder.render() {
        treeNode {
            attrs {
                key = props.aspect.id ?: ""
                className = "aspect-tree-view--aspect-node"
                expanded = props.aspect.id == props.selectedAspectId && props.selectedPropertyIndex != null
                onExpanded = { handleExpandStateChanged() }
                treeNodeContent = buildElement {
                    aspectNode {
                        attrs {
                            this.aspect = props.aspect
                            isAspectSelected = props.aspect.id ==
                                    props.selectedAspectId && props.selectedPropertyIndex == null
                            onClick = props.aspectsModel::selectAspect
                            onAddToListIconClick = props.aspectsModel::createProperty
                            onExpandAllStructure = ::handleExpandAllStructure
                        }
                    }
                }!!
            }

            if (props.aspect.properties.isNotEmpty() || (props.aspect.id == props.selectedAspectId && props.selectedPropertyIndex != null)) {
                aspectProperties {
                    attrs {
                        this.aspect = props.aspect
                        selectedAspectId = props.selectedAspectId
                        selectedPropertyIndex = props.selectedPropertyIndex
                        onSubtreeExpandStateChanged = ::handleExpandStateChanged
                        aspectContext = props.aspectContext
                        subtreeExpanded = state.expandedSubtree
                        aspectsModel = props.aspectsModel
                    }
                }
            }
        }
    }

    interface State : RState {
        var expandedSubtree: Boolean
    }

    interface Props : RProps {
        var aspect: AspectData
        var selectedAspectId: String?
        var selectedPropertyIndex: Int?
        var aspectContext: Map<String, AspectData>
        var aspectsModel: AspectsModel
    }
}

fun RBuilder.aspectTreeView(block: RHandler<AspectTreeView.Props>) = child(AspectTreeView::class, block)