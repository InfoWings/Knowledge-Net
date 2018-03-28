package com.infowings.catalog.history

import com.infowings.catalog.aspects.editconsole.popup.popup
import com.infowings.catalog.aspects.treeview.aspectRootLabel
import com.infowings.catalog.aspects.treeview.aspectTreeProperties
import com.infowings.catalog.common.AspectDataView
import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinext.js.invoke
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

class AspectFullContainer : RComponent<AspectFullContainer.Props, AspectFullContainer.State>() {

    companion object {
        init {
            kotlinext.js.require("styles/aspect-tree-view.scss")
        }
    }

    override fun State.init(props: Props) {
        expanded = true
    }

    override fun componentDidMount() {
        setState {
            expanded = true
        }
    }

    private fun handleExpanderClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            expanded = !expanded
        }
    }

    override fun RBuilder.render() {
        div("aspect-popup-container") {
            popup {
                attrs.closePopup = { props.onExit() }

                div(classes = "aspect-tree-view--root") {
                    if (props.view.aspectData.properties.isNotEmpty()) {
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
                        attrs {
                            aspect = props.view.aspectData
                            onClick = {}
                            selected = false
                        }
                    }
                }
                if (props.view.aspectData.properties.isNotEmpty() && state.expanded) {
                    val children = props.view.related.map { it.copy(properties = emptyList()) }
                    aspectTreeProperties {
                        attrs {
                            parentAspect = props.view.aspectData
                            aspectContext = children.filter { it.id != null }.map { it.id!! to it }.toMap()
                            onAspectPropertyClick = { _, _ -> Unit }
                            this.selectedAspect = null
                            selectedPropertyIndex = null
                            parentSelected = false
                            onAspectPropertyRequest = {}
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var onExit: () -> Unit
        var view: AspectDataView
    }

    interface State : RState {
        var expanded: Boolean
    }
}

fun RBuilder.aspectFullContainer(handler: RHandler<AspectFullContainer.Props>) =
    child(AspectFullContainer::class, handler)