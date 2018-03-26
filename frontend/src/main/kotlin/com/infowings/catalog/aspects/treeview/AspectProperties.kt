package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.treeview.treeNode
import react.*

class AspectProperties : RComponent<AspectProperties.Props, RState>() {

    private fun handleAddPropertyToAspect(aspect: AspectData) {
        props.onSelectAspect(aspect.id)
        props.onAddAspectProperty(aspect.properties.size)
    }

    override fun RBuilder.render() {
        props.aspect.properties.mapIndexed { index, property ->
            treeNode {
                val childAspect =
                        if (property.id.isNotEmpty())
                            props.aspectContext(property.aspectId)
                                    ?: error("AspectPropertyData.aspectId should be among ids of received aspects")
                        else null

                attrs {
                    key = if (property.id.isEmpty()) index.toString() else property.id
                    className = "aspect-tree-view--aspect-property-node"
                    treeNodeContent = buildElement {
                        aspectPropertyNode {
                            attrs {
                                aspectProperty = property
                                isPropertySelected = props.aspect.id == props.selectedAspectId
                                        && index == props.selectedPropertyIndex
                                correspondingAspect = childAspect
                                isCorrespondingAspectSelected =
                                        if (childAspect == null) false else childAspect.id == props.selectedAspectId
                                onClick = { props.onAspectPropertyClick(props.aspect.id, index) }
                                onAddToListIconClick = if (childAspect == null) null else {
                                    { handleAddPropertyToAspect(childAspect) }
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
                        }
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
    }

}

fun RBuilder.aspectProperties(block: RHandler<AspectProperties.Props>) = child(AspectProperties::class, block)