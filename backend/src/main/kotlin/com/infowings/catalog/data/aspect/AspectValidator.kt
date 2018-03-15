package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.record.OVertex

/**
 * Class for validating aspects.
 */
internal class AspectValidator(private val db: OrientDatabase, private val aspectService: AspectService) {

    /**
     * Check business key of given [AspectData]
     * @throws AspectAlreadyExist
     * @throws IllegalArgumentException
     */
    internal fun checkBusinessKey(aspectData: AspectData) {
        // check aspect business key
        val alreadyExists = aspectService
                .findByName(aspectData.name)
                .filter { it.id != aspectData.id }
                .any { it.measure?.name == aspectData.measure }

        if (alreadyExists) {
            throw AspectAlreadyExist(aspectData.name, aspectData.measure)
        }
        // check aspect properties business key
        val notValid = aspectData.properties.distinctBy { Pair(it.name, it.aspectId) }.size != aspectData.properties.size
        if (notValid) {
            throw IllegalArgumentException("Not correct property business key $aspectData")
        }
    }

    /**
     * Data consistency check.
     * For example, conformity of measure and base type
     * @throws IllegalArgumentException
     */
    internal fun checkAspectData(aspectData: AspectData) {
        val measureString: String? = aspectData.measure
        val baseType: String? = aspectData.baseType

        if (measureString != null) {
            val measure: Measure<*> = GlobalMeasureMap[measureString]
                    ?: throw IllegalArgumentException("Measure $measureString incorrect")

            if (baseType != null && measure.baseType != BaseType.restoreBaseType(baseType)) {
                throw IllegalArgumentException("Measure $measure and base type $baseType relation incorrect")
            }
        }
    }

    internal fun validateExistingAspect(aspectVertex: OVertex, aspectData: AspectData) {
        checkAspectVersion(aspectVertex, aspectData)
        checkBaseTypeChangeCriteria(aspectVertex, aspectData)
        checkMeasureChangeCriteria(aspectVertex, aspectData)
    }

    private fun checkAspectVersion(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectVertex.version != aspectData.version) {
            throw AspectModificationException(aspectVertex.id, "Old version, db: ${aspectVertex.version}, param: ${aspectData.version}")
        }

        val realVersionMap = aspectVertex.properties.map { it.id to it.version }.toMap()
        val receivedVersionMap = aspectData.properties.filter { it.id.isNotEmpty() }.map { it.id to it.version }.toMap()

        if (realVersionMap.keys.size != receivedVersionMap.keys.size) {
            throw AspectModificationException(aspectVertex.id, "Old version")
        }

        val different = realVersionMap.any { (k, v) -> v != receivedVersionMap[k] }
        if (different) {
            throw AspectModificationException(aspectVertex.id, "Old version")
        }
    }

    private fun checkBaseTypeChangeCriteria(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectData.baseType != aspectVertex.baseType) {
            if ((aspectData.measure != null && aspectData.measure == aspectVertex.measureName)
                    || thereExistAspectImplementation(aspectVertex.id)) {

                throw AspectModificationException(aspectVertex.id, "Impossible to change base type")
            }
        }
    }

    private fun checkMeasureChangeCriteria(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectData.measure != aspectVertex.measureName) {
            val sameGroup = aspectVertex.measureName == aspectData.measure
            if (!sameGroup && thereExistAspectImplementation(aspectVertex.id)) {
                throw AspectModificationException(aspectVertex.id, "Impossible to change measure")
            }
        }
    }

    internal fun validateExistingAspectProperty(aspectPropertyVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        checkPropertyAspectChangeCriteria(aspectPropertyVertex, aspectPropertyData)
    }

    private fun checkPropertyAspectChangeCriteria(aspectVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        if (aspectVertex.aspect != aspectPropertyData.aspectId) {
            if (thereExistAspectImplementation(aspectPropertyData.aspectId)) {
                throw AspectPropertyModificationException(aspectVertex.id, "Impossible to change aspectId")
            }
        }
    }

    // todo: Complete this method in future
    private fun thereExistAspectImplementation(aspectId: String): Boolean = false
}