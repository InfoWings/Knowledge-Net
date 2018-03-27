package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.aspects.treeview.view.aspectRootLabel
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.components.treeview.TreeNodeContentProps
import com.infowings.catalog.utils.addToListIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*

/**
 * View component. Draws label for root aspect node with icon. Attaches handler to the icon
 */
class AspectNode : RComponent<AspectNode.CombinedProps, RState>() {

    private fun handleAddToListClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.setExpanded(true)
        if (props.aspect.properties.isEmpty() || props.aspect.properties.last() != AspectPropertyData("", "", "", "")) {
            props.onClick(props.aspect.id)
            props.onAddToListIconClick(props.aspect.properties.size)
        }
    }

    override fun RBuilder.render() {
        aspectRootLabel {
            attrs {
                aspect = props.aspect
                selected = props.isAspectSelected
                onClick = props.onClick
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

    interface Props : RProps {
        var aspect: AspectData
        var isAspectSelected: Boolean
        var onClick: (String?) -> Unit
        var onAddToListIconClick: (propertyIndex: Int) -> Unit
    }

    interface CombinedProps : Props, TreeNodeContentProps
}

fun RBuilder.aspectNode(block: RHandler<AspectNode.Props>) =
        child(AspectNode::class, block)
