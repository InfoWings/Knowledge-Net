package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

class AspectTreeProperty : RComponent<AspectTreeProperty.Props, AspectTreeProperty.State>() {

    private fun handleExpanderClick(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        setState {
            expanded = !expanded
        }
    }

    private fun handleAddToListClick(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        val childAspect = props.aspect ?: error("Clicked add to list with no aspect")
        setState {
            expanded = true
        }
        props.onAspectPropertyRequest(childAspect)
    }

    override fun RBuilder.render() {
        val childAspect = props.aspect
        val selectedAspect = props.selectedAspect
        div(classes = "aspect-tree-view--property") {
            if (childAspect != null && childAspect.properties.isNotEmpty()) {
                if (state.expanded) {
                    squareMinusIcon(classes = "aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                        attrs {
                            onClickFunction = ::handleExpanderClick
                        }
                    }
                } else {
                    squarePlusIcon(classes = "aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                        attrs {
                            onClickFunction = ::handleExpanderClick
                        }
                    }
                }
            } else {
                svg("aspect-tree-view--line-icon")
            }
            aspectPropertyLabel {
                attrs {
                    aspectProperty = props.aspectProperty
                    aspect = childAspect
                    onClick = props.onLabelClick
                    propertySelected = props.propertySelected
                    aspectSelected = childAspect != null && childAspect.id == props.selectedAspect?.id
                }
            }
            if (props.aspect != null) {
                if (props.aspect!!.deleted) {
                    ripIcon("aspect-tree-view--rip-icon") {}
                } else {
                    addToListIcon(classes = "aspect-tree-view--add-to-list-icon") {
                        attrs {
                            onClickFunction = ::handleAddToListClick
                        }
                    }
                }
            }
        }

        if (childAspect != null && childAspect.properties.isNotEmpty() && state.expanded) {
            aspectTreeProperties {
                attrs {
                    parentAspect =
                            if (selectedAspect != null && selectedAspect.id == childAspect.id) selectedAspect else childAspect
                    aspectContext = props.aspectContext
                    onAspectPropertyClick = props.onAspectPropertyClick
                    this.selectedAspect = props.selectedAspect
                    selectedPropertyIndex = props.selectedPropertyIndex
                    parentSelected = childAspect.id == props.selectedAspect?.id
                    onAspectPropertyRequest = props.onAspectPropertyRequest
                }
            }
        }
    }

    interface Props : RProps {
        var parentAspect: AspectData
        var aspectProperty: AspectPropertyData
        var aspect: AspectData?
        var onAspectPropertyClick: (AspectData, propertyIndex: Int) -> Unit
        var onLabelClick: () -> Unit
        var aspectContext: Map<String, AspectData>
        var propertySelected: Boolean
        var selectedAspect: AspectData?
        var selectedPropertyIndex: Int?
        var onAspectPropertyRequest: (AspectData) -> Unit
    }

    interface State : RState {
        var expanded: Boolean
    }
}

fun RBuilder.aspectTreeProperty(block: RHandler<AspectTreeProperty.Props>) = child(AspectTreeProperty::class, block)