package core.process.tree


class TreeData(children: TreeChild) {
    var root = TreeChild(-1, "null", children = arrayOf(children))
}

class TreeChild(var id: Int,
                var category: String,
                var editable: Boolean = true,
                var children: Array<TreeChild> = arrayOf()) {

    var parentId: Int? = null

    init {
        this.children.forEach {
            it.parentId = id;
        }
    }
}

data class Column(var dataIndex: String, var name: String, var expandable: Boolean = false)
