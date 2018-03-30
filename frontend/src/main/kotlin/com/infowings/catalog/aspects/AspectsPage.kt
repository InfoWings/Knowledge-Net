package com.infowings.catalog.aspects

import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState

class AspectsPage : RComponent<RouteSuppliedProps, RState>() {
    override fun RBuilder.render() {
        child(Header::class) {
            attrs.location = props.location.pathname
        }
        aspectApiMiddleware(AspectsModelComponent::class)
    }
}
