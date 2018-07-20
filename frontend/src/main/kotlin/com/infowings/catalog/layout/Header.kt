package com.infowings.catalog.layout

import com.infowings.catalog.utils.removeAuthRole
import com.infowings.catalog.wrappers.History
import com.infowings.catalog.wrappers.blueprint.*
import com.infowings.catalog.wrappers.react.asReactElement
import com.infowings.catalog.wrappers.reactRouter
import react.*

class Header : RComponent<Header.Props, RState>() {

    override fun RBuilder.render() {
        Navbar {
            NavbarGroup {
                attrs.align = Alignment.LEFT
                NavbarHeading {
                    +"Knowledge Net"
                }
                NavbarDivider {}
                reactRouter.Link {
                    attrs {
                        className = "pt-button pt-minimal${if (props.location == "/objects") " pt-active" else ""}"
                        role = "button"
                        to = "/objects"
                    }
                    +"Objects"
                }
                reactRouter.Link {
                    attrs {
                        className = "pt-button pt-minimal${if (props.location == "/aspects") " pt-active" else ""}"
                        role = "button"
                        to = "/aspects"
                    }
                    +"Aspects"
                }
                reactRouter.Link {
                    attrs {
                        className = "pt-button pt-minimal${if (props.location == "/measures") " pt-active" else ""}"
                        role = "button"
                        to = "/measures"
                    }
                    +"Measures"
                }
                reactRouter.Link {
                    attrs {
                        className = "pt-button pt-minimal${if (props.location == "/subjects") " pt-active" else ""}"
                        role = "button"
                        to = "/subjects"
                    }
                    +"Subjects"
                }
                reactRouter.Link {
                    attrs {
                        className = "pt-button pt-minimal${if (props.location == "/reference") " pt-active" else ""}"
                        role = "button"
                        to = "/reference"
                    }
                    +"Reference Books"
                }
                reactRouter.Link {
                    attrs {
                        className = "pt-button pt-minimal${if (props.location == "/history") " pt-active" else ""}"
                        role = "button"
                        to = "/history"
                    }
                    +"History"
                }
            }
            NavbarGroup {
                attrs.align = Alignment.RIGHT
                Button {
                    attrs {
                        className = "pt-minimal"
                        onClick = {
                            removeAuthRole()
                            props.history.push("/objects")
                        }
                        text = "Logout".asReactElement()
                    }
                }
            }

        }
    }

    interface Props : RProps {
        var location: String
        var history: History
    }
}

fun RBuilder.header(block: RHandler<Header.Props>) {
    child(Header::class, block)
}