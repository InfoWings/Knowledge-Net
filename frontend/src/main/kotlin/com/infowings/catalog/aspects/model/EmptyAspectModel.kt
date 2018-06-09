package com.infowings.catalog.aspects.model

import com.infowings.catalog.aspects.filter.AspectsFilter
import com.infowings.catalog.aspects.getAllAspects
import com.infowings.catalog.aspects.treeview.AspectNodeExpandedStateWrapper
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectOrderBy
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.utils.ServerException
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div

class EmptyAspectModelComponent : RComponent<EmptyAspectModelComponent.Props, EmptyAspectModelComponent.State>(), AspectsModel {

    override fun selectAspect(aspectId: String?) {}

    override fun selectProperty(index: Int) {}

    override fun discardSelect() {}

    override fun createProperty(index: Int) {}

    override fun updateAspect(aspect: AspectData) {}

    override fun updateProperty(property: AspectPropertyData) {}

    override suspend fun submitAspect() {}

    override suspend fun deleteAspect(force: Boolean) {}

    override suspend fun deleteAspectProperty() {}

    override fun EmptyAspectModelComponent.State.init() {
        data = emptyList()
        loading = true
        serverError = false
    }

    override fun componentDidMount() {
        fetchAspects()
    }

    private fun fetchAspects(orderBy: List<AspectOrderBy> = emptyList()) {
        launch {
            try {
                val response = getAllAspects(orderBy)
                setState {
                    data = response.aspects
                    context = response.aspects.associate { Pair(it.id!!, it) }.toMutableMap()
                    loading = false
                }
            } catch (exception: ServerException) {
                setState {
                    data = emptyList()
                    context = mutableMapOf()
                    loading = false
                    serverError = true
                }
            }
        }
    }

    override fun RBuilder.render() {
        if (!state.loading) {
            val aspects = props.aspectsFilter.applyToAspects(state.data)
            val notDeletedAspects = aspects.filterNot { it.deleted }
            if (notDeletedAspects.isEmpty()) {
                +props.emptyMessage
            } else {
                div(classes = "aspect-tree-view") {
                    notDeletedAspects.map { aspect ->
                        child(AspectNodeExpandedStateWrapper::class) {
                            attrs {
                                this.aspect = aspect
                                selectedAspectId = null
                                selectedPropertyIndex = null
                                this.aspectsModel = this@EmptyAspectModelComponent
                                aspectContext = state.context
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectsFilter: AspectsFilter
        var emptyMessage: String
    }

    interface State : RState {

        /**
         * Last fetched data from server (actual)
         */
        var data: List<AspectData>
        /**
         * Flag showing if the data is still being fetched
         */
        var loading: Boolean
        /**
         * Map from AspectId to actual AspectData objects. Necessary for reconstructing tree structure
         * (AspectPropertyData contains aspectId)
         */
        var context: MutableMap<String, AspectData>
        /**
         * Server error happened
         */
        var serverError: Boolean
    }
}

fun RBuilder.emptyAspectModelComponent(handler: RHandler<EmptyAspectModelComponent.Props>) = child(EmptyAspectModelComponent::class, handler)