package catalog.auth

import com.infowings.common.UserDto
import kotlinx.coroutines.experimental.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.get
import react.*
import react.dom.*
import utils.login
import wrappers.RouteSuppliedProps
import wrappers.reactRouter
import kotlin.browser.localStorage
import kotlinx.serialization.json.JSON as KJSON

class AuthState(var authorized: Boolean = false, var wrongAuth: Boolean = false) : RState

class AuthComponent : RComponent<RouteSuppliedProps, AuthState>() {

    lateinit var loginInput: HTMLInputElement
    lateinit var passwordInput: HTMLInputElement

    fun logIn(e: Event) {
        e.preventDefault()
        launch {
            val success = login(UserDto(loginInput.value, passwordInput.value))
            setState {
                if (success) {
                    authorized = localStorage["auth-role"] != null
                    if (authorized) {
                        props.history.push("/")
                    }
                } else {
                    wrongAuth = true
                }
            }
        }
    }

    override fun componentDidMount() {
        setState {
            authorized = localStorage["auth-role"] != null
            wrongAuth = false
        }
    }

    override fun RBuilder.render() {
        if (state.authorized) {
            reactRouter.Redirect { attrs.to = "/" }
        } else {
            form(classes = "auth-form-iwings") {
                attrs.onSubmitFunction = ::logIn
                div("form-group") {
                    label {
                        attrs.htmlFor = "loginInput"
                        +"Login"
                    }
                    input(InputType.text, classes = "form-control") {
                        attrs.id = "loginInput"
                        ref { loginInput = it }
                    }
                }
                div("form-group") {
                    label {
                        attrs.htmlFor = "passwordInput"
                        +"Password"
                    }
                    input(InputType.password, classes = "form-control") {
                        attrs.id = "passwordInput"
                        ref { passwordInput = it }
                    }
                }
                button(type = ButtonType.submit, classes = "btn btn-primary") {
                    +"Sign in"
                }
                if (state.wrongAuth) {
                    p {
                        +"Wrong login and password pair"
                    }
                }
            }
        }
    }
}
