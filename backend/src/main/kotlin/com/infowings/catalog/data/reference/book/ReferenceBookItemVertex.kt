package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.RefBookNodeDescriptor
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex
import hasIncomingEdges

fun OVertex.toReferenceBookItemVertex(): ReferenceBookItemVertex {
    this.checkClass(OrientClass.REFBOOK_ITEM)
    return ReferenceBookItemVertex(this)
}

const val ASPECT_REFERENCE_BOOK_EDGE = "AspectReferenceBookEdge"
const val REFERENCE_BOOK_ITEM_VERTEX = "ReferenceBookItemVertex"
const val REFERENCE_BOOK_CHILD_EDGE = "ReferenceBookChildEdge"
const val REFERENCE_BOOK_ROOT_EDGE = "ReferenceBookRootEdge"

enum class RefBookField(val extName: String) {
    VALUE("value"),
    DESCRIPTION("description"),
    GUID("guid"),
    DELETED("deleted"),
    LINK_CHILDREN("children"),
    LINK_ASPECT("aspect"),
    LINK_PARENT("parent"),
    LINK_ROOT("root"),
}

class ReferenceBookItemVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = REFERENCE_BOOK_ITEM_VERTEX
    val edgeName = REFERENCE_BOOK_CHILD_EDGE

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            RefBookField.VALUE.extName to asStringOrEmpty(value),
            RefBookField.DESCRIPTION.extName to asStringOrEmpty(description),
            RefBookField.GUID.extName to asStringOrEmpty(guid)
        ),
        links = mapOf(
            RefBookField.LINK_CHILDREN.extName to children.map { it.identity },
            RefBookField.LINK_ASPECT.extName to listOfNotNull(aspect).map { it.identity },
            RefBookField.LINK_PARENT.extName to listOfNotNull(parent).map { it.identity },
            RefBookField.LINK_ROOT.extName to listOfNotNull(root).map { it.identity }
        )
    )

    val aspect: AspectVertex?
        get() = getVertices(ODirection.IN, ASPECT_REFERENCE_BOOK_EDGE)
            .map { it.toAspectVertex() }
            .filterNot { it.deleted }
            .firstOrNull()

    var value: String
        get() = this[RefBookField.VALUE.extName]
        set(value) {
            this[RefBookField.VALUE.extName] = value
        }

    var description: String?
        get() = this[RefBookField.DESCRIPTION.extName]
        set(value) {
            this[RefBookField.DESCRIPTION.extName] = value
        }

    var deleted: Boolean
        get() = this[RefBookField.DELETED.extName] ?: false
        set(value) {
            this[RefBookField.DELETED.extName] = value
        }

    val children: List<ReferenceBookItemVertex>
        get() = getVertices(ODirection.OUT, edgeName).map { it.toReferenceBookItemVertex() }

    val root: ReferenceBookItemVertex?
        get() = getVertices(ODirection.IN, REFERENCE_BOOK_ROOT_EDGE).firstOrNull()?.toReferenceBookItemVertex()

    /**
     * Return parent ReferenceBookItemVertex or null
     */
    val parent: ReferenceBookItemVertex?
        get() = getVertices(ODirection.IN, edgeName).firstOrNull()?.toReferenceBookItemVertex()

    val guid: String?
        get() = guid(OrientEdge.GUID_OF_REFBOOK_ITEM)

    fun toReferenceBookItem(): ReferenceBookItem {
        val children = children.map { it.toReferenceBookItem() }
        return ReferenceBookItem(id, value, description, children, deleted, version, guid)
    }

    fun toNodeDescriptor(): RefBookNodeDescriptor =
        RefBookNodeDescriptor(id, value, description)


    fun isLinkedBy() = hasIncomingEdges(OBJECT_VALUE_DOMAIN_ELEMENT_EDGE)

    fun getLinkedInSubtree(): MutableList<ReferenceBookItemVertex> {
        val linkedChildren = children.flatMap { it.getLinkedInSubtree() }.toMutableList()
        if (isLinkedBy()) {
            linkedChildren.add(this)
        }
        return linkedChildren
    }

    override fun equals(other: Any?): Boolean = vertex == other

    override fun hashCode(): Int = vertex.hashCode()
}
