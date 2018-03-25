package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.components.treeview.TreeNodeContentProps
import com.infowings.catalog.utils.addToListIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*

class AspectPropertyNode : RComponent<AspectPropertyNode.CombinedProps, RState>() {

    private fun handleAddToListClick(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.setExpanded(true)
        props.onAddToListIconClick?.also { it.invoke() }
    }

    override fun RBuilder.render() {
        aspectPropertyLabel {
            attrs {
                aspectProperty = props.aspectProperty
                aspect = props.correspondingAspect
                onClick = props.onClick
                propertySelected = props.isPropertySelected
                aspectSelected = props.isCorrespondingAspectSelected
            }
        }

        if (props.correspondingAspect != null) {
            addToListIcon(classes = "aspect-tree-view--add-to-list-icon") {
                attrs {
                    onClickFunction = ::handleAddToListClick
                }
            }
        }
    }

    interface Props : RProps {
        var aspectProperty: AspectPropertyData
        var isPropertySelected: Boolean

        var correspondingAspect: AspectData?
        var isCorrespondingAspectSelected: Boolean

        var onClick: () -> Unit
        var onAddToListIconClick: (() -> Unit)?
    }

    interface CombinedProps : Props, TreeNodeContentProps
}

fun RBuilder.aspectPropertyNode(block: RHandler<AspectPropertyNode.Props>) = child(AspectPropertyNode::class, block)