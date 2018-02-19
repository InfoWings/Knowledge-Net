package com.infowings.common.catalog.aspects

import com.infowings.common.catalog.UserRole
import com.infowings.common.catalog.utils.getRequest
import kotlinx.coroutines.experimental.launch
import org.w3c.dom.get
import react.*
import react.dom.p
import kotlin.browser.localStorage


class RMessageState(var message: String) : RState

class TestMessageContainer : RComponent<RProps, RMessageState>() {

    init {
        state.message = ""
    }

    override fun componentDidMount() {
        launch {
            val newMessage = when (localStorage["auth-role"]) {
                UserRole.ADMIN.name -> getRequest("/api/admin")
                UserRole.POWERED_USER.name -> getRequest("/api/powereduser")
                UserRole.USER.name -> getRequest("/api/user")
                else -> throw RuntimeException("Illegal state")
            }
            setState {
                message = newMessage
            }
        }
    }

    override fun RBuilder.render() {
        p {
            +state.message
        }
    }
}
