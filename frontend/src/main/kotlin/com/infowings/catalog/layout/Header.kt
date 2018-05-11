package com.infowings.catalog.layout

import com.infowings.catalog.utils.removeAuthRole
import com.infowings.catalog.wrappers.History
import com.infowings.catalog.wrappers.blueprint.*
import com.infowings.catalog.wrappers.react.asReactElement
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
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/objects"
                        onClick = { props.history.push("/objects") }
                        text = "Objects".asReactElement()
                    }
                }
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/aspects"
                        onClick = { props.history.push("/aspects") }
                        text = "Aspects".asReactElement()
                    }
                }
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/measures"
                        onClick = { props.history.push("/measures") }
                        text = "Measures".asReactElement()
                    }
                }
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/subjects"
                        onClick = { props.history.push("/subjects") }
                        text = "Subjects".asReactElement()
                    }
                }
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/reference"
                        onClick = { props.history.push("/reference") }
                        text = "Reference Books".asReactElement()
                    }
                }
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/history"
                        onClick = { props.history.push("/history") }
                        text = "History".asReactElement()
                    }
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