package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.react.use
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
        div(classes = "aspect-tree-view--root") {
            if (props.aspect.properties.isNotEmpty()) {
                svg(classes = "aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
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
                svg(classes = "aspect-tree-view--line-icon")
            }
            if (props.aspect.id != null) {
                svg(classes = "aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                    attrs {
                        onClickFunction = ::handleAddToListClick
                    }
                    use("svg/sprite.svg#icon-add-to-list")
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
