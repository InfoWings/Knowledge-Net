package core.process.tree

import com.infowings.common.catalog.utils.plainObj
import react.RBuilder
import react.RProps
import wrappers.Grid


data class GridProps(var stateKey: String = "tree-grid-1",
                     var gridType: String = "tree",
                     var showTreeRootNode: Boolean = false,
                     var columns: Array<Column>,
                     var data: TreeData,
                     var plugins: dynamic) : RProps

fun RBuilder.tree() {
    Grid {
        attrs {
            plainObj {
                this.stateKey = "tree-grid-1"
                this.columns = arrayOf(Column(dataIndex = "category", name = "Category", expandable = true))
                this.data = TreeData(TreeChild(1, category = "1", children = arrayOf(TreeChild(2, "2",
                        children = arrayOf(TreeChild(22, category = "3"))))))
                this.plugins = object {
                    val GRID_ACTIONS = null
                    val SELECTION_MODEL = object {
                        val mode = "single"
                    }
                    val PAGER = object {
                        val enable = false
                    }
                    val BULK_ACTIONS = object {
                        val enable = false
                    }
                }
                this.gridType = "tree"
            }
        }
    }
}


//val dataS = object {
//    val root = object {
//        val id = -1
//        val parentId= null
//        val children = arrayOf(
//                object {
//                    val id = 1
//                    val parentId = -1
//                    val name = "Category 1"
//                    val category = "asdd"
//                    val editable = true
//                    var children = arrayOf(
//                        object {
//                            val id = 3
//                            val parentId = 1
//                            val name = "Category 3"
//                            val category = "asdd"
//                            val editable = true
//                        }
//                    )
//                },
//                object {
//                    val id = 2
//                    val parentId = -1
//                    val name = "Category 2"
//                    val category = "asdd"
//                    val editable = true
//                })
//    }
//}