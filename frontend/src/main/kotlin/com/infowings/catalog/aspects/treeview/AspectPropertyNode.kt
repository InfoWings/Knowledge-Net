package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.aspects.treeview.view.aspectPropertyLabel
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.components.treeview.TreeNodeContentProps
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.ripIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*

/**
 * View component. Draws aspect property label along with add-to-list icon. Attaches handler to icon click
 */
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

        val correspondingAspect = props.correspondingAspect
        if (props.aspectProperty.id.isNotEmpty() && correspondingAspect != null) {
            if (correspondingAspect.deleted) {
                ripIcon("aspect-tree-view--rip-icon") {}
            } else {
                addToListIcon(classes = "aspect-tree-view--add-to-list-icon") {
                    attrs.onClickFunction = ::handleAddToListClick
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