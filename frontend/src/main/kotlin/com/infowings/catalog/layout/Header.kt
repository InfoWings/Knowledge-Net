package com.infowings.catalog.layout

import com.infowings.catalog.utils.removeAuthRole
import com.infowings.catalog.wrappers.History
import com.infowings.catalog.wrappers.blueprint.*
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
                        active = props.location == "/aspects"
                        onClick = { props.history.push("/aspects") }
                        text = buildElement { +"Aspects" }
                    }
                }
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/measures"
                        onClick = { props.history.push("/measures") }
                        text = buildElement { +"Measures" }
                    }
                }
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/subjects"
                        onClick = { props.history.push("/subjects") }
                        text = buildElement { +"Subjects" }
                    }
                }
                Button {
                    attrs {
                        className = "pt-minimal"
                        active = props.location == "/reference"
                        onClick = { props.history.push("/reference") }
                        text = buildElement { +"Reference Books" }
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
                            props.history.push("/aspects")
                        }
                        text = buildElement { +"Logout" }
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