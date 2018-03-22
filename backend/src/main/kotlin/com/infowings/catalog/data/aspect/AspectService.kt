package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Measure
import com.infowings.catalog.storage.*
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.OVertex


/**
 * Data layer for Aspect & Aspect properties
 * Both stored as vertexes [ASPECT_CLASS] & [ASPECT_PROPERTY_CLASS] linked by [ASPECT_ASPECTPROPERTY_EDGE]
 * [ASPECT_CLASS] can be linked with [Measure] by [ASPECT_MEASURE_CLASS]
 */
class AspectService(private val db: OrientDatabase,
                    private val aspectDaoService: AspectDaoService
) {

    private val aspectValidator = AspectValidator(aspectDaoService, this)

    /**
     * Creates new Aspect if [id] = null or empty and saves it into DB else updating existing
     * @param aspectData data that represents Aspect, which will be saved or updated
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     * @throws AspectCyclicDependencyException if one of AspectProperty of the aspect refers to parent Aspect
     */
    fun save(aspectData: AspectData): Aspect {

        val save: AspectVertex = transaction(db) {

            val aspectVertex = aspectData
                    .checkAspectDataConsistent()
                    .checkBusinessKey()
                    .getOrCreateAspectVertex()

            aspectVertex.saveAspectProperties(aspectData.properties)

            return@transaction aspectDaoService.saveAspect(aspectVertex, aspectData)
        }

        return findById(save.id)
    }

    /**
     * Search [Aspect] by it's name
     * @return List of [Aspect] with name [name]
     */
    fun findByName(name: String): Set<Aspect> = aspectDaoService.findByName(name).map { it.toAspect() }.toSet()

    fun getAspects(): List<Aspect> = aspectDaoService.getAspects().map { it.toAspect() }.toList()

    fun getAspect(vertex: OVertex): Aspect = vertex.toAspect()

    /**
     * Search [Aspect] by it's id
     * @throws AspectDoesNotExist
     */
    fun findById(id: String): Aspect = aspectDaoService.getAspectVertex(id)?.toAspect() ?: throw AspectDoesNotExist(id)

    /**
     * @param aspectId aspect id to start
     * @return list of the current aspect and all its parents
     */
    fun findParentAspects(aspectId: String): List<AspectData> = session(db) {
        val q = "traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM :aspectRecord"
        return@session db.query(q, mapOf("aspectRecord" to ORecordId(aspectId))) {
            it.mapNotNull { it.toVertexOrNull()?.toAspectData() }.toList()
        }
    }

    /**
     * Load property by id
     * @throws AspectPropertyDoesNotExist
     */
    private fun loadAspectProperty(propertyId: String): AspectProperty =
            aspectDaoService.getAspectPropertyVertex(propertyId)?.toAspectProperty()
                    ?: throw AspectPropertyDoesNotExist(propertyId)

    private fun loadProperties(aspectVertex: AspectVertex): List<AspectProperty> = transaction(db) {
        aspectVertex.properties.map { loadAspectProperty(it.id) }
    }

    /**
     * Create empty vertex in case [aspectData.id] is null or empty
     * Otherwise validate and return vertex of class [ASPECT_CLASS] with given id
     * @throws IllegalStateException
     * @throws AspectConcurrentModificationException
     * */
    private fun AspectData.getOrCreateAspectVertex(): AspectVertex {
        val aspectId = id

        if (aspectId.isNullOrEmpty())
            return aspectDaoService.createNewAspectVertex()


        return aspectDaoService.getAspectVertex(aspectId!!)
                ?.validateExistingAspect(this)
                ?: throw IllegalArgumentException("Incorrect aspect id")

    }

    /**
     * Create empty vertex in case [aspectPropertyData.id] is null or empty
     * Otherwise validate and return vertex of class [ASPECT_PROPERTY_CLASS] with given id
     * @throws IllegalStateException
     * @throws AspectPropertyModificationException
     * */
    private fun AspectPropertyData.getOrCreatePropertyVertex(): AspectPropertyVertex {
        val propertyId = id

        if (propertyId.isEmpty())
            return aspectDaoService.createNewAspectPropertyVertex()


        return aspectDaoService.getAspectPropertyVertex(propertyId)
                ?.validateExistingAspectProperty(this)
                ?: throw IllegalArgumentException("Incorrect property id")

    }

    private fun AspectVertex.toAspect(): Aspect {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
        return Aspect(id, name, measure, baseTypeObj?.let { OpenDomain(it) }, baseTypeObj, loadProperties(this), version)
    }

    private fun AspectPropertyVertex.toAspectProperty(): AspectProperty =
            AspectProperty(id, name, findById(aspect), AspectPropertyCardinality.valueOf(cardinality), version)

    private fun AspectPropertyVertex.validateExistingAspectProperty(aspectPropertyData: AspectPropertyData): AspectPropertyVertex = this.also { aspectValidator.validateExistingAspectProperty(this, aspectPropertyData) }

    private fun AspectVertex.validateExistingAspect(aspectData: AspectData): AspectVertex = this.also { aspectValidator.validateExistingAspect(this, aspectData) }

    private fun AspectData.checkAspectDataConsistent(): AspectData = this.also { aspectValidator.checkAspectDataConsistent(this) }

    private fun AspectData.checkBusinessKey() = this.also { aspectValidator.checkBusinessKey(this) }

    private fun AspectVertex.saveAspectProperties(propertyData: List<AspectPropertyData>) {
        propertyData.forEach {
            val aspectPropertyVertex = it.getOrCreatePropertyVertex()
            aspectDaoService.saveAspectProperty(this, aspectPropertyVertex, it)
        }
    }
}

sealed class AspectException(message: String? = null) : Exception(message)
class AspectAlreadyExist(val name: String, subject: String?) :
    AspectException("name = $name, subject ${subject ?: "GLOBAL"}")
class AspectDoesNotExist(val id: String) : AspectException("id = $id")
class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")
class AspectConcurrentModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectPropertyModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectCyclicDependencyException(cyclicIds: List<String>) :
        AspectException("Cyclic dependencies on aspects with id: $cyclicIds")

class AspectNameCannotBeNull : AspectException()