package com.infowings.catalog.errors

import com.infowings.catalog.utils.ApiException
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.utils.NotModifiedException
import com.infowings.catalog.utils.ServerException
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.blueprint.Position
import com.infowings.catalog.wrappers.blueprint.Toaster
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinext.js.jsObject
import react.ReactElement

val errorToaster = Toaster.create(jsObject {
    position = Position.TOP_RIGHT
})

fun showError(apiException: ApiException) {
    errorToaster.show(
        jsObject {
            icon = "warning-sign"
            intent = Intent.DANGER
            message = apiException.toastMessage()
            timeout = DEFAULT_TIMEOUT_MILLISECONDS
        },
        apiException.timestamp?.toString()
    )
}

private const val DEFAULT_TIMEOUT_MILLISECONDS = 9000

private fun ApiException.toastMessage(): ReactElement {
    return when (this) {
        is ServerException -> "Oops, something went wrong, changes weren't saved".asReactElement()
        is BadRequestException -> this.message.asReactElement()
        is NotModifiedException -> "Entity was not modified".asReactElement()
    }
}