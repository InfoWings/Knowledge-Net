package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import react.*
import react.dom.div

class AspectTreeProperties : RComponent<AspectTreeProperties.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--properties-block") {
            props.aspectProperties.map {
                aspectTreeProperty {
                    attrs {
                        key = it.id
                        aspectProperty = it
                        aspect = props.aspectContext[it.aspectId] ?: throw Error("Aspect Property $it has aspectId that " +
                                "was neigher in the fetched list nor created")
                        onAspectPropertyClick = props.onAspectPropertyClick
                        aspectContext = props.aspectContext
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectProperties: List<AspectPropertyData>
        var aspectContext: Map<String, AspectData>
        var onAspectPropertyClick: (AspectPropertyData) -> Unit
    }

}

fun RBuilder.aspectTreeProperties(block: RHandler<AspectTreeProperties.Props>) = child(AspectTreeProperties::class, block)