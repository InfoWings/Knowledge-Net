package com.infowings.catalog.reference.book

import com.infowings.catalog.layout.header
import com.infowings.catalog.reference.book.treeview.ReferenceBookTreeView
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState

class ReferenceBookPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {

        header {
            attrs {
                location = props.location.pathname
                history = props.history
            }
        }

        referenceBookApiMiddleware(ReferenceBookTreeView::class)
    }
}