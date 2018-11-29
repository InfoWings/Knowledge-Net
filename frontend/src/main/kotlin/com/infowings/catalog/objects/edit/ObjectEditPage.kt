package com.infowings.catalog.objects.edit

import com.infowings.catalog.layout.header
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState

class ObjectEditPage : RComponent<ObjectEditPage.Props, RState>() {

    override fun RBuilder.render() {
        header {
            attrs.location = props.location.pathname
            attrs.history = props.history
        }
        val objectId = props.objectId
        if (objectId == null) {
            objectCreateApiModel { props.history.push("/objects/${encodeURIComponent(it)}") }
        } else {
            objectEditApiModel(objectId, props.editMode, props.highlightedGuid)
        }
    }

    interface Props : RouteSuppliedProps {
        var objectId: String?
        var editMode: Boolean
        var highlightedGuid: String?
    }
}