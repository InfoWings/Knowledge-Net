package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.wrappers.react.use
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

class AspectTreeRoot : RComponent<AspectTreeRoot.Props, AspectTreeRoot.State>() {

    private fun handleExpanderClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            expanded = !expanded
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--root") {
            if (props.aspect.properties.isNotEmpty()) {
                svg("aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                    attrs {
                        onClickFunction = ::handleExpanderClick
                    }
                    if (state.expanded) {
                        use("svg/sprite.svg#icon-squared-minus")
                    } else {
                        use("svg/sprite.svg#icon-squared-plus")
                    }
                }
            } else {
                svg("aspect-tree-view--line-icon") {
                    use("svg/sprite.svg#icon-add-to-list")
                }
            }
            aspectRootLabel {
                attrs {
                    aspect = props.aspect
                    onClick = props.onAspectClick
                    selected = props.selectedId == props.aspect.id
                }
            }
        }
        if (props.aspect.properties.isNotEmpty() && state.expanded) {
            aspectTreeProperties {
                attrs {
                    aspectProperties = props.aspect.properties
                    aspectContext = props.aspectContext
                    onAspectPropertyClick = props.onAspectPropertyClick
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var onAspectClick: (AspectData) -> Unit
        var onAspectPropertyClick: (AspectPropertyData) -> Unit
        var aspectContext: Map<String, AspectData>
        var selectedId: String?
    }

    interface State : RState {
        var expanded: Boolean
    }
}

fun RBuilder.aspectTreeRoot(block: RHandler<AspectTreeRoot.Props>) =
        child(AspectTreeRoot::class, block)
