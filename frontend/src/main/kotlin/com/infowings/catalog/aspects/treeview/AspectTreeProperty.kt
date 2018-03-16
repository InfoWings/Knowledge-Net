package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.wrappers.react.use
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

    override fun RBuilder.render() {
        val childAspect = props.aspect
        div(classes = "aspect-tree-view--property") {
            svg("aspect-tree-view--line-icon") {
                use("svg/sprite.svg#icon-dots-two-horizontal")
            }
            if (childAspect != null && childAspect.properties.isNotEmpty()) {
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
                svg("aspect-tree-view--line-icon")
            }
            if (props.parentAspect.id != null) {
                svg("aspect-tree-view--line-icon") {
                    use("svg/sprite.svg#icon-add-to-list")
                }
            } else {
                svg("aspect-tree-view--line-icon")
            }
            aspectPropertyLabel {
                attrs {
                    aspectProperty = props.aspectProperty
                    aspect = childAspect
                    onClick = props.onAspectPropertyClick
                    propertySelected = props.propertySelected
                    aspectSelected = childAspect != null && childAspect.id == props.selectedId
                }
            }
            if (childAspect != null && childAspect.properties.isNotEmpty() && state.expanded) {
                aspectTreeProperties {
                    attrs {
                        parentAspect = childAspect
                        aspectContext = props.aspectContext
                        onAspectPropertyClick = props.onAspectPropertyClick
                        selectedId = props.selectedId
                        selectedPropertyIndex = props.selectedPropertyIndex
                        parentSelected = childAspect.id == props.selectedId
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var parentAspect: AspectData
        var aspectProperty: AspectPropertyData
        var aspect: AspectData?
        var onAspectPropertyClick: (AspectPropertyData) -> Unit
        var aspectContext: Map<String, AspectData>
        var propertySelected: Boolean
        var selectedId: String?
        var selectedPropertyIndex: Int?
    }

    interface State : RState {
        var expanded: Boolean
    }
}

fun RBuilder.aspectTreeProperty(block: RHandler<AspectTreeProperty.Props>) = child(AspectTreeProperty::class, block)