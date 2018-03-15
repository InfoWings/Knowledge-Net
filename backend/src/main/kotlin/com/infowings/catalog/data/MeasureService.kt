package com.infowings.catalog.data

import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.MeasureGroup
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex

const val MEASURE_GROUP_VERTEX = "MeasureGroupVertex"
const val MEASURE_VERTEX = "MeasureVertex"
const val MEASURE_GROUP_EDGE = "MeasureGroupEdge"
const val MEASURE_BASE_EDGE = "MeasureEdge"
const val MEASURE_BASE_AND_GROUP_EDGE = "MeasureBaseAndGroupEdge"


/**
 * Public OVertex Extensions.
 */
fun OVertex.toMeasure() = GlobalMeasureMap[this["name"]]


/**
 * Измерения делятся на группы. Каждая группа имеет название и базовый элемент.
 * Например для группы Длинна базовый элемент - 'метр'. 'Километр', 'сантиметр'
 * и остальные элементы группы ссылаются на базовый. Связь измерение <-> базовый имеет тип {MEASURE_BASE_EDGE}
 * Базовый <-> группа имеет тип {MEASURE_GROUP_EDGE}. Связь между группами - {MEASURE_GROUP_EDGE}
 * */
class MeasureService(val database: OrientDatabase) {

    /** Возвращает вершину типа {MeasureGroupVertex}, описывающую запрашиваемую группу измерений.
     *  Если группа измерений с указанным именем не найдена, возвращает null. */
    fun findMeasureGroup(groupName: String): OVertex? {
        val query = "SELECT * from $MEASURE_GROUP_VERTEX where name = ?"
        return database.query(query, groupName) {
            it.map { it.toVertex() }.firstOrNull()
        }
    }

    /** Возвращает вершину типа {MeasureVertex}, описывающую запрашиваемое измерение.
     *  Если измерение с указанным именем не найдена, возвращает null. */
    fun findMeasure(measureName: String): OVertex? {
        val query = "SELECT * from $MEASURE_VERTEX where name = ?"
        return database.query(query, measureName) {
            it.map { it.toVertex() }.firstOrNull()
        }
    }

    /** Сохраняет группу измерений и все измерения, которые в ней содержатся.
     *  Возвращает ссылку на вершину, описывающую указанную группу.
     *  Если группа уже существовала, возвращаем null. */
    fun saveGroup(group: MeasureGroup<*>): OVertex? = transaction(database) {
        if (findMeasureGroup(group.name) != null) {
            loggerFor<MeasureService>().info("Group with name ${group.name} already exist in db")
            return@transaction null
        }
        val groupVertex = database.createNewVertex(MEASURE_GROUP_VERTEX)
        groupVertex.setProperty("name", group.name)
        val baseVertex = createMeasureVertexWithoutSaving(group.base.name)
        groupVertex.addEdge(baseVertex, MEASURE_BASE_AND_GROUP_EDGE).save<ORecord>()
        baseVertex.addEdge(groupVertex, MEASURE_BASE_AND_GROUP_EDGE).save<ORecord>()
        group.measureList.forEach {
            createMeasureVertexWithoutSaving(it.name).addEdge(baseVertex, MEASURE_BASE_EDGE).save<ORecord>()
        }
        baseVertex.save<ORecord>()
        return@transaction groupVertex.save()
    }

    /** Соединяем две вершины типа {MeasureGroupVertex} двусторонней связью типа {MeasureGroupEdge}.
     *  Пример:   LengthGroup <----> SpeedGroup]
     * */
    fun linkGroupsBidirectional(first: MeasureGroup<*>, second: MeasureGroup<*>) {
        linkGroups(first, second)
        linkGroups(second, first)
    }


    /** Соединяем две вершины типа {MeasureGroupVertex} односторонней связью типа {MeasureGroupEdge}.
     *  Пример:   LengthGroup ----> SpeedGroup]
     * */
    fun linkGroups(source: MeasureGroup<*>, target: MeasureGroup<*>): OEdge? {
        val firstVertexGroup = findMeasureGroup(source.name) ?: return null
        val secondVertexGroup = findMeasureGroup(target.name) ?: return null
        val addedBefore = firstVertexGroup.getEdges(ODirection.OUT, MEASURE_GROUP_EDGE).find { it.to.id == secondVertexGroup.id }
        if (addedBefore != null) {
            return addedBefore
        }
        return firstVertexGroup.addEdge(secondVertexGroup, MEASURE_GROUP_EDGE).save()
    }

    private fun createMeasureVertexWithoutSaving(measureName: String): OVertex {
        findMeasure(measureName).let { if (it != null) return it }
        val groupVertex = database.createNewVertex(MEASURE_VERTEX)
        groupVertex.setProperty("name", measureName)
        return groupVertex
    }
}