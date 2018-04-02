package com.infowings.catalog.subjects

import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1

class SubjectsPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {
        child(Header::class) {
            attrs.location = props.location.pathname
        }
        h1 { +"Subjects Page" }

        child(SubjectApiMiddleware::class) {

        }
    }
}
