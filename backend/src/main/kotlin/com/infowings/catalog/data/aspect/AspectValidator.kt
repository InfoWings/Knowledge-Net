package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

/**
 * Class for validating aspects.
 */
internal class AspectValidator(private val db: OrientDatabase) {

    /**
     * Check business key of given [AspectData]
     * @throws AspectAlreadyExist
     * @throws IllegalArgumentException
     */
    internal fun checkBusinessKey(aspectData: AspectData) {
        checkAspectBusinessKey(aspectData)
        checkAspectPropertyBusinessKey(aspectData)
    }

    /**
     * Data consistency check.
     * For example, conformity of measure and base type
     * @throws IllegalArgumentException
     */
    internal fun checkAspectDataConsistent(aspectData: AspectData) {
        val measureName: String? = aspectData.measure
        val baseType: String? = aspectData.baseType

        if (measureName == null && baseType == null) {
            throw IllegalArgumentException("Measure and BaseType can't be null in same time")
        }

        if (measureName != null) {
            val measure: Measure<*> = GlobalMeasureMap[measureName]
                    ?: throw IllegalArgumentException("Measure $measureName incorrect")

            if (baseType != null && measure.baseType != BaseType.restoreBaseType(baseType)) {
                throw IllegalArgumentException("Measure $measure and base type $baseType relation incorrect")
            }
        }
    }

    internal fun validateExistingAspect(aspectVertex: OVertex, aspectData: AspectData) {
        checkAspectVersion(aspectVertex, aspectData)

        // this describes case when there exists something that links to this aspect (Aspect is not 'free')
        if (aspectVertex.getVertices(ODirection.IN).any()) {
            checkBaseTypeChangeCriteria(aspectVertex, aspectData)
            checkMeasureChangeCriteria(aspectVertex, aspectData)
        }
    }

    internal fun validateExistingAspectProperty(aspectPropertyVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        checkPropertyAspectChangeCriteria(aspectPropertyVertex, aspectPropertyData)
    }

    private fun checkAspectBusinessKey(aspectData: AspectData) {
        val sql = "SELECT from $ASPECT_CLASS WHERE name=? and @rid <> ?"
        db.query(sql, aspectData.name, ORecordId(aspectData.id)) {
            if (it.any()) {
                throw AspectAlreadyExist(aspectData.name)
            }
        }
    }

    private fun checkAspectPropertyBusinessKey(aspectData: AspectData) {
        // check aspect properties business key
        val notValid = aspectData.properties.distinctBy { Pair(it.name, it.aspectId) }.size != aspectData.properties.size

        if (notValid) {
            throw IllegalArgumentException("Not correct property business key $aspectData")
        }
    }

    private fun checkAspectVersion(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectVertex.version != aspectData.version) {
            throw AspectConcurrentModificationException(aspectVertex.id, "Old Aspect version. Expected: ${aspectVertex.version}. Actual: ${aspectData.version}")
        }

        val realVersionMap = aspectVertex.properties.map { it.id to it.version }.toMap()
        val receivedVersionMap = aspectData.properties.filter { it.id.isNotEmpty() }.map { it.id to it.version }.toMap()

        if (realVersionMap.keys.size != receivedVersionMap.keys.size) {
            throw AspectConcurrentModificationException(aspectVertex.id, "Properties changed")
        }

        val different = realVersionMap.any { (k, v) -> v != receivedVersionMap[k] }
        if (different) {
            throw AspectConcurrentModificationException(aspectVertex.id, "Properties changed")
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
            if (!sameGroup || thereExistAspectImplementation(aspectVertex.id)) {
                throw AspectModificationException(aspectVertex.id, "Impossible to change measure")
            }
        }
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