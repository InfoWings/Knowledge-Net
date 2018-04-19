package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.storage.id

/**
 * Class for validating aspects.
 * Methods should be called in transaction
 */

fun AspectVertex.checkAspectVersion(aspectData: AspectData) = this.also {
    if (version != aspectData.version) {
        throw AspectConcurrentModificationException(
            id,
            "Old Aspect version. Expected: $version. Actual: ${aspectData.version}"
        )
    }

    val realVersionMap = properties.map { it.id to it.version }.toMap()
    val receivedVersionMap = aspectData.properties.filter { it.id.isNotEmpty() }.map { it.id to it.version }.toMap()

    if (realVersionMap.keys.size != receivedVersionMap.keys.size) {
        throw AspectConcurrentModificationException(id, "Properties changed")
    }

    val different = realVersionMap.any { (k, v) -> v != receivedVersionMap[k] }
    if (different) {
        throw AspectConcurrentModificationException(id, "Properties changed")
    }
}

class AspectValidator(
    private val aspectDaoService: AspectDaoService
) {

    /**
     * Check business key of given [AspectData]
     * @throws AspectAlreadyExist
     * @throws AspectInconsistentStateException
     */
    fun checkBusinessKey(aspectData: AspectData) {
        aspectData
            .checkAspectBusinessKey()
            .checkAspectPropertyBusinessKey()
    }

    /**
     * Data consistency check.
     * For example, conformity of measure and base type
     * @throws AspectInconsistentStateException
     */
    fun checkAspectDataConsistent(aspectData: AspectData) {
        // check properties not link to deleted aspects
        aspectData.properties.filterNot { it.deleted }.forEach { it.checkForRemoved() }

        val measureName: String? = aspectData.measure
        val baseType: String? = aspectData.baseType

        when {
            measureName == null && baseType == null ->
                throw AspectInconsistentStateException("Measure and Base Type can't be null at the same time. " +
                        "Please, enter either Measure or Base Type")
            measureName == null && baseType != null -> BaseType.restoreBaseType(baseType)
        // will throw on incorrect baseType

            measureName != null && baseType != null -> {
                val measure: Measure<*> = GlobalMeasureMap[measureName]
                        ?: throw AspectInconsistentStateException("Measure $measureName incorrect")

                if (measure.baseType != BaseType.restoreBaseType(baseType)) {
                    throw AspectInconsistentStateException("Measure $measure and base type $baseType relation incorrect")
                }
            }
        }

    }

    fun validateExistingAspect(aspectVertex: AspectVertex, aspectData: AspectData) {
        aspectVertex
            .checkForRemoved()
            .checkAspectVersion(aspectData)
            .checkCyclicDependencies(aspectData)

        // this describes case when there exists something that links to this aspect (Aspect is not 'free')
        if (aspectVertex.isLinkedBy()) {
            aspectVertex
                .checkBaseTypeChangeCriteria(aspectData)
                .checkMeasureChangeCriteria(aspectData)
        }
    }

    fun validateExistingAspectProperty(
        aspectPropertyVertex: AspectPropertyVertex,
        aspectPropertyData: AspectPropertyData
    ) {
        aspectPropertyVertex
            .checkPropertyAspectChangeCriteria(aspectPropertyData)
    }

    private fun AspectData.checkAspectBusinessKey() = this.also {
        val name = this.name ?: throw AspectNameCannotBeNull()
        id?.let { checkAspectBusinessKeyForExistingAspect(this, name) } ?: checkAspectBusinessKeyForNewAspect(this)
    }

    private fun checkAspectBusinessKeyForExistingAspect(aspectData: AspectData, name: String) {
        aspectDaoService.getAspectsByNameAndSubjectWithDifferentId(name, aspectData.subject?.id, aspectData.id).let {
            if (it.any()) {
                throw AspectAlreadyExist(name, null)
            }
        }
    }

    private fun checkAspectBusinessKeyForNewAspect(aspectData: AspectData) {
        val name = aspectData.name ?: throw AspectNameCannotBeNull()
        aspectDaoService.getAspectsByNameAndSubjectWithDifferentId(name, aspectData.subject?.id, null)
            .let {
                /*
                  getAspectsByNameAndSubjectWithDifferentId returns all aspects with the same name is subject is null

                  In theory it would be better to enhance getAspectsByNameAndSubjectWithDifferentId or introduce
                  another method.
                  But at the moment internals of AspectDaoService seem to be overcomplicated - many different kinds
                  of queries are composed as plain concatenation of sql pieces using tricky set of conditions.
                  So it seems not very good idea to add even more complexity there.

                  At some moment it makes sense to refactor AspectDaoService based on some kind of QueryBuilder
                   pattern - compose queries using row of higher level method calls keeping plain SQL pieces behind
                   scene.

                  But right now it seems more convenient to make additional filtering here to avoid overcompication of
                  AspectDaoService
                 */

                if (aspectData.subject == null) {
                    val foundEmpty = it.find {
                        it.subject == null
                    }
                    if (foundEmpty != null) {
                        throw AspectAlreadyExist(name, null)
                    }
                } else {
                    if (it.isNotEmpty()) {
                        throw AspectAlreadyExist(name, aspectData.subject?.name)
                    }
                }
            }
    }
    private fun AspectData.checkAspectPropertyBusinessKey() = this.also {

        // check aspect properties business key
        // there should be aspectData.properties.size unique pairs (name, aspectId) in property list
        // not call db because we suppose that Aspect Property business key is exist only inside concrete aspect
        val aliveProperties = actualData().properties

        val notValid =
            aliveProperties.distinctBy { Pair(it.name, it.aspectId) }.size != aliveProperties.size

        if (notValid) {
            throw AspectInconsistentStateException("Aspect properties should have unique pairs of name and assigned aspect")
        }
    }

    private fun AspectVertex.checkForRemoved() = also {
        if (deleted) {
            throw AspectModificationException(id, "Aspect is removed")
        }
    }

    private fun AspectVertex.checkCyclicDependencies(aspectData: AspectData) = this.also {
        val parentsIds = aspectDaoService.findParentAspects(id).mapNotNull { it.id }
        val cyclicIds = aspectData.properties
            .map { it.aspectId }
            .filter { parentsIds.contains(it) }
            .toList()

        if (cyclicIds.isNotEmpty()) throw AspectCyclicDependencyException(cyclicIds)
    }

    private fun AspectVertex.checkBaseTypeChangeCriteria(aspectData: AspectData) = this.also {
        if (aspectData.baseType != baseType) {
            if ((aspectData.measure != null && aspectData.measure == measureName)
                || thereExistAspectImplementation(id)
            ) {
                throw AspectModificationException(id, "Impossible to change base type")
            }
        }
    }

    private fun AspectVertex.checkMeasureChangeCriteria(aspectData: AspectData) = this.also {
        if (aspectData.measure != measureName) {
            val sameGroup = measureName == aspectData.measure
            if (!sameGroup && thereExistAspectImplementation(id)) {
                throw AspectModificationException(id, "Impossible to change measure")
            }
        }
    }

    private fun AspectPropertyVertex.checkPropertyAspectChangeCriteria(aspectPropertyData: AspectPropertyData) =
        this.also {
            if (aspect != aspectPropertyData.aspectId) {
                if (thereExistAspectPropertyImplementation(aspectPropertyData.id)) {
                    throw AspectPropertyModificationException(id, "Impossible to change aspectId")
                }
            }
        }

    private fun AspectPropertyData.checkForRemoved() = also {
        val relatedAspect = aspectDaoService.getAspectVertex(aspectId)
        if (relatedAspect?.deleted != null && relatedAspect.deleted) {
            throw AspectDoesNotExist(aspectId)
        }
    }

    // todo: Complete this method in future
    private fun thereExistAspectImplementation(aspectId: String): Boolean = false

    // todo: Complete this method in future
    private fun thereExistAspectPropertyImplementation(aspectPropertyId: String): Boolean = false
}