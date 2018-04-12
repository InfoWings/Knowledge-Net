import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

const val notDeletedSql = "(deleted is NULL or deleted = false)"

fun OVertex.hasIncomingEdges(): Boolean =
        this.getEdges(ODirection.IN).any()
fun OVertex.allIncomingEdges(): List<OEdge> =
        this.getEdges(ODirection.IN).toList()
fun OVertex.incomingEdges(typeString: String): List<OEdge> =
        this.getEdges(ODirection.IN, typeString).toList()

