package catalog.aspects


class AspectRoot(children: Array<AspectNode>) {
    var root = AspectNode(-1, "null", "null", "null", "null", children = children)
}

class AspectNode(var id: Int,
                 var name: String,
                 var measureUnit: String,
                 var type: String,
                 var domain: String,
                 var editable: Boolean = true,
                 var leaf: Boolean = false,
                 var children: Array<AspectNode> = arrayOf()) {

    var parentId: Int? = null

    init {
        this.children.forEach {
            it.parentId = id;
        }
    }
}

data class Column(
        var dataIndex: String,
        var name: String,
        var expandable: Boolean = false,
        var editable: Boolean = true,
        var editor: String = "<input type=\"text\" required/>"
)
