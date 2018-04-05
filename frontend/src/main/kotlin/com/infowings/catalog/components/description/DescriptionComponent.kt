package com.infowings.catalog.components.description

import react.RBuilder

fun RBuilder.descriptionComponent(className: String?, description: String?) =
    descriptionTooltip(className, description)

fun RBuilder.descriptionComponent(
    className: String?,
    description: String?,
    onNewDescriptionConfirmed: (String) -> Unit,
    onEditStarted: (() -> Unit)?
) = child(EditableDescriptionComponent::class) {
    attrs.className = className
    attrs.description = description
    attrs.onNewDescriptionConfirmed = onNewDescriptionConfirmed
    attrs.onEditStarted = onEditStarted
}