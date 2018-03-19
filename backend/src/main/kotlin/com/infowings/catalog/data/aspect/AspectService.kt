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
class AspectService(private val db: OrientDatabase, private val aspectDaoService: AspectDaoService) {

    private val aspectValidator = AspectValidator(aspectDaoService)

    /**
     * Creates new Aspect if [id] = null or empty and saves it into DB else updating existing
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     */
    fun save(aspectData: AspectData): Aspect {

        val save: OVertex = transaction(db) {

            aspectValidator.checkAspectDataConsistent(aspectData)
            aspectValidator.checkBusinessKey(aspectData)

            val aspectVertex: OVertex = getOrCreateAspectVertex(aspectData)

            aspectData.properties.forEach { propertyData ->
                val aspectPropertyVertex = getOrCreatePropertyVertex(propertyData)
                aspectDaoService.saveAspectProperty(aspectVertex, aspectPropertyVertex, propertyData)
            }

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

    /**
     * Search [Aspect] by it's id
     * @throws AspectDoesNotExist
     */
    fun findById(id: String): Aspect = aspectDaoService.getAspectVertex(id)?.toAspect() ?: throw AspectDoesNotExist(id)

    /**
     * Load property by id
     * @throws AspectPropertyDoesNotExist
     */
    private fun loadAspectProperty(propertyId: String): AspectProperty =
            aspectDaoService.getAspectPropertyVertex(propertyId)?.toAspectProperty()
                    ?: throw AspectPropertyDoesNotExist(propertyId)

    private fun loadProperties(oVertex: OVertex): List<AspectProperty> = transaction(db) {
        oVertex.properties.map { loadAspectProperty(it.id) }
    }

    /**
     * Create empty vertex in case [aspectData.id] is null or empty
     * Otherwise validate and return vertex of class [ASPECT_CLASS] with given id
     * @throws IllegalStateException
     * @throws AspectConcurrentModificationException
     * */
    private fun getOrCreateAspectVertex(aspectData: AspectData): OVertex {
        val aspectId = aspectData.id

        if (aspectId.isNullOrEmpty())
            return aspectDaoService.createNewAspectVertex()


        return aspectDaoService.getAspectVertex(aspectId!!)
                ?.validateExistingAspect(aspectData)
                ?: throw IllegalArgumentException("Incorrect aspect id")

    }

    /**
     * Create empty vertex in case [aspectPropertyData.id] is null or empty
     * Otherwise validate and return vertex of class [ASPECT_PROPERTY_CLASS] with given id
     * @throws IllegalStateException
     * @throws AspectPropertyModificationException
     * */
    private fun getOrCreatePropertyVertex(aspectPropertyData: AspectPropertyData): OVertex {
        val propertyId = aspectPropertyData.id

        if (propertyId.isEmpty())
            return aspectDaoService.createNewAspectPropertyVertex()


        return aspectDaoService.getAspectPropertyVertex(propertyId)
                ?.validateExistingAspectProperty(aspectPropertyData)
                ?: throw IllegalArgumentException("Incorrect property id")

    }

    private fun OVertex.toAspect(): Aspect {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
        return Aspect(id, name, measure, baseTypeObj?.let { OpenDomain(it) }, baseTypeObj, loadProperties(this), version)
    }

    private fun OVertex.toAspectProperty(): AspectProperty =
            AspectProperty(id, name, findById(aspect), AspectPropertyCardinality.valueOf(cardinality), version)

    private fun OVertex.validateExistingAspectProperty(aspectPropertyData: AspectPropertyData): OVertex {
        aspectValidator.validateExistingAspectProperty(this, aspectPropertyData)
        return this
    }

    private fun OVertex.validateExistingAspect(aspectData: AspectData): OVertex {
        aspectValidator.validateExistingAspect(this, aspectData)
        return this
    }
}

sealed class AspectException(message: String? = null) : Exception(message)
class AspectAlreadyExist(val name: String) : AspectException("name = $name")
class AspectDoesNotExist(val id: String) : AspectException("id = $id")
class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")
class AspectConcurrentModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectPropertyModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
