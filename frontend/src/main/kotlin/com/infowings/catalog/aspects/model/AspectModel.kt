package com.infowings.catalog.aspects.model

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData

interface AspectsModel {
    /**
     * Method for selecting existing aspect (or new but not yet saved if [AspectData.id] == null)
     *
     * if supplied [AspectData.id] does not exist in context (should not happen at all), does nothing
     *
     * @param aspectId - [AspectData.id] of [AspectData] to select.
     */
    fun selectAspect(aspectId: String?)

    /**
     * Method for selecting property of existing (or new) aspect by index of the property inside the aspect.
     *
     * The reason for selecting aspect property by index instead of by id or instance is that new aspect properties
     * have all the same id (empty string) and there are no restrictions on the state of the properties while editing,
     * which means that there may be two properties with exact same content inside one aspect (may be use case for
     * copying). Validation regarding possible restrictions is performed on server side.
     *
     * @param index - index of [AspectPropertyData] inside [AspectData.properties] list to select.
     */
    fun selectProperty(index: Int)

    /**
     * Method for canceling selected state.
     *
     * By default resets state to creating new aspect.
     */
    fun discardSelect()

    /**
     * Method for creating new [AspectPropertyData] inside currently selected [AspectData] at index.
     */
    fun createProperty(index: Int)

    /**
     * Method for updating currently selected [AspectData]
     *
     * Made suspended in case if submission to server happens immediately after a call to this method (setState should
     * complete before submission to the server).
     */
    fun updateAspect(aspect: AspectData)

    /**
     * Method for updating currently selected [AspectPropertyData]
     *
     * Made suspended in case if submission to server happens immediately after a call to this method (setState should
     * complete before submission to the server).
     */
    fun updateProperty(property: AspectPropertyData)

    /**
     * Method for submitting changes of currently selected [AspectData] to the server
     */
    suspend fun submitAspect()

    /**
     * Method for requesting delete of currently selected [AspectData] to the server
     */
    suspend fun deleteAspect(force: Boolean)

    suspend fun deleteAspectProperty(force: Boolean)
}
