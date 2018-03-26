package com.infowings.catalog.app

import com.infowings.catalog.aspects.AspectsPage
import com.infowings.catalog.auth.AuthComponent
import com.infowings.catalog.auth.privateRoute
import com.infowings.catalog.reference.book.ReferenceBookPage
import com.infowings.catalog.measures.MeasuresPage
import com.infowings.catalog.wrappers.reactRouter
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

class CatalogAppComponent : RComponent<RProps, RState>() {

    override fun RBuilder.render() {
        reactRouter.Switch {
            reactRouter.Route {
                attrs {
                    path = "/login"
                    component = ::AuthComponent
                }
            }
            privateRoute(
                "/aspects",
                renderFunction = { props ->
                    child(AspectsPage::class) {
                        attrs {
                            location = props.location; history = props.history; match = props.match
                        }
                    }
                })
            privateRoute(
                "/measures",
                renderFunction = { props ->
                    child(MeasuresPage::class) {
                        attrs {
                            location = props.location; history = props.history; match = props.match
                        }
                    }
                })
            privateRoute(
                "/reference",
                renderFunction = { props ->
                    child(ReferenceBookPage::class) {
                        attrs {
                            location = props.location; history = props.history; match = props.match
                        }
                    }
                })
            reactRouter.Route {
                attrs {
                    path = "/"
                    render = { reactRouter.Redirect { attrs.to = "/aspects" } }
                }
            }
        }
    }
}