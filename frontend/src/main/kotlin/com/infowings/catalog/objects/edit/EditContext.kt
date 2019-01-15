package com.infowings.catalog.objects.edit

sealed class EditContextModel

data class EditExistingContextModel(val identity: String) : EditContextModel() {
    override fun toString() = "(EditExistingContextModel: $identity)"
}
object EditNewChildContextModel : EditContextModel() {
    override fun toString() = "(EditNewChildContextModel)"
}

data class EditContext(
    val currentContext: EditContextModel?,
    val setContext: (EditContextModel?) -> Unit
)

