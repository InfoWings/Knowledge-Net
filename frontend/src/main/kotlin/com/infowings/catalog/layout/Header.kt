package com.infowings.catalog.layout

import com.infowings.catalog.utils.removeAuthRole
import com.infowings.catalog.wrappers.reactRouter
import react.*
import react.dom.div
import react.dom.li
import react.dom.ul

class HeaderProps(var location: String) : RProps

class Header : RComponent<HeaderProps, RState>() {

    override fun RBuilder.render() {
        div(classes = "navbar navbar-default") {
            div(classes = "container-fluid") {
                ul(classes = "nav navbar-nav") {
                    li(classes = if (props.location == "/aspects") "active" else "") {
                        reactRouter.Link {
                            attrs {
                                to = "/aspects"
                            }
                            +"Aspects"
                        }
                    }
                    li(classes = if (props.location == "/units") "active" else "") {
                        reactRouter.Link {
                            attrs {
                                to = "/units"
                            }
                            +"Units"
                        }
                    }
                    li(classes = if (props.location == "/reference") "active" else "") {
                        reactRouter.Link {
                            attrs {
                                to = "/reference"
                            }
                            +"Reference books"
                        }
                    }
                    li(classes = if (props.location == "/history") "active" else "") {
                        reactRouter.Link {
                            attrs {
                                to = "/history"
                            }
                            +"History"
                        }
                    }
                }
                ul(classes = "nav navbar-nav navbar-right") {
                    li {
                        reactRouter.Link {
                            attrs {
                                to = "/"
                                onClick = { removeAuthRole() }
                            }
                            +"Logout"
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.header(handler: RHandler<HeaderProps>) = child(Header::class, handler)