package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectHint
import com.infowings.catalog.common.AspectHintSource
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.wrappers.react.asReactElement
import com.infowings.catalog.wrappers.react.label
import react.ReactElement
import react.buildElement
import react.dom.div
import react.dom.span

fun elemByAspectName(v: String): ReactElement {
    return buildElement {
        label {
            +v
        }
    } ?: "???".asReactElement()
}

fun elemByAspectDesc(v: String, description: String?): ReactElement {
    return buildElement {
        div {
            descriptionComponent(
                className = "aspect-tree-view--description-icon",
                description = description
            )

            span {
                +" "
            }

            label {
                +v
            }
        }
    } ?: "???".asReactElement()
}


fun elemByRefBookValue(aspectName: String, value: String): ReactElement {
    return buildElement {
        label {
            +"$aspectName:[$value]"
        }
    } ?: "???".asReactElement()
}

fun elemByRefBookDesc(aspectName: String, value: String, description: String?): ReactElement {
    return buildElement {
        div {
            descriptionComponent(
                className = "aspect-tree-view--description-icon",
                description = description
            )

            span {
                +" "
            }

            label {
                +"$aspectName:[$value]"
            }
        }
    } ?: "???".asReactElement()

}

fun elemByProperty(aspectName: String, propName: String, subAspectName: String): ReactElement {
    return buildElement {
        label {
            +"$aspectName.$propName-$subAspectName"
        }
    } ?: "???".asReactElement()
}

fun AspectHint.listEntry() = when (AspectHintSource.valueOf(source)) {
    AspectHintSource.ASPECT_NAME -> elemByAspectName(name)
    AspectHintSource.ASPECT_DESCRIPTION -> elemByAspectDesc(name, description)
    AspectHintSource.REFBOOK_NAME -> elemByRefBookValue(name, refBookItem ?: "")
    AspectHintSource.REFBOOK_DESCRIPTION -> elemByRefBookDesc(name, refBookItem ?: "???", refBookItemDesc)
    AspectHintSource.ASPECT_PROPERTY_WITH_ASPECT -> elemByProperty(name, propertyName ?: "???", subAspectName ?: "???")
    else -> elemByAspectName(name)
}