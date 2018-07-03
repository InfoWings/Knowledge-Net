package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.common.TreeAspectPropertyResponse
import com.infowings.catalog.common.TreeAspectResponse
import com.infowings.catalog.storage.ASPECT_ASPECT_PROPERTY_EDGE
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.record.ODirection

private sealed class AspectHolder
private class AspectVertexHolder(val vertex: AspectVertex) : AspectHolder() {
    var properties: MutableList<AspectPropertyVertexHolder> = mutableListOf()
    var completedAspect: TreeAspectResponse? = null
    var isComplete: Boolean = false

    fun verifyNext(propertyVertex: AspectPropertyVertex) {
        val incomingAspectEdge = propertyVertex.getEdges(ODirection.IN, ASPECT_ASPECT_PROPERTY_EDGE).first()
        val edgeId = incomingAspectEdge.identity
        val isPropertyHasEdgeLikeInAspect = vertex.getEdges(ODirection.OUT, ASPECT_ASPECT_PROPERTY_EDGE).any {
            it.identity == edgeId
        }
        if (!isPropertyHasEdgeLikeInAspect) {
            throw IllegalStateException("Supplied property vertex (id = ${propertyVertex.id}) is not inside aspect (id = ${vertex.id})")
        }
    }

    fun completeWith(response: TreeAspectResponse) {
        this.completedAspect = response
        this.isComplete = true
    }
}

private class AspectPropertyVertexHolder(val vertex: AspectPropertyVertex) : AspectHolder() {
    var aspect: AspectVertexHolder? = null
    var completedProperty: TreeAspectPropertyResponse? = null
    var isComplete: Boolean = false

    fun verifyNext(aspectVertex: AspectVertex) {
        val incomingAspectPropertyEdgeId = aspectVertex.getEdges(ODirection.IN, ASPECT_ASPECT_PROPERTY_EDGE).first().identity
        val isPropertyHasEqualOutgoingEdge = vertex.getEdges(ODirection.OUT, ASPECT_ASPECT_PROPERTY_EDGE).first() == incomingAspectPropertyEdgeId
        if (!isPropertyHasEqualOutgoingEdge) {
            throw IllegalStateException("Supplied aspect vertex (id = ${aspectVertex.id} is not inside aspect property (id = ${vertex.id})")
        }
    }

    fun completeWith(response: TreeAspectPropertyResponse) {
        completedProperty = response
        isComplete = true
    }
}

class AspectTreeBuilder {

    private var aspectTraversalState: MutableList<AspectHolder> = mutableListOf()

    fun tryAppendAspect(aspectVertex: AspectVertex) {
        if (aspectTraversalState.isEmpty()) {
            val vertexHolder = AspectVertexHolder(aspectVertex)
            aspectTraversalState.add(vertexHolder)
            tryReduceTraversalState()
        } else {
            val lastVertexHolderInState = aspectTraversalState.last()
            when (lastVertexHolderInState) {
                is AspectVertexHolder -> throw IllegalStateException("Two successive AspectVertexes in aspect tree traversal")
                is AspectPropertyVertexHolder -> {
                    lastVertexHolderInState.verifyNext(aspectVertex)
                    aspectTraversalState.add(AspectVertexHolder(aspectVertex))
                    tryReduceTraversalState()
                }
            }
        }
    }

    fun tryAppendAspectProperty(propertyVertex: AspectPropertyVertex) {
        if (aspectTraversalState.isEmpty()) {
            throw IllegalStateException("First Vertex in traversal is AspectPropertyVertex. Should be AspectVertex")
        } else {
            val lastVertexHolderInState = aspectTraversalState.last()
            when (lastVertexHolderInState) {
                is AspectPropertyVertexHolder -> throw IllegalStateException("Two successive AspectPropertyVertexes in aspect tree traversal")
                is AspectVertexHolder -> {
                    lastVertexHolderInState.verifyNext(propertyVertex)
                    aspectTraversalState.add(AspectPropertyVertexHolder(propertyVertex))
                }
            }
        }
    }

    fun tryBuildAspectTree(): TreeAspectResponse {
        val firstInAspectState = aspectTraversalState.firstOrNull()
        if (firstInAspectState != null && firstInAspectState is AspectVertexHolder && firstInAspectState.isComplete) {
            return firstInAspectState.completedAspect ?: throw IllegalStateException("Completed aspect holder does not contain built aspect tree")
        } else {
            throw IllegalStateException("Aspect tree is not yet completed")
        }
    }

    private tailrec fun tryReduceTraversalState() {
        when (aspectTraversalState.size) {
            0 -> throw IllegalStateException("Traversal state is empty, cannot be reduced")
            1 -> {
                tryReduceLastAspect()
            }
            else -> {
                if (tryReduceLastAspect()) {
                    tryReduceLastAspectProperty()
                    tryReduceTraversalState()
                }
            }
        }
    }

    private fun tryReduceLastAspect(): Boolean {
        val lastVertexHolderInState = aspectTraversalState.last()
        when (lastVertexHolderInState) {
            is AspectPropertyVertexHolder -> throw IllegalStateException("Expected last vertex in state to be Aspect Property")
            is AspectVertexHolder -> {
                val isReadyForReduce = lastVertexHolderInState.properties.all {
                    it.isComplete
                }
                if (isReadyForReduce) {
                    val aspectVertex = lastVertexHolderInState.vertex
                    val aspectResponse = TreeAspectResponse(
                        aspectVertex.id,
                        aspectVertex.name,
                        aspectVertex.measureName,
                        aspectVertex.baseType,
                        aspectVertex.baseType?.let { OpenDomain(BaseType.restoreBaseType(it)).toString() },
                        aspectVertex["refBookId"],
                        lastVertexHolderInState.properties.map {
                            it.completedProperty ?: throw IllegalStateException("Completed flag is up while completed property is absent")
                        }
                    )
                    lastVertexHolderInState.completeWith(aspectResponse)
                    aspectTraversalState.removeAt(aspectTraversalState.lastIndex)
                    return true
                } else {
                    return false
                }
            }
        }
    }

    private fun tryReduceLastAspectProperty() {
        val lastVertexHolderInState = aspectTraversalState.last()
        when (lastVertexHolderInState) {
            is AspectVertexHolder -> throw IllegalStateException("Expected last vertex in state to be Aspect")
            is AspectPropertyVertexHolder -> {
                val aspectPropertyVertex = lastVertexHolderInState.vertex
                val aspectPropertyResponse = TreeAspectPropertyResponse(
                    aspectPropertyVertex.id,
                    PropertyCardinality.valueOf(aspectPropertyVertex.cardinality),
                    aspectPropertyVertex.name,
                    lastVertexHolderInState.aspect?.completedAspect ?: throw IllegalStateException("Expected child aspect to be reduced")
                )
                lastVertexHolderInState.completeWith(aspectPropertyResponse)
                aspectTraversalState.removeAt(aspectTraversalState.lastIndex)
            }
        }
    }

}