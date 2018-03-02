package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import react.RClass
import react.dom.input
import react.rFunction

fun aspectColumn(accessor: String, headerName: String, cell: RClass<RTableRendererProps>? = null) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = rFunction("AspectColumn") { +headerName }
            className = "aspect-cell"
            cell?.let {
                this.Cell = cell
            }
        }

fun aspectCell(onFieldChanged: (data: AspectData, value: String) -> Unit) = rFunction<RTableRendererProps>("AspectCell") { props ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = props.value?.toString() ?: ""
            onChangeFunction = { onFieldChanged(props.original.aspect as AspectData, it.asDynamic().target.value) }
        }
    }
}

fun aspectPropertyAspectCell(onFieldChanged: (data: AspectData, value: String) -> Unit) = rFunction<RTableRendererProps>("AspectCell") { props ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = props.value?.toString() ?: ""
            onChangeFunction = { onFieldChanged(props.original.aspect as AspectData, it.asDynamic().target.value) }
        }
    }
}

fun aspectPropertyCell(onPropertyChanged: (index: Int, value: String) -> Unit) = rFunction<RTableRendererProps>("AspectPropertyCell") { rTableRendererProps ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = rTableRendererProps.value?.toString() ?: ""
            onChangeFunction = { onPropertyChanged(rTableRendererProps.index, it.asDynamic().target.value) }
        }
    }
}


