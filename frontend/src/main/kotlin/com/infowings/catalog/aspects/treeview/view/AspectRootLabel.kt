package com.infowings.catalog.aspects.treeview.view

import com.infowings.catalog.common.AspectData
import react.*

/**
 * View component. Draws label (or placeholder) for aspect in top level of aspect tree.
 */
class AspectRootLabel : RComponent<AspectRootLabel.Props, RState>() {

    override fun RBuilder.render() {
        val className = if (props.selected) "aspect-tree-view--label__selected" else null
        if (!props.aspect.name.isNullOrEmpty() || !props.aspect.measure.isNullOrEmpty()
                || !props.aspect.domain.isNullOrEmpty() || !props.aspect.baseType.isNullOrEmpty()) {
            aspectLabel(
                className = className,
                aspectName = props.aspect.name ?: "",
                aspectMeasure = props.aspect.measure ?: "",
                aspectDomain = props.aspect.domain ?: "",
                aspectBaseType = props.aspect.baseType ?: "",
                aspectSubjectName = props.aspect.subject?.name ?: "Global",
                onClick = { props.onClick(props.aspect.id) }
            )
        } else {
            placeholderAspectLabel(className)
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var onClick: (String?) -> Unit
        var selected: Boolean
    }
}

fun RBuilder.aspectRootLabel(block: RHandler<AspectRootLabel.Props>) = child(AspectRootLabel::class, block)