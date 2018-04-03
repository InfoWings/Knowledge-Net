package com.infowings.catalog.components.description

import com.infowings.catalog.utils.descriptionIcon
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

class DescriptionIconTarget : RComponent<DescriptionIconTarget.Props, RState>() {

    override fun RBuilder.render() {
        descriptionIcon(classes = props.className) {

        }
    }

    interface Props : RProps {
        var className: String?
        var description: String?
        var onDescriptionChanged: (String?) -> Unit
    }
}