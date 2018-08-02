package com.infowings.catalog.objects.edit

sealed class EditContextModel

data class EditExistingContextModel(val identity: String) : EditContextModel()
object EditNewChildContextModel : EditContextModel()

data class EditContext(
    val currentContext: EditContextModel?,
    val setContext: (EditContextModel?) -> Unit
)

