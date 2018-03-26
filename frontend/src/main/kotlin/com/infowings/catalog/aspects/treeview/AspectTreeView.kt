package com.infowings.catalog.aspects.treeview

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
            props.aspects.map { aspect ->
                treeNode {
                    attrs {
                        key = aspect.id ?: ""
                        className = "aspect-tree-view--aspect-node"
                        treeNodeContent = buildElement {
                            aspectNode {
                                attrs {
                                    this.aspect = aspect
                                    isAspectSelected = aspect.id == props.selectedAspectId
                                    onClick = props.onAspectClick
                                    onAddToListIconClick = props.onAddAspectProperty
                                }
                            }
                        }!!
                    }

                    if (aspect.properties.isNotEmpty()) {
                        aspectProperties {
                            attrs {
                                this.aspect = aspect
                                selectedAspectId = props.selectedAspectId
                                selectedPropertyIndex = props.selectedPropertyIndex
                                onSelectAspect = props.onAspectClick
                                onAspectPropertyClick = props.onAspectPropertyClick
                                aspectContext = props.aspectContext
                                onAddAspectProperty = props.onAddAspectProperty
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspects: List<AspectData>
        var onAspectClick: (aspectId: String?) -> Unit
        var onAspectPropertyClick: (aspectId: String?, propertyIndex: Int) -> Unit
        var aspectContext: (aspectId: String) -> AspectData?
        var selectedAspectId: String?
        var selectedPropertyIndex: Int?
        var onAddAspectProperty: (propertyIndex: Int) -> Unit
    }
}

fun RBuilder.aspectTreeView(block: RHandler<AspectTreeView.Props>) = child(AspectTreeView::class, block)