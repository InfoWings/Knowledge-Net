import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.hasIncomingEdges(): Boolean =
        this.getEdges(ODirection.IN).any()
fun OVertex.allIncomingEdges(): List<OEdge> =
        this.getEdges(ODirection.IN).toList()

