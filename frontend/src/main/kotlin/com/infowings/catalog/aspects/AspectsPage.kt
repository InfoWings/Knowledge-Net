package com.infowings.catalog.aspects

import com.infowings.catalog.aspects.model.DefaultAspectsModelComponent
import com.infowings.catalog.layout.header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState

class AspectsPage : RComponent<RouteSuppliedProps, RState>() {
    override fun RBuilder.render() {
        header {
            attrs.location = props.location.pathname
            attrs.history = props.history
        }
        aspectApiMiddleware(DefaultAspectsModelComponent::class)
    }
}
