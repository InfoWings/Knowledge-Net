package com.infowings.common.catalog.aspects

import com.infowings.common.catalog.layout.Header
import com.infowings.common.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1


class AspectsPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {
        child(Header::class) {
            attrs { location = props.location.pathname }
        }
        h1 { +"AspectsPage" }
        child(AspectsTable::class) {}
    }
}
