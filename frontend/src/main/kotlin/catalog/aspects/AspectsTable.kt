package catalog.aspects

import kotlinx.coroutines.experimental.launch
import org.w3c.dom.events.Event
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import utils.plainObj
import wrappers.Grid

data class GridProps(var stateKey: String = "tree-grid-1",
                     var gridType: String = "tree",
                     var height: Boolean = false,
                     var showTreeRootNode: Boolean = false,
                     var columns: Array<Column>,
                     var data: AspectRoot,
                     var plugins: dynamic,
                     var events: dynamic) : RProps

private val aspectData = AspectRoot(arrayOf(
        AspectNode(1, name = "Name1", measureUnit = "MU1", type = "type1", domain = "domain1", editable = true, children = arrayOf(
                AspectNode(2, name = "Name1.1", measureUnit = "MU1", type = "type1.1", domain = "domain1.1", editable = true, leaf = true, children = emptyArray()),
                AspectNode(3, name = "Name1.2", measureUnit = "MU1", type = "type1.2", domain = "domain1.1", editable = true, leaf = true, children = emptyArray())
        )),
        AspectNode(4, name = "Name2", measureUnit = "MU2", type = "type2", domain = "domain2", editable = true, children = arrayOf(
                AspectNode(5, name = "Name2.1", measureUnit = "MU2", type = "type2", domain = "domain2.1", editable = true, children = arrayOf(
                        AspectNode(6, name = "Name2.1.1", measureUnit = "MU2.1", type = "type2.2", domain = "domain2.1.1", editable = true, leaf = true, children = emptyArray())
                )),
                AspectNode(7, name = "Name2.2", measureUnit = "MU3", type = "type3", domain = "domain", editable = true, leaf = true, children = emptyArray())
        ))
))

fun handleAfter(state: dynamic, e: Event) {
    println(state)
    println(e)
}

class AspectsTable : RComponent<RProps, RState>() {

    override fun RBuilder.render() {
        Grid {
            attrs {
                plainObj {
                    this.stateKey = "aspect-grid"
                    this.gridType = "tree"
                    this.columns = arrayOf(
                            Column(dataIndex = "name", name = "Category", expandable = true, editable = true),
                            Column(dataIndex = "measureUnit", name = "Measurement Unit", expandable = false, editable = true),
                            Column(dataIndex = "type", name = "Type", expandable = false, editable = true),
                            Column(dataIndex = "domain", name = "Domain", expandable = false, editable = true)
                    )
                    this.data = aspectData
                    this.plugins = object {
                        val GRID_ACTIONS = object {
                            val iconCls = "action-icon"
                            val menu = arrayOf(
                                    object {}
                            )
                        }
                        val SELECTION_MODEL = object {
                            val mode = "single"
                        }
                        val PAGER = object {
                            val enable = false
                        }
                        val BULK_ACTIONS = object {
                            val enable = false
                        }
                        val EDITOR = object {
                            val type = "inline"
                            val enabled = true
                            val focusOnEdit = true
                        }
                    }
                    this.events = object {
                        fun HANDLE_AFTER_INLINE_EDITOR_SAVE(state: dynamic, e: Event) {
                            println(state)
                            println(e)
                        }
                    }
                }
            }
        }
    }
}