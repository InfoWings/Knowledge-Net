package com.infowings.catalog.users

import com.infowings.catalog.common.User
import com.infowings.catalog.layout.header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

class UsersPage : RComponent<RouteSuppliedProps, UsersPage.State>() {

    override fun State.init() {
        users = emptySet()
        loading = true
    }

    override fun componentDidMount() {
        launch {
            val allUsers = getAllUsers().users
            setState {
                users = allUsers
                loading = false
            }
        }
    }

    override fun RBuilder.render() {
        header {
            attrs {
                location = props.location.pathname
                history = props.history
            }
        }

        child(UsersListComponent::class) {
            attrs {
                users = state.users
            }
        }
    }

    interface State : RState {
        var users: Set<User>
        var loading: Boolean
    }
}