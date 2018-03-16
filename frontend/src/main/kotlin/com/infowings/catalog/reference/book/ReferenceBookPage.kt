package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.aspectApiMiddleware
import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState

class ReferenceBookPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {

        child(Header::class) {
            attrs { location = props.location.pathname }
        }

        aspectApiMiddleware(ReferenceBookControl::class)
    }
}