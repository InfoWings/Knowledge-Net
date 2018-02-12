package catalog.home

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div
import react.dom.h1
import react.dom.li
import react.dom.ul
import kotlin.browser.localStorage

class HomeComponent : RComponent<RProps, RState>() {

    override fun RBuilder.render() {
        div("navbar navbar-default") {
            div("container-fluid") {
                ul("nav navbar-nav navbar-right") {
                    li {
                        wrappers.reactRouter.Link {
                            attrs {
                                to = "/"
                                onClick = { localStorage.removeItem("auth-role"); }
                            }
                            +"Logout"
                        }
                    }
                }
            }
        }
        h1 { +"Hi there" }
        child(TestMessageContainer::class) {}
    }
}
