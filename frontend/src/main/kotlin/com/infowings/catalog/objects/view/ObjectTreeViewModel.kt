package com.infowings.catalog.objects.view

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.objects.*
import com.infowings.catalog.objects.filter.ObjectsFilter
import com.infowings.catalog.objects.filter.objectExcludeFilterComponent
import com.infowings.catalog.objects.filter.objectSubjectFilterComponent
import com.infowings.catalog.objects.view.tree.objectLazyTreeView
import com.infowings.catalog.utils.ServerException
import com.infowings.catalog.wrappers.blueprint.Button
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div

interface ObjectsLazyModel {
    fun requestDetailed(id: String)
    fun updateObject(index: Int, block: ObjectLazyViewModel.() -> Unit)
}

class ObjectTreeViewModelComponent(props: ObjectsViewApiConsumerProps) : RComponent<ObjectsViewApiConsumerProps, ObjectTreeViewModelComponent.State>(props),
    ObjectsLazyModel {

    override fun State.init(props: ObjectsViewApiConsumerProps) {
        objects = props.objects.toLazyView(props.detailedObjectsView)
        filterBySubject = ObjectsFilter(emptyList(), emptyList())
    }

    override fun componentWillReceiveProps(nextProps: ObjectsViewApiConsumerProps) {
        if (props.objects != nextProps.objects) {
            setState {
                objects = nextProps.objects.toLazyView(nextProps.detailedObjectsView)
            }
        } else {
            setState {
                objects = objects.mergeDetails(nextProps.detailedObjectsView)
            }
        }
    }

    private fun refreshObjects() {
        val oldByGuid = state.objects.filter { it.guid != null }.map { it.guid to it }.toMap()

        val withDetails = oldByGuid.filter { it.value.objectProperties != null }.keys

        launch {
            try {
                val response = getAllObjects().objects

                val freshByGuid = response.filter { it.guid != null }.map { it.guid to it }.toMap()

                val detailsNeeded = response.filter {
                    if (oldByGuid.containsKey(it.guid)) {
                        val viewModel = oldByGuid[it.guid] ?: throw IllegalStateException("no view model")
                        viewModel.objectProperties != null
                    } else false
                }

                val freshDetails = detailsNeeded.map { it.id to getDetailedObject(it.id) }.toMap()

                val enriched = response.toLazyView(freshDetails).map {
                    val viewModel = oldByGuid[it.guid]
                    if (viewModel == null) {
                        it
                    } else {
                        it.copy(expanded = viewModel.expanded, expandAllFlag = viewModel.expandAllFlag)
                    }
                }

                setState {
                    objects = enriched
                }
            } catch (exception: ServerException) {
                println("something wrong: $exception")
            }

        }

    }

    override fun requestDetailed(id: String) {
        props.objectApiModel.fetchDetailedObject(id)
    }

    override fun updateObject(index: Int, block: ObjectLazyViewModel.() -> Unit) = setState {
        objects[index].block()
    }

    override fun RBuilder.render() {
        div(classes = "object-header__text-filters") {

            objectSubjectFilterComponent {
                attrs {
                    subjectsFilter = state.filterBySubject.subjects
                    onChange = ::setSubjectsFilter
                }
            }

            objectExcludeFilterComponent {
                attrs {
                    selected = state.filterBySubject.excluded
                    onChange = {
                        setState {
                            filterBySubject = filterBySubject.copy(excluded = it)
                        }
                    }
                }
            }

            div("object-header__refresh") {
                Button {
                    attrs {
                        icon = "refresh"
                        onClick = {
                            refreshObjects()
                        }
                    }
                }
            }
        }


        val subjectNames = state.filterBySubject.subjects.mapNotNull { it?.name }.toSet()
        val excludedGuids = state.filterBySubject.excluded.mapNotNull { it.guid }.toSet()

        val relevantIndices = if (subjectNames.isEmpty()) state.objects.indices.toList() else state.objects
            .mapIndexedNotNull { index: Int, model: ObjectLazyViewModel ->
                if (model.subjectName in subjectNames || excludedGuids.contains(model.guid)) index else null
            }

        objectLazyTreeView {
            attrs {
                indices = relevantIndices
                objects = state.objects //if (subjectNames.isEmpty()) state.objects else state.objects
                //.filter { it.subjectName in subjectNames || excludedGuids.contains(it.guid) }
                objectTreeViewModel = this@ObjectTreeViewModelComponent
            }
        }
    }

    private fun setSubjectsFilter(subjects: List<SubjectData?>) = setState {
        filterBySubject = filterBySubject.copy(subjects = subjects)
    }


    interface State : RState {
        var objects: List<ObjectLazyViewModel>
        var filterBySubject: ObjectsFilter
    }
}

fun RBuilder.objectsViewModel(block: RHandler<ObjectsViewApiConsumerProps>) =
    child(ObjectTreeViewModelComponent::class, block)
