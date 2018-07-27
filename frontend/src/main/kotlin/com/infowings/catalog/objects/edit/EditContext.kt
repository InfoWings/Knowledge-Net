package com.infowings.catalog.objects.edit

import react.createContext

sealed class EditContextModel

data class EditExistingContextModel(val identity: String) : EditContextModel()
data class EditNewChildContextModel(val parentIdentity: String) : EditContextModel()

data class EditContext(
    val currentContext: EditContextModel?,
    val setContext: ((EditContextModel) -> Unit)?
)

val editContext = createContext(defaultValue = EditContext(null, null))