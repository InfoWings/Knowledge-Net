package catalog.aspects

import react.*
import react.dom.span
import wrappers.table.*
import kotlin.js.json

fun headerComponent(columnName: String) = rFunction<RTableRendererProps>("AspectHeader") {
    span {
        +columnName
    }
}

val cellComponent = rFunction<RTableRendererProps>("LoggingCell") { rTableRendererProps ->
    +rTableRendererProps.value.toString()
}

fun aspectColumn(accessor: String, header: RClass<RTableRendererProps>, cell: RClass<RTableRendererProps>) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = header
            this.Cell = cell
        }

private val aspectData = arrayOf(
        json(
                "category" to "Category",
                "measureUnit" to "measureUnit",
                "type" to "type",
                "domain" to "domain"
        )
)

class AspectsTable : RComponent<RProps, RState>() {


    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                        aspectColumn("category", headerComponent("Category"), cellComponent),
                        aspectColumn("measureUnit", headerComponent("Measure Unit"), cellComponent),
                        aspectColumn("type", headerComponent("Type"), cellComponent),
                        aspectColumn("domain", headerComponent("Domain"), cellComponent)
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