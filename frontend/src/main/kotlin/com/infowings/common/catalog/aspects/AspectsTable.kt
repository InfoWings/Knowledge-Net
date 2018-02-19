package com.infowings.common.catalog.aspects

import com.infowings.common.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.common.catalog.wrappers.table.RTableRendererProps
import com.infowings.common.catalog.wrappers.table.ReactTable
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.events.InputEvent
import react.*
import react.dom.input
import react.dom.span

fun headerComponent(columnName: String) = rFunction<RTableRendererProps>("AspectHeader") {
    span {
        +columnName
    }
}

val cellComponent = rFunction<RTableRendererProps>("LoggingCell") { rTableRendererProps ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = rTableRendererProps.value.toString()
            onChangeFunction = { e ->
                val inputEvent = e as InputEvent
                console.log(inputEvent.data)
                console.log(value)
                console.log(rTableRendererProps.original)
            }
        }
    }
}

fun aspectColumn(accessor: String, header: RClass<RTableRendererProps>, cell: RClass<RTableRendererProps>? = null) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = header
            cell?.let {
                this.Cell = cell
            }
        }

data class Aspect(
        val id: String,
        val name: String,
        val measureUnit: String,
        val domain: String,
        val baseType: String
)

private val aspectData = arrayOf(
        Aspect("0", "name", "measureUnit", "domain", "baseType"),
        Aspect("1", "name", "measureUnit", "domain", "baseType"),
        Aspect("2", "name", "measureUnit", "domain", "baseType"),
        Aspect("3", "name", "measureUnit", "domain", "baseType"),
        Aspect("4", "name", "measureUnit", "domain", "baseType"),
        Aspect("5", "name", "measureUnit", "domain", "baseType"),
        Aspect("6", "name", "measureUnit", "domain", "baseType"),
        Aspect("7", "name", "measureUnit", "domain", "baseType"),
        Aspect("8", "name", "measureUnit", "domain", "baseType"),
        Aspect("9", "name", "measureUnit", "domain", "baseType"),
        Aspect("10", "name", "measureUnit", "domain", "baseType"),
        Aspect("11", "name", "measureUnit", "domain", "baseType")
)

/**
 * Use as: child(AspectsTable::class) {}
 */
class AspectsTable : RComponent<RProps, RState>() {

    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                        aspectColumn("name", headerComponent("Name"), cellComponent),
                        aspectColumn("measureUnit", headerComponent("Measure Unit")),
                        aspectColumn("domain", headerComponent("Domain")),
                        aspectColumn("baseType", headerComponent("Base Type"))
                )
                data = aspectData
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false

            }
        }
    }
}