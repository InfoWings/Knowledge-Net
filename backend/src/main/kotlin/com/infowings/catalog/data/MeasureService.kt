package com.infowings.catalog.data

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResultSet

const val MEASURE_GROUP_VERTEX = "MeasureGroupVertex"
const val MEASURE_VERTEX = "MeasureVertex"
const val MEASURE_GROUP_EDGE = "MeasureGroupEdge"
const val MEASURE_BASE_EDGE = "MeasureEdge"
const val MEASURE_BASE_AND_GROUP_EDGE = "MeasureGroupEdge"

/**
 * Измерения делятся на группы. Каждая группа имеет название и базовый элемент.
 * Например для группы Длинна базовый элемент - 'метр'. 'Километр', 'сантиметр'
 * и остальные элементы группы ссылаются на базовый. Связь измерение <-> базовый имеет тип {MEASURE_BASE_EDGE}
 * Базовый <-> группа имеет тип {MEASURE_GROUP_EDGE}. Связь между группами - {MEASURE_GROUP_EDGE}
 * */
class MeasureService {
    /** Возвращает вершину типа {MeasureGroupVertex}, описывающую запрашиваемую группу измерений.
     *  Если группа измерений с указанным именем не найдена, возвращает null. */
    fun findMeasureGroup(groupName: String, database: OrientDatabase): OVertex? {
        val query = "SELECT * from $MEASURE_GROUP_VERTEX where name = ?"
        val records = session(database) { it.query(query, groupName) }
        val vertex = records.getVertex()
        records.close()
        return vertex
    }

    /** Возвращает вершину типа {MeasureVertex}, описывающую запрашиваемое измерение.
     *  Если измерение с указанным именем не найдена, возвращает null. */
    fun findMeasure(measureName: String, database: OrientDatabase): OVertex? {
        val query = "SELECT * from $MEASURE_VERTEX where name = ?"
        val records = session(database) { it.query(query, measureName) }
        val vertex = records.getVertex()
        records.close()
        return vertex
    }

    /** Сохраняет группу измерений и все измерения, которые в ней содержатся.
     *  Возвращает ссылку на вершину, описывающую указанную группу.
     *  Если группа уже существовала, возвращаем null. */
    fun saveGroup(group: MeasureGroup<*>, database: OrientDatabase): OVertex? = session(database) { session ->
        if (findMeasureGroup(group.name, database) != null) {
            loggerFor<MeasureService>().info("Group with name ${group.name} already exist in db")
            return null
        }
        val groupVertex = session.newVertex(MEASURE_GROUP_VERTEX)
        groupVertex.setProperty("name", group.name)
        val baseVertex = createMeasure(group.base.name, database)
        groupVertex.addEdge(baseVertex, MEASURE_BASE_AND_GROUP_EDGE).save<ORecord>()
        baseVertex.addEdge(groupVertex, MEASURE_BASE_AND_GROUP_EDGE).save<ORecord>()
        group.measureList.forEach {
            createMeasure(it.name, database).addEdge(baseVertex, MEASURE_BASE_EDGE).save<ORecord>()
        }
        baseVertex.save<ORecord>()
        return groupVertex.save()
    }

    /** Соединяем две вершины типа {MeasureGroupVertex} двусторонней связью типа {MeasureGroupEdge}.
     *  Пример:   LengthGroup <----> SpeedGroup]
     * */
    fun linkGroupsBidirectional(first: MeasureGroup<*>, second: MeasureGroup<*>, database: OrientDatabase) {
        linkGroups(first, second, database)
        linkGroups(second, first, database)
    }


    /** Соединяем две вершины типа {MeasureGroupVertex} односторонней связью типа {MeasureGroupEdge}.
     *  Пример:   LengthGroup ----> SpeedGroup]
     * */
    fun linkGroups(source: MeasureGroup<*>, target: MeasureGroup<*>, database: OrientDatabase): OEdge? {
        val firstVertexGroup = findMeasureGroup(source.name, database) ?: return null
        val secondVertexGroup = findMeasureGroup(target.name, database) ?: return null
        val addedBefore = firstVertexGroup.getEdges(ODirection.OUT, MEASURE_GROUP_EDGE).find { it.to.identity == secondVertexGroup.identity }
        if (addedBefore != null) {
            return addedBefore
        }
        return firstVertexGroup.addEdge(secondVertexGroup, MEASURE_GROUP_EDGE).save()
    }

    private fun createMeasure(measureName: String, database: OrientDatabase): OVertex {
        findMeasure(measureName, database).let { if (it != null) return it }
        val groupVertex = session(database) { it.newVertex(MEASURE_VERTEX) }
        groupVertex.setProperty("name", measureName)
        return groupVertex
    }

    private fun OResultSet.getVertex() = if (this.hasNext()) this.next().vertex.orElse(null) else null
}