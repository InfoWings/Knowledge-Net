package com.infowings.catalog.aspects

import com.infowings.catalog.layout.Header
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import com.infowings.catalog.wrappers.RouteSuppliedProps

class AspectsPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {
        child(Header::class) {
            attrs.location = props.location.pathname
        }
        h1 { +"AspectsPage" }
        child(AspectApiMiddleware::class) {
            //            attrs {
//                apiReceiver = AspectsTable::class
//            }
//            child(AspectsTable::class) {
//                attrs {
//                    data = aspectData
//                    aspectsMap = aspectData.associate { aspect -> Pair(aspect.id, aspect) }
//                }
//            }
        }
    }
}
