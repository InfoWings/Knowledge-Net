package com.infowings.catalog.reference.book

import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div

class Popup : RComponent<Popup.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "popup_outer") {
            attrs {
                onClickFunction = { props.closePopup() }
            }
        }
        div(classes = "popup") {
            children()
        }
    }

    interface Props : RProps {
        var closePopup: () -> Unit
    }
}