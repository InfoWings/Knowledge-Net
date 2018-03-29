package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.wrappers.react.setStateWithCallback
import react.*

/**
 * View component. Draws aspect tree node for aspect property at any level except for the root.
 */
class AspectProperties : RComponent<AspectProperties.Props, RState>() {

    private fun handleAddPropertyToAspect(aspect: AspectData) {
        props.onSelectAspect(aspect.id)
        props.onAddAspectProperty(aspect.properties.size) // Not allow create property if last one is already empty?
    }

    override fun RBuilder.render() {
        props.aspect.properties.forEachIndexed { index, property ->
            if (!property.deleted) {
                child(AspectPropertyNodeExpandedWrapper::class) {
                    attrs {
                        parentAspect = props.aspect
                        propertyIndex = index
                        aspectProperty = property
                        selectedAspectId = props.selectedAspectId
                        selectedPropertyIndex = props.selectedPropertyIndex
                        onSelectAspect = props.onSelectAspect
                        onAspectPropertyClick = props.onAspectPropertyClick
                        aspectContext = props.aspectContext
                        onAddAspectProperty = props.onAddAspectProperty
                        onAddPropertyToAspect = ::handleAddPropertyToAspect
                        subtreeExpanded = props.subtreeExpanded
                        onSubtreeExpandStateChanged = props.onSubtreeExpandStateChanged
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var selectedAspectId: String?
        var selectedPropertyIndex: Int?
        var onSelectAspect: (aspectId: String?) -> Unit
        var onAspectPropertyClick: (aspectId: String?, propertyIndex: Int) -> Unit
        var aspectContext: (aspectId: String) -> AspectData?
        var onAddAspectProperty: (propertyIndex: Int) -> Unit
        var subtreeExpanded: Boolean
        var onSubtreeExpandStateChanged: () -> Unit
    }

}

/**
 * Wrapper component that incapsulates and manages state of expanded subtree.
 */
class AspectPropertyNodeExpandedWrapper(props: Props) : RComponent<AspectPropertyNodeExpandedWrapper.Props, AspectPropertyNodeExpandedWrapper.State>(props) {

    override fun State.init(props: Props) {
        subtreeExpanded = props.subtreeExpanded
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        setState {
            subtreeExpanded = nextProps.subtreeExpanded
        }
    }

    private fun onSubtreeExpandStateChanged() {
        setStateWithCallback(props.onSubtreeExpandStateChanged) {
            subtreeExpanded = false
        }
    }

    override fun RBuilder.render() {
        treeNode {
            val childAspect =
                    if (props.aspectProperty.aspectId.isNotEmpty())
                        props.aspectContext(props.aspectProperty.aspectId)
                                ?: error("AspectPropertyData.aspectId should be among ids of received aspects")
                    else null

            attrs {
                key = if (props.aspectProperty.id.isEmpty()) props.propertyIndex.toString() else props.aspectProperty.id
                className = "aspect-tree-view--aspect-property-node"
                expanded = props.subtreeExpanded
                onExpanded = { onSubtreeExpandStateChanged() }
                treeNodeContent = buildElement {
                    aspectPropertyNode {
                        attrs {
                            aspectProperty = props.aspectProperty
                            isPropertySelected = props.parentAspect.id == props.selectedAspectId
                                    && props.propertyIndex == props.selectedPropertyIndex
                            correspondingAspect = childAspect
                            isCorrespondingAspectSelected =
                                    if (childAspect == null) false else childAspect.id == props.selectedAspectId
                            onClick = { props.onAspectPropertyClick(props.parentAspect.id, props.propertyIndex) }
                            onAddToListIconClick = if (childAspect == null) null else {
                                { props.onAddPropertyToAspect(childAspect) }
                            }
                        }
                    }
                }!!
            }

            if (childAspect != null && childAspect.properties.isNotEmpty()) {
                aspectProperties {
                    attrs {
                        aspect = childAspect
                        selectedAspectId = props.selectedAspectId
                        selectedPropertyIndex = props.selectedPropertyIndex
                        onSelectAspect = props.onSelectAspect
                        onAspectPropertyClick = props.onAspectPropertyClick
                        aspectContext = props.aspectContext
                        onAddAspectProperty = props.onAddAspectProperty
                        subtreeExpanded = state.subtreeExpanded
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var parentAspect: AspectData
        var propertyIndex: Int
        var aspectProperty: AspectPropertyData
        var selectedAspectId: String?
        var selectedPropertyIndex: Int?
        var onSelectAspect: (aspectId: String?) -> Unit
        var onAspectPropertyClick: (aspectId: String?, propertyIndex: Int) -> Unit
        var aspectContext: (aspectId: String) -> AspectData?
        var onAddAspectProperty: (propertyIndex: Int) -> Unit
        var onAddPropertyToAspect: (AspectData) -> Unit
        var subtreeExpanded: Boolean
        var onSubtreeExpandStateChanged: () -> Unit
    }

    interface State : RState {
        var subtreeExpanded: Boolean
    }
}

fun RBuilder.aspectProperties(block: RHandler<AspectProperties.Props>) = child(AspectProperties::class, block)