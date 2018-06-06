package com.infowings.catalog.components.reference

import com.infowings.catalog.aspects.filter.AspectsFilter
import com.infowings.catalog.aspects.model.emptyAspectModelComponent
import com.infowings.catalog.common.SubjectData
import react.*
import react.dom.div

class ReferenceComponent : RComponent<ReferenceComponent.Props, RState>() {

    override fun RBuilder.render() {
        div("reference-component") {
            emptyAspectModelComponent {
                attrs {
                    emptyMessage = "No linked aspects"
                    aspectsFilter = AspectsFilter(listOf(props.subject), emptyList())
                }
            }
        }
    }

    interface Props : RProps {
        var subject: SubjectData
    }
}

fun RBuilder.referenceComponent(handler: RHandler<ReferenceComponent.Props>) = child(ReferenceComponent::class, handler)