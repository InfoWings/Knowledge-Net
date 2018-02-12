package wrappers

import react.RBuilder
import react.RClass
import react.RComponent
import react.RState


@JsModule("react-sortable-tree")
external val reactLib: ReactLib = definedExternally

//@JsName("ReactSortableTree")
//external val ReactSortableTree : RClass<SortableTreeProps>

external interface ReactLib {
    var SortableTree: RClass<SortableTreeProps>
}

class SortableTree(props: SortableTreeProps) : RComponent<SortableTreeProps, RState>(props) {
    override fun RBuilder.render() {
        println(reactLib)
        console.log(reactLib)
        val current = reactLib
        val c2 = ReactSortableTree
        val c3 = default2
        console.log(c2)
        console.log(c3)
        c3.DecoratedComponent {
            attrs {
                treeData = props.treeData
                onChange = props.onChange
            }
        }
    }
}

fun RBuilder.sortableTree(props: SortableTreeProps) = child(SortableTree::class) {
    attrs {
        treeData = props.treeData
        onChange = props.onChange
    }
}