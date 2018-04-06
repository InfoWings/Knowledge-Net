package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.aspects.AspectsModel
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.emptyAspectPropertyData
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.wrappers.react.setStateWithCallback
import react.*

/**
 * View component. Draws aspect tree node for aspect property at any level except for the root.
 */
class AspectProperties : RComponent<AspectProperties.Props, RState>() {

    private fun addPropertyToAspect(aspect: AspectData) {
        props.aspectsModel.selectAspect(aspect.id)
        if (aspect.properties.isEmpty() || aspect.properties.last() != emptyAspectPropertyData) {
            props.aspectsModel.createProperty(aspect.properties.size) // Not allow create property if last one is already empty?
        }
    }

    override fun RBuilder.render() {
        props.aspect.properties.forEachIndexed { index, property ->
            if (!property.deleted) {
                child(AspectPropertyNodeExpandedWrapper::class) {
                    attrs {
                        parentAspect = props.aspect
                        propertyIndex = index
                        selectedAspectId = props.selectedAspectId
                        selectedPropertyIndex = props.selectedPropertyIndex
                        subtreeExpanded = props.subtreeExpanded
                        onSubtreeExpandStateChanged = props.onSubtreeExpandStateChanged
                        aspectContext = props.aspectContext
                        onAddPropertyToAspect = ::addPropertyToAspect
                        aspectsModel = props.aspectsModel
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var selectedAspectId: String?
        var selectedPropertyIndex: Int?
        var subtreeExpanded: Boolean
        var aspectContext: (aspectId: String) -> AspectData?
        var onSubtreeExpandStateChanged: () -> Unit
        var aspectsModel: AspectsModel
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
            val aspectProperty = props.parentAspect.properties[props.propertyIndex]
            console.log(props.parentAspect)
            console.log(aspectProperty)
            console.log(props.aspectContext(aspectProperty.aspectId))
            val childAspect =
                if (aspectProperty.aspectId.isNotEmpty())
                    props.aspectContext(aspectProperty.aspectId)
                            ?: error("AspectPropertyData.aspectId should be among ids of received aspects")
                else null

            attrs {
                key = if (aspectProperty.id.isEmpty()) props.propertyIndex.toString() else aspectProperty.id
                className = "aspect-tree-view--aspect-property-node"
                expanded = props.subtreeExpanded
                onExpanded = { onSubtreeExpandStateChanged() }
                treeNodeContent = buildElement {
                    aspectPropertyNode {
                        attrs {
                            this.aspectProperty = aspectProperty
                            isPropertySelected = props.parentAspect.id == props.selectedAspectId
                                    && props.propertyIndex == props.selectedPropertyIndex
                            correspondingAspect = childAspect
                            isCorrespondingAspectSelected =
                                    if (childAspect == null) false else childAspect.id == props.selectedAspectId
                            onClick = {
                                props.aspectsModel.selectAspectProperty(
                                    props.parentAspect.id,
                                    props.propertyIndex
                                )
                            }
                            onAddToListIconClick = childAspect?.let { { props.onAddPropertyToAspect(childAspect) } }
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
                        aspectContext = props.aspectContext
                        subtreeExpanded = state.subtreeExpanded
                        aspectsModel = props.aspectsModel
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var parentAspect: AspectData
        var propertyIndex: Int

        var selectedAspectId: String?
        var selectedPropertyIndex: Int?
        var aspectContext: (aspectId: String) -> AspectData?

        var subtreeExpanded: Boolean
        var onSubtreeExpandStateChanged: () -> Unit

        var onAddPropertyToAspect: (AspectData) -> Unit
        var aspectsModel: AspectsModel
    }

    interface State : RState {
        var subtreeExpanded: Boolean
    }
}

fun RBuilder.aspectProperties(block: RHandler<AspectProperties.Props>) = child(AspectProperties::class, block)