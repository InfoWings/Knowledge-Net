package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.AspectHint
import com.infowings.catalog.common.AspectHintSource
import com.infowings.catalog.common.AspectTree
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.newValueButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.SelectedProperty
import com.infowings.catalog.objects.edit.tree.ValueSlot
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.propertyAspect
import com.infowings.catalog.objects.edit.tree.inputs.valueSlotSelect
import react.RProps
import react.dom.div
import react.rFunction

private fun AspectTree.toHint() =
    AspectHint(
        name = name,
        description = "",
        source = AspectHintSource.ASPECT_NAME.toString(),
        refBookItem = null,
        refBookItemDesc = null,
        property = null,
        subAspectName = null,
        aspectName = null,
        subjectName = name + " " + subjectName,
        parentAspect = null,
        id = id,
        guid = ""
    )


val objectPropertyEditLineFormat = rFunction<ObjectPropertyEditLineFormatProps>("ObjectPropertyEditLineFormat") { props ->
    div(classes = "object-tree-edit__object-property") {
        name(
            className = "object-property__name",
            value = props.name ?: "",
            onChange = props.onNameChanged,
            onCancel = props.onNameChanged,
            disabled = props.disabled
        )
        propertyAspect(
            value = props.aspect?.let {
                val v = it.toHint()
                if (props.selectedProp == null) v else v.copy(name = v.name + ":" + props.selectedProp?.name ?: "")
            },
            onSelect = { hint ->
                when (hint.source) {
                    AspectHintSource.ASPECT_PROPERTY_WITH_ASPECT.name -> {
                        hint.parentAspect?.let {
                            props.onAspectChanged(AspectTree(id = it.id, name = it.name, subjectName = "${it.name} ( ${it.subjectName} )"),
                                SelectedProperty(id = hint.property?.id ?: "", name = hint.subAspectName?:"???", cardinality = hint.property?.cardinality?:""))
                        }
                    } else ->
                    props.onAspectChanged(AspectTree(id = hint.id, name = hint.name, subjectName = hint.subjectName?.drop(hint.name.length)), null)
                }
            },
            onActivity = {
                println("aaa: ${props.selectedProp}")
                if (props.aspect != null) {
                   props.onReselectProperty()
                }
            },
            disabled = props.disabled || props.onAddValue != null // If onAddValue exists, it means the property has id.
            // API does not allow editing aspect, so it is better to just disable the option.
        )

        val selectedProp = props.selectedProp
        if (selectedProp != null) {
            val activeSlots = listOf(ValueSlot.New) + props.valueSlots
                .filter { it.aPropertyId == selectedProp.id }

            if (activeSlots.size > 1) {
                valueSlotSelect(
                    activeSlots,
                    {
                        props.onSlotChanged(it)
                    },
                    props.selectedSlot ?: activeSlots.firstOrNull()
                )
            } else {
                if (props.selectedSlot != ValueSlot.New) {
                    props.onSlotChanged(ValueSlot.New)
                }
            }
        }

        props.onDescriptionChanged?.let {
            if (props.disabled) {
                descriptionComponent(
                    className = "object-input-description",
                    description = props.description
                )
            } else {
                descriptionComponent(
                    className = "object-input-description",
                    description = props.description,
                    onNewDescriptionConfirmed = it,
                    onEditStarted = null
                )
            }
        }
        props.onConfirmCreate?.let {
            submitButtonComponent(it)
        }
        props.onCancel?.let {
            cancelButtonComponent(it)
        }
        props.onRemoveProperty?.let {
            minusButtonComponent(it, true)
        }
        props.onAddValue?.let {
            if (props.editMode) {
                newValueButtonComponent(it)
            }
        }
        props.onCancelProperty?.let {
            cancelButtonComponent(it)
        }
    }
}

interface ObjectPropertyEditLineFormatProps : RProps {
    var name: String?
    var aspect: AspectTree?
    var selectedProp: SelectedProperty?
    var selectedSlot: ValueSlot?
    var valueSlots: List<ValueSlot>
    var description: String?
    var onNameChanged: (String) -> Unit
    var onAspectChanged: (AspectTree, propId: SelectedProperty?) -> Unit
    var onSlotChanged: (valueSlot: ValueSlot?) -> Unit
    var onDescriptionChanged: ((String) -> Unit)?
    var onConfirmCreate: (() -> Unit)?
    var onCancel: (() -> Unit)?
    var onCancelProperty: (() -> Unit)?
    var onReselectProperty: (() -> Unit)
    var onAddValue: (() -> Unit)?
    var onRemoveProperty: (() -> Unit)?
    var disabled: Boolean
    var editMode: Boolean
}
