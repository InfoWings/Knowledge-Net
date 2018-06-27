package com.infowings.catalog.objects

import com.infowings.catalog.layout.header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState

class ObjectsViewPage : RComponent<RouteSuppliedProps, RState>() {
    override fun RBuilder.render() {
        header {
            attrs.location = props.location.pathname
            attrs.history = props.history
        }
        objectViewApiModel
    }
}