package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.aspects.treeview.view.placeholderPropertyLabel
import com.infowings.catalog.aspects.treeview.view.propertyLabel
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import react.*

class AspectPropertyLabel : RComponent<AspectPropertyLabel.Props, RState>() {

    override fun RBuilder.render() {
        val selectedClass = when {
            props.aspectSelected -> "aspect-tree-view--label__aspect-selected"
            props.propertySelected -> "aspect-tree-view--label__property-selected"
            else -> ""
        }
        if (props.aspectProperty.name != "" || props.aspectProperty.cardinality != "" || props.aspectProperty.aspectId != "") {
            propertyLabel(
                    className = selectedClass,
                    aspectPropertyName = props.aspectProperty.name,
                    aspectPropertyCardinality = props.aspectProperty.cardinality,
                    aspectName = props.aspect?.name ?: "",
                    aspectMeasure = props.aspect?.measure ?: "",
                    aspectDomain = props.aspect?.domain ?: "",
                    aspectBaseType = props.aspect?.baseType ?: "",
                    onClick = props.onClick
            )
        } else {
            placeholderPropertyLabel(className = selectedClass)
        }
    }

    interface Props : RProps {
        var aspectProperty: AspectPropertyData
        var aspect: AspectData?
        var onClick: () -> Unit
        var propertySelected: Boolean
        var aspectSelected: Boolean
    }
}

fun RBuilder.aspectPropertyLabel(block: RHandler<AspectPropertyLabel.Props>) = child(AspectPropertyLabel::class, block)