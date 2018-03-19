package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

/**
 * Class for validating aspects.
 * Methods should be called in transaction
 */
class AspectValidator(private val aspectDaoService: AspectDaoService, private val suggestionService: SuggestionService) {

    /**
     * Check business key of given [AspectData]
     * @throws AspectAlreadyExist
     * @throws IllegalArgumentException
     */
    fun checkBusinessKey(aspectData: AspectData) {
        checkAspectBusinessKey(aspectData)
        checkAspectPropertyBusinessKey(aspectData)
    }

    /**
     * Data consistency check.
     * For example, conformity of measure and base type
     * @throws IllegalArgumentException
     */
    fun checkAspectDataConsistent(aspectData: AspectData) {
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

    fun validateExistingAspect(aspectVertex: OVertex, aspectData: AspectData) {
        checkAspectVersion(aspectVertex, aspectData)
        checkCyclicDependencies(aspectVertex, aspectData)
        // this describes case when there exists something that links to this aspect (Aspect is not 'free')
        if (aspectVertex.getVertices(ODirection.IN).any()) {
            checkBaseTypeChangeCriteria(aspectVertex, aspectData)
            checkMeasureChangeCriteria(aspectVertex, aspectData)
        }
    }

    fun validateExistingAspectProperty(aspectPropertyVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        checkPropertyAspectChangeCriteria(aspectPropertyVertex, aspectPropertyData)
    }

    private fun checkCyclicDependencies(aspectVertex: OVertex, aspectData: AspectData) {
        val parentsIds = suggestionService.findParentAspects(aspectVertex.id).mapNotNull { it.id }
        val cyclicIds = aspectData.properties
                .map { it.aspectId }
                .filter { parentsIds.contains(it) }
                .toList()

        if (cyclicIds.isNotEmpty()) throw AspectCyclicDependencyException(cyclicIds)
    }

    private fun checkAspectBusinessKey(aspectData: AspectData) {
        // val sql = "SELECT from $ASPECT_CLASS WHERE name=? and @rid <> ? and ($notDeletedSql)"

        aspectDaoService.getAspectsByNameWithDifferentId(aspectData.name, aspectData.id).let {
            if (it.any()) {
                throw AspectAlreadyExist(aspectData.name)
            }
        }
    }

    private fun checkAspectPropertyBusinessKey(aspectData: AspectData) {
        // check aspect properties business key
        // there should be aspectData.properties.size unique pairs (name, aspectId) in property list
        // not call db because we suppose that Aspect Property business key is exist only inside concrete aspect
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
            if (!sameGroup && thereExistAspectImplementation(aspectVertex.id)) {
                throw AspectModificationException(aspectVertex.id, "Impossible to change measure")
            }
        }
    }

    private fun checkPropertyAspectChangeCriteria(aspectVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        if (aspectVertex.aspect != aspectPropertyData.aspectId) {
            if (thereExistAspectPropertyImplementation(aspectPropertyData.id)) {
                throw AspectPropertyModificationException(aspectVertex.id, "Impossible to change aspectId")
            }
        }
    }

    // todo: Complete this method in future
    private fun thereExistAspectImplementation(aspectId: String): Boolean = false

    // todo: Complete this method in future
    private fun thereExistAspectPropertyImplementation(aspectPropertyId: String): Boolean = false
}

private const val notDeletedSql = "deleted is NULL or deleted = false"