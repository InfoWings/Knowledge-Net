package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

class AspectValidator(val db: OrientDatabase, val aspectService: AspectService) {

    internal fun aspectVertex(aspectData: AspectData): OVertex {
        val aspectId = aspectData.id

        return if (aspectId?.isEmpty() == false) {
            db.getVertexById(aspectId)?.also { validateExistingAspect(it, aspectData) }
                    ?: throw IllegalStateException("Aspect with id $aspectId does not exist")
        } else {
            session(database = db) { it.newVertex(ASPECT_CLASS) }
        }
    }

    internal fun aspectPropertyVertex(aspectPropertyData: AspectPropertyData): OVertex {
        val propertyId = aspectPropertyData.id

        return if (!propertyId.isEmpty()) {
            db.getVertexById(propertyId)?.also { validateExistingAspectProperty(it, aspectPropertyData) }
                    ?: throw IllegalArgumentException("Incorrect property id")
        } else {
            session(database = db) { it.newVertex(ASPECT_PROPERTY_CLASS) }
        }
    }


    internal fun checkBusinessKey(aspectData: AspectData) {
        // check aspect business key
        if (aspectService.findByName(aspectData.name).filter { it.id != aspectData.id }.any { it.measure?.name == aspectData.measure }) {
            throw AspectAlreadyExist(aspectData.name, aspectData.measure)
        }
        // check aspect properties business key
        val valid = aspectData.properties.distinctBy { Pair(it.name, it.aspectId) }.size != aspectData.properties.size
        if (valid) {
            throw IllegalArgumentException("Not correct property business key $aspectData")
        }
    }

    internal fun checkAspectData(aspectData: AspectData) {
        val measureString: String? = aspectData.measure
        val baseType: String? = aspectData.baseType

        if (measureString != null && baseType != null) {
            val measure: Measure<*> = GlobalMeasureMap[measureString]
                    ?: throw IllegalArgumentException("Measure $measureString incorrect")

            if (measure.baseType != BaseType.restoreBaseType(aspectData.baseType)) {
                throw IllegalArgumentException("Measure $measure and base type $baseType relation incorrect")
            }
        }
    }

    private fun validateExistingAspect(aspectVertex: OVertex, aspectData: AspectData) {
        checkAspectVersion(aspectVertex, aspectData)
        checkBaseTypeChangeCriteria(aspectVertex, aspectData)
        checkMeasureChangeCriteria(aspectVertex, aspectData)
    }

    private fun checkAspectVersion(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectVertex.version != aspectData.version) {
            throw AspectModificationException(aspectVertex.id, "Old version, db: ${aspectVertex.version}, param: ${aspectData.version}")
        }
        val realVersionMap = aspectVertex.properties.map { it.id to it.version }.toMap()
        val receivedVersionMap = aspectData.properties.filter { it.id != "" }.map { it.id to it.version }.toMap()

        if (realVersionMap.keys.size != receivedVersionMap.keys.size) {
            throw AspectModificationException(aspectVertex.id, "Old version")
        }

        val different = realVersionMap.any { (k, v) -> v != receivedVersionMap[k] }
        if (different) {
            throw AspectModificationException(aspectVertex.id, "Old version")
        }
    }

    private fun checkBaseTypeChangeCriteria(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectData.baseType != aspectVertex.baseType?.name) {
            if ((aspectData.measure != null && aspectData.measure == aspectVertex.measureName)
            /* или сущуствует хотя бы один экземпляр Аспекта */) {

                throw AspectModificationException(aspectVertex.id, "Impossible to change base type")
            }
        }
    }

    private fun checkMeasureChangeCriteria(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectData.measure != aspectVertex.measureName) {
            val sameGroup = aspectVertex.measureName == aspectData.measure
            if (!sameGroup && false /* сущуствует хотя бы один экземпляр Аспекта */) {
                throw AspectModificationException(aspectVertex.id, "Impossible to change measure")
            }
        }
    }

    private fun validateExistingAspectProperty(aspectPropertyVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        checkPropertyAspectChangeCriteria(aspectPropertyVertex, aspectPropertyData)
    }

    private fun checkPropertyAspectChangeCriteria(aspectVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        if (aspectVertex.aspect != aspectPropertyData.id) {
            if (false /* сущуствует хотя бы один экземпляр Аспекта */) {
                throw AspectPropertyModificationException(aspectVertex.id, "Impossible to change aspectId")
            }
        }
    }
}