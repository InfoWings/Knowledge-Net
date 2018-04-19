package com.infowings.catalog.subjects

import com.infowings.catalog.layout.header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.div

class SubjectsPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {
        header {
            attrs {
                location = props.location.pathname
                history = props.history
            }
        }

        div("subjects-page") {
            child(SubjectApiMiddleware::class) {}
        }
    }
}
