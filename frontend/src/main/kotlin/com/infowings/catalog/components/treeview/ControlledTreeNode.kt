package com.infowings.catalog.components.treeview

import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.svg


/**
 * Component represents possible node in a tree (or any list that can be expanded).
 *
 * [RProps.children] can be hidden or expanded (any React element), node content may influence on expansion state
 * (by extending [TreeNodeContentProps]).
 */
class ControlledTreeNode : RComponent<ControlledTreeNode.Props, RState>() {

    companion object {
        init {
            kotlinext.js.require("styles/tree-view.scss")
        }
    }

    override fun RBuilder.render() {
        val className = props.className
        val additionalClasses = className?.let { " $it" } ?: ""
        div(classes = "tree-view--node$additionalClasses") {
            if (!props.hideExpandButton) {
                if (Children.count(props.children) > 0) {
                    if (props.expanded) {
                        squareMinusIcon(classes = "tree-view--expander-icon") {
                            attrs.onClickFunction = { props.onExpanded(false) }
                        }
                    } else {
                        squarePlusIcon(classes = "tree-view--expander-icon") {
                            attrs.onClickFunction = { props.onExpanded(true) }
                        }
                    }
                } else {
                    svg(classes = "tree-view--expander-icon")
                }
            }
            child(props.treeNodeContent)
        }

        if (Children.count(props.children) > 0 && props.expanded) {
            div(classes = "tree-view--children$additionalClasses") {
                children()
            }
        }
    }

    class Props(
        var className: String?,
        var expanded: Boolean,
        var onExpanded: (Boolean) -> Unit,
        var treeNodeContent: ReactElement,
        var hideExpandButton: Boolean
    ) : RProps
}

fun RBuilder.controlledTreeNode(block: RHandler<ControlledTreeNode.Props>) = child(ControlledTreeNode::class, block)
