package com.infowings.catalog.subjects

import com.infowings.catalog.app.renderFunction
import com.infowings.catalog.auth.privateRoute
import com.infowings.catalog.components.reference.ReferencePage
import com.infowings.catalog.layout.header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.reactRouter
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.div

class SubjectRouter : RComponent<RouteSuppliedProps, RState>() {
    override fun RBuilder.render() {
        reactRouter.Switch {
            privateRoute("${props.match.url}/reference/:subjectName", true, renderFunction<ReferencePage>())
            privateRoute(props.match.url, true, renderFunction<SubjectsPage>())
            reactRouter.Route {
                attrs {
                    path = "/"
                    render = { reactRouter.Redirect { attrs.to = props.match.url } }
                }
            }
        }
    }
}

class SubjectsPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {
        header {
            attrs {
                location = props.location.pathname
                history = props.history
            }
        }

        div("subjects-page") {
            child(SubjectApiMiddleware::class) {
                attrs {
                    location = props.location
                    history = props.history
                    match = props.match
                }
            }
        }
    }
}
