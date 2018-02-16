package catalog.aspects

import com.infowings.common.UserRole
import kotlinx.coroutines.experimental.launch
import org.w3c.dom.get
import react.*
import react.dom.p
import utils.getResponseText
import kotlin.browser.localStorage


class RMessageState(var message: String) : RState

class TestMessageContainer : RComponent<RProps, RMessageState>() {

    init {
        state.message = ""
    }

    override fun componentDidMount() {
        launch {
            val newMessage = when (localStorage["auth-role"]) {
                UserRole.ADMIN.name -> getResponseText("/api/admin")
                UserRole.POWERED_USER.name -> getResponseText("/api/powereduser")
                UserRole.USER.name -> getResponseText("/api/user")
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
