package catalog.aspects

import kotlinx.html.js.onClickFunction
import react.RClass
import react.dom.div
import react.dom.i
import react.dom.span
import react.rFunction
import wrappers.table.RTableColumnDescriptor
import wrappers.table.RTableRendererProps

fun checkboxColumn(columnHeader: RClass<RTableRendererProps>, onSaveAspect: (String) -> Unit, onResetAspect: (String) -> Unit) = RTableColumnDescriptor {
    this.accessor = "pending"
    this.Header = columnHeader
    this.width = 55.0
    this.Cell = rFunction("CheckboxCell") { props ->
        if (props.value as Boolean) {
            div(classes = "aspect-management-container") {
                div(classes = "aspect-icon-container") {
                    attrs {
                        onClickFunction = { onResetAspect(props.original.aspect.id) }
                    }
                    i(classes = "fas fa-times-circle circle-red") {}
                }
                div(classes = "aspect-icon-container") {
                    attrs {
                        onClickFunction = { onSaveAspect(props.original.aspect.id) }
                    }
                    i(classes = "far fa-check-circle circle-green") {}
                }
            }
        } else {
            div(classes = "aspect-management-container hidden") {
                div(classes = "aspect-icon-container") {
                    attrs {
                        onClickFunction = { onResetAspect(props.original.aspect.id) }
                    }
                    i(classes = "fas fa-times-circle circle-red") {}
                }
                div(classes = "aspect-icon-container") {
                    attrs {
                        onClickFunction = { onSaveAspect(props.original.aspect.id) }
                    }
                    i(classes = "far fa-check-circle circle-green") {}
                }
            }
        }
    }
}