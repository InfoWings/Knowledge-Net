package com.infowings.catalog.components.treeview

import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.svg

interface TreeNodeContentProps : RProps {
    var isExpanded: Boolean
    var setExpanded: (Boolean) -> Unit
}

class TreeNode(props: Props) : RComponent<TreeNode.Props, TreeNode.State>(props) {

    companion object {
        init {
            require("styles/tree-view.scss")
        }
    }

    override fun State.init(props: Props) {
        expanded = false
    }

    private fun setExpanded(expanded: Boolean) {
        setState {
            this.expanded = expanded
        }
    }

    override fun RBuilder.render() {
        val className = props.className
        val additionalClasses = className?.let { " $it" } ?: ""
        div(classes = "tree-view--node$additionalClasses") {
            if (React.Children.count(props.children) > 0) {
                if (state.expanded) {
                    squareMinusIcon(classes = "tree-view--expander-icon") {
                        attrs.onClickFunction = { setExpanded(false) }
                    }
                } else {
                    squarePlusIcon(classes = "tree-view--expander-icon") {
                        attrs.onClickFunction = { setExpanded(true) }
                    }
                }
            } else {
                svg(classes = "tree-view--expander-icon")
            }
            child(React.cloneElement<TreeNodeContentProps>(props.treeNodeContent) {
                isExpanded = state.expanded
                setExpanded = ::setExpanded
            })
        }
        if (React.Children.count(props.children) > 0 && state.expanded) {
            div(classes = "tree-view--children$additionalClasses") {
                children()
            }
        }
    }

    interface Props : RProps {
        var className: String?
        var treeNodeContent: ReactElement
    }

    interface State : RState {
        var expanded: Boolean
    }
}

fun RBuilder.treeNode(block: RHandler<TreeNode.Props>) = child(TreeNode::class, block)

