package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.aspects.AspectsModel
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.treeview.treeNode
import kotlinext.js.invoke
import kotlinext.js.require
import react.*
import react.dom.div


/**
 * View Component. Draws List of [treeNode] for each [AspectData] in [AspectTreeView.Props.aspects] list
 */
class AspectTreeView : RComponent<AspectTreeView.Props, RState>() {

    companion object {
        init {
            require("styles/aspect-tree-view.scss")
        }
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
        }
    }

    interface Props : RProps {
        var aspects: List<AspectData>
        var selectedAspectId: String?
        var selectedPropertyIndex: Int?
        var aspectContext: (aspectId: String) -> AspectData?
        var aspectsModel: AspectsModel
    }
}

/**
 * Wrapper component that incapsulates and manages state of expanded aspect tree.
 */
class AspectNodeExpandedStateWrapper : RComponent<AspectNodeExpandedStateWrapper.Props, AspectNodeExpandedStateWrapper.State>() {

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
                onExpanded = { handleExpandStateChanged() }
                treeNodeContent = buildElement {
                    aspectNode {
                        attrs {
                            this.aspect = props.aspect
                            isAspectSelected = props.aspect.id == props.selectedAspectId
                            onClick = props.aspectsModel::selectAspect
                            onAddToListIconClick = props.aspectsModel::createProperty
                            onExpandAllStructure = ::handleExpandAllStructure
                        }
                    }
                }!!
            }

            if (props.aspect.properties.isNotEmpty()) {
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
        var aspectContext: (aspectId: String) -> AspectData?
        var aspectsModel: AspectsModel
    }
}

fun RBuilder.aspectTreeView(block: RHandler<AspectTreeView.Props>) = child(AspectTreeView::class, block)