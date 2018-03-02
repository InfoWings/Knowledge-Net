package com.infowings.catalog.aspects

import kotlinx.html.js.onClickFunction
import react.RClass
import react.dom.div
import react.dom.i
import react.rFunction
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps

/**
 * Component that represents a couple of icons (discard changes made to aspect or make request to server and save changes)
 * If aspect is not changed, icons should not be displayed
 */
fun controlsColumn(columnHeader: RClass<RTableRendererProps>, onSaveAspect: (String) -> Unit, onResetAspect: (String) -> Unit) = RTableColumnDescriptor {
    this.accessor = "pending"
    this.Header = columnHeader
    this.width = 55.0
    this.Cell = rFunction("CheckboxCell") { props ->
        div(classes = "aspect-management-container${if (props.value as Boolean) "" else " hidden"}") {
            div(classes = "aspect-icon-container") {
                attrs.onClickFunction = { onResetAspect(props.original.aspect.id) }
                i(classes = "fas fa-times-circle circle-red") {}
            }
            div(classes = "aspect-icon-container") {
                attrs.onClickFunction = { onSaveAspect(props.original.aspect.id) }
                i(classes = "far fa-check-circle circle-green") {}
            }
        }
    }
}

/**
 * Component that represents a couple of icons (discard changes made to aspect or make request to server and save changes)
 * in the aspect subtable (controls are related to aspect, bound with property)
 * If aspect is not changed, icons should not be displayed
 *
 * The only difference with above column descriptor is the Cell component that gets aspect id from AspectPropertyRow,
 * not the AspectRow
 */
fun controlsPropertyColumn(onSaveAspect: (String) -> Unit, onResetAspect: (String) -> Unit) = RTableColumnDescriptor {
    this.accessor = "pending"
    this.width = 55.0
    this.Cell = rFunction("CheckboxCell") { props ->
        div(classes = "aspect-management-container${if (props.value as Boolean) "" else " hidden"}") {
            div(classes = "aspect-icon-container") {
                attrs.onClickFunction = { onResetAspect(props.original.property.aspectId) }
                i(classes = "fas fa-times-circle circle-red") {}
            }
            div(classes = "aspect-icon-container") {
                attrs.onClickFunction = { onSaveAspect(props.original.property.aspectId) }
                i(classes = "far fa-check-circle circle-green") {}
            }
        }
    }
}

/**
 * Component that represents a couple of icons (discard changes made to aspect or make request to server and save changes)
 * If aspect is not changed, icons should not be displayed
 */
fun controlsPropertyColumn(onSaveAspect: (String) -> Unit, onResetAspect: (String) -> Unit) = RTableColumnDescriptor {
    this.accessor = "pending"
    this.width = 55.0
    this.Cell = rFunction("CheckboxCell") { props ->
        if (props.value as Boolean) {
            div(classes = "aspect-management-container") {
                div(classes = "aspect-icon-container") {
                    attrs.onClickFunction = { onResetAspect(props.original.property.aspectId) }
                    i(classes = "fas fa-times-circle circle-red") {}
                }
                div(classes = "aspect-icon-container") {
                    attrs.onClickFunction = { onSaveAspect(props.original.property.aspectId) }
                    i(classes = "far fa-check-circle circle-green") {}
                }
            }
        } else {
            div(classes = "aspect-management-container hidden") {
                div(classes = "aspect-icon-container") {
                    attrs.onClickFunction = { onResetAspect(props.original.property.aspectId) }
                    i(classes = "fas fa-times-circle circle-red") {}
                }
                div(classes = "aspect-icon-container") {
                    attrs.onClickFunction = { onSaveAspect(props.original.property.aspectId) }
                    i(classes = "far fa-check-circle circle-green") {}
                }
            }
        }
    }
}