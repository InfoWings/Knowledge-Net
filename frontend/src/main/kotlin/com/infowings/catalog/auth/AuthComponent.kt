package com.infowings.catalog.auth

import com.infowings.catalog.common.UserCredentials
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.utils.getAuthorizationRole
import com.infowings.catalog.utils.login
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Label
import com.infowings.catalog.wrappers.reactRouter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.form
import react.dom.input
import react.dom.p
import kotlinx.serialization.json.JSON as KJSON

class AuthState(var authorized: Boolean = false, var wrongAuth: Boolean = false) : RState

class AuthComponent : RComponent<RouteSuppliedProps, AuthState>(), JobCoroutineScope by JobSimpleCoroutineScope() {

    lateinit var loginInput: HTMLInputElement
    lateinit var passwordInput: HTMLInputElement

    fun logIn(e: Event) {
        e.preventDefault()
        launch {
            val success = login(UserCredentials(loginInput.value, passwordInput.value))
            setState {
                if (success) {
                    authorized = getAuthorizationRole() != null
                    if (authorized) {
                        props.history.push("/")
                    }
                } else {
                    wrongAuth = true
                }
            }
        }
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun componentDidMount() {
        job = Job()
        setState {
            authorized = getAuthorizationRole() != null
            wrongAuth = false
        }
    }

    override fun RBuilder.render() {
        if (state.authorized) {
            reactRouter.Redirect { attrs.to = "/" }
        } else {
            div(classes = "iwings-auth-form") {
                form(classes = "iwings-auth-form--form") {
                    attrs.onSubmitFunction = ::logIn
                    Label {
                        attrs {
                            text = buildElement { +"Login" }
                        }
                        input(InputType.text, classes = "bp3-input") {
                            attrs.placeholder = "Login"
                            ref { loginInput = it }
                        }
                    }
                    Label {
                        attrs {
                            text = buildElement { +"Password" }
                        }
                        input(InputType.password, classes = "bp3-input") {
                            attrs.placeholder = "Password"
                            ref { passwordInput = it }
                        }
                    }
                    Button {
                        attrs.className = "bp3-fill"
                        attrs.type = InputType.submit.realValue
                        +"Sign In"
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
}
