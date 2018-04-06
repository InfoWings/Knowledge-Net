package com.infowings.catalog.app

import com.infowings.catalog.aspects.AspectsPage
import com.infowings.catalog.auth.AuthComponent
import com.infowings.catalog.auth.privateRoute
import com.infowings.catalog.history.HistoryPage
import com.infowings.catalog.reference.book.ReferenceBookPage
import com.infowings.catalog.subjects.SubjectsPage
import com.infowings.catalog.units.UnitsPage
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
                "/reference",
                renderFunction = { rprops ->
                    child(ReferenceBookPage::class) {
                        attrs {
                            location = rprops.location; history = rprops.history; match = rprops.match
                        }
                    }
                })
            privateRoute(
                "/aspects",
                renderFunction = { rprops ->
                    child(AspectsPage::class) {
                        attrs {
                            location = rprops.location; history = rprops.history; match = rprops.match
                        }
                    }
                })
            privateRoute(
                "/units",
                renderFunction = { rprops ->
                    child(UnitsPage::class) {
                        attrs {
                            location = rprops.location; history = rprops.history; match = rprops.match
                        }
                    }
                })
            privateRoute(
                "/subjects",
                renderFunction = { rprops ->
                    child(SubjectsPage::class) {
                        attrs {
                            location = rprops.location; history = rprops.history; match = rprops.match
                        }
                    }
                })
            privateRoute("/history") { rprops ->
                child(HistoryPage::class) {
                    attrs {
                        location = rprops.location; history = rprops.history; match = rprops.match
                    }
                }
            }
            reactRouter.Route {
                attrs {
                    path = "/"
                    render = { reactRouter.Redirect { attrs.to = "/aspects" } }
                }
            }
        }
    }
}