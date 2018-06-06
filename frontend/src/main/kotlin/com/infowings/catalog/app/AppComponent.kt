package com.infowings.catalog.app

import com.infowings.catalog.aspects.AspectsPage
import com.infowings.catalog.auth.AuthComponent
import com.infowings.catalog.auth.privateRoute
import com.infowings.catalog.history.HistoryPage
import com.infowings.catalog.measures.MeasuresPage
import com.infowings.catalog.objects.ObjectsPage
import com.infowings.catalog.subjects.SubjectsPage
import com.infowings.catalog.reference.book.ReferenceBookPage
import com.infowings.catalog.subjects.SubjectRouter
import com.infowings.catalog.wrappers.RouteSuppliedProps
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

            privateRoute("/aspects", true, renderFunction<AspectsPage>())
            privateRoute("/objects", true, renderFunction<ObjectsPage>())
            privateRoute("/measures", true, renderFunction<MeasuresPage>())
            privateRoute("/reference", true, renderFunction<ReferenceBookPage>())
            privateRoute("/subjects", false, renderFunction<SubjectRouter>())
            privateRoute("/history", true, renderFunction<HistoryPage>())

            reactRouter.Route {
                attrs {
                    path = "/"
                    render = { reactRouter.Redirect { attrs.to = "/objects" } }
                }
            }
        }
    }
}

inline fun <reified T : RComponent<RouteSuppliedProps, out RState>> renderFunction(): RBuilder.(RouteSuppliedProps) -> Unit {
    return { props ->
        child(T::class) {
            attrs {
                location = props.location; history = props.history; match = props.match
            }
        }
    }
}