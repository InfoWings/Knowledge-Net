@file:JsModule("react-sortable-tree")

package wrappers

import react.RClass
import react.RProps

external interface TreeData {
    var title: String
    var children: Array<TreeData>
}

external interface SortableTreeProps : RProps {
    var treeData: Array<TreeData>
    var onChange: (treeData: TreeData) -> Unit
}

external val ReactSortableTree: dynamic = definedExternally

external interface Default {
    var DecoratedComponent: RClass<SortableTreeProps>
}

@JsName("default")
external val default2: Default = definedExternally