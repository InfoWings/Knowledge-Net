package com.infowings.catalog.users

import com.infowings.catalog.common.User
import com.infowings.catalog.wrappers.blueprint.EditableText
import kotlinext.js.require
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div

class UsersListComponent : RComponent<UsersListComponent.Props, UsersListComponent.State>() {

    companion object {
        init {
            require("styles/users-list.scss")
        }
    }

    override fun RBuilder.render() {

        div(classes = "users-list") {
            props.users.forEach { user ->
                div(classes = "users-list--user") {

                    attrs {
                        key = user.username
                    }

                    EditableText {
                        attrs {
                            className = "users-list--username"
                            defaultValue = user.username
                            disabled = true
                        }
                    }

                    EditableText {
                        attrs {
                            className = "users-list--role"
                            defaultValue = user.role.name
                            disabled = true
                        }
                    }

                    EditableText {
                        attrs {
                            className = "users-list--blocked"
                            defaultValue = if (user.blocked) "blocked" else "active"
                            disabled = true
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var users: Set<User>
    }

    interface State : RState {
        var errorMessages: List<String>
    }
}