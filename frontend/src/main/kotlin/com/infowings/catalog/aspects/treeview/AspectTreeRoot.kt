package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

class AspectTreeRoot : RComponent<AspectTreeRoot.Props, AspectTreeRoot.State>() {

    override fun componentWillReceiveProps(nextProps: Props) {
        if (nextProps.selectedAspect?.id == nextProps.aspect.id) {
            setState {
                expanded = true
            }
        }
    }

    private fun handleExpanderClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            expanded = !expanded
        }
    }

    private fun handleAddToListClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.onAspectPropertyRequest(props.aspect)
    }

    override fun RBuilder.render() {
        console.log(props.aspect)
        div(classes = "aspect-tree-view--root") {
            if (props.aspect.properties.isNotEmpty()) {
                if (state.expanded) {
                    squareMinusIcon(classes = "aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                        attrs.onClickFunction = ::handleExpanderClick
                    }
                } else {
                    squarePlusIcon(classes = "aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                        attrs.onClickFunction = ::handleExpanderClick
                    }
                }
            } else {
                svg(classes = "aspect-tree-view--line-icon")
            }
            aspectRootLabel {
                val selectedAspect = props.selectedAspect
                attrs {
                    aspect = if (selectedAspect != null && selectedAspect.id == props.aspect.id) selectedAspect else props.aspect
                    onClick = props.onAspectClick
                    selected = props.selectedAspect?.id == props.aspect.id
                }
            }
            if (props.aspect.name != "") {
                addToListIcon(classes = "aspect-tree-view--add-to-list-icon") {
                    attrs {
                        onClickFunction = ::handleAddToListClick
                    }
                }
            }
        }
        if (props.aspect.properties.isNotEmpty() && state.expanded) {
            aspectTreeProperties {
                val selectedAspect = props.selectedAspect
                attrs {
                    parentAspect = if (selectedAspect != null && selectedAspect.id == props.aspect.id) selectedAspect else props.aspect
                    aspectContext = props.aspectContext
                    onAspectPropertyClick = props.onAspectPropertyClick
                    this.selectedAspect = props.selectedAspect
                    selectedPropertyIndex = props.selectedPropertyIndex
                    parentSelected = props.aspect.id == props.selectedAspect?.id
                    onAspectPropertyRequest = props.onAspectPropertyRequest
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var onAspectClick: (AspectData) -> Unit
        var onAspectPropertyClick: (AspectData, propertyIndex: Int) -> Unit
        var aspectContext: Map<String, AspectData>
        var selectedAspect: AspectData?
        var selectedPropertyIndex: Int?
        var onAspectPropertyRequest: (AspectData) -> Unit
    }

    interface State : RState {
        var expanded: Boolean
    }
}

fun RBuilder.aspectTreeRoot(block: RHandler<AspectTreeRoot.Props>) =
        child(AspectTreeRoot::class, block)
