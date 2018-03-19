package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import react.*
import react.dom.div

class AspectTreeProperties : RComponent<AspectTreeProperties.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--properties-block") {
            props.parentAspect.properties.mapIndexed { index, property ->
                aspectTreeProperty {
                    attrs {
                        key = property.id
                        parentAspect = props.parentAspect
                        aspectProperty = property
                        aspect = if (property.id == "") null
                        else props.aspectContext[property.aspectId]
                                ?: throw Error("Aspect Property $property has aspectId that " +
                                "was neigher in the fetched list nor created")
                        onAspectPropertyClick = props.onAspectPropertyClick
                        onLabelClick = { props.onAspectPropertyClick(props.parentAspect, index) }
                        aspectContext = props.aspectContext
                        selectedAspect = props.selectedAspect
                        selectedPropertyIndex = props.selectedPropertyIndex
                        propertySelected = props.parentSelected && index == props.selectedPropertyIndex
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var parentAspect: AspectData
        var aspectContext: Map<String, AspectData>
        var onAspectPropertyClick: (AspectData, propertyIndex: Int) -> Unit
        var selectedAspect: AspectData?
        var selectedPropertyIndex: Int?
        var parentSelected: Boolean
    }

}

fun RBuilder.aspectTreeProperties(block: RHandler<AspectTreeProperties.Props>) = child(AspectTreeProperties::class, block)