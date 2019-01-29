package com.infowings.catalog.objects.view

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.components.paginationPanel
import com.infowings.catalog.objects.*
import com.infowings.catalog.objects.filter.ObjectsFilter
import com.infowings.catalog.objects.filter.objectExcludeFilterComponent
import com.infowings.catalog.objects.filter.objectSubjectFilterComponent
import com.infowings.catalog.objects.search.objectSearchComponent
import com.infowings.catalog.objects.view.sort.objectSort
import com.infowings.catalog.objects.view.tree.objectLazyTreeView
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.utils.ServerException
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.*
import react.dom.div

interface ObjectsLazyModel {
    fun requestDetailed(id: String)
    fun updateObject(index: Int, block: ObjectLazyViewModel.() -> Unit)
}

class ObjectTreeViewModelComponent(props: ObjectsViewApiConsumerProps) : RComponent<ObjectsViewApiConsumerProps, ObjectTreeViewModelComponent.State>(props),
    ObjectsLazyModel, JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun State.init(props: ObjectsViewApiConsumerProps) {
        objects = props.objects.toLazyView(props.detailedObjectsView)
        filterBySubject = ObjectsFilter(emptyList(), emptyList())
    }

    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
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

        launch {
            try {
                val response = getAllObjects(props.orderBy, props.query, props.paginationData.offset, props.paginationData.limit).objects
                val detailsNeeded = response.filter { oldByGuid[it.guid]?.objectProperties != null ?: false }
                val freshDetails = detailsNeeded.map { it.id to getDetailedObject(it.id) }.toMap()

                val enriched = response.toLazyView(freshDetails).map { model ->
                    oldByGuid[model.guid]?.let { model.copy(expanded = it.expanded, expandAllFlag = it.expandAllFlag) } ?: model
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
        header()
        objectTreeView()
        pagination()
    }

    private fun RBuilder.objectTreeView() {
        val subjectNames = state.filterBySubject.subjects.mapNotNull { it?.name }.toSet()
        val excludedGuids = state.filterBySubject.excluded.mapNotNull { it.guid }.toSet()

        val relevantIndices = when {
            subjectNames.isEmpty() -> state.objects.indices.toList()
            else -> state.objects.mapIndexedNotNull { index: Int, model: ObjectLazyViewModel ->
                if (model.subjectName in subjectNames || model.guid in excludedGuids) index else null
            }
        }

        objectLazyTreeView {
            val currProps = props
            attrs {
                indices = relevantIndices
                objects = state.objects
                objectTreeViewModel = this@ObjectTreeViewModelComponent
                history = currProps.history
            }
        }
    }

    private fun RBuilder.pagination() {
        div(classes = "object-tree-view__header object-header") {
            div(classes = "object-header__pages") {
                paginationPanel {
                    paginationData = props.paginationData
                    onPageSelect = { props.onPage(it) }
                }
            }
        }
    }

    private fun RBuilder.header() {
        div(classes = "object-tree-view__header object-header") {
            div(classes = "object-header__sort-search") {
                Button {
                    attrs {
                        icon = "plus"
                        onClick = { props.history.push("/objects/new") }
                        intent = Intent.SUCCESS
                        text = "New object".asReactElement()
                    }
                }
                objectSort {
                    attrs {
                        this.onOrderByChanged = { orderBy ->
                            props.onOrderByChanged(orderBy)
                            refreshObjects()
                        }
                    }
                }
                objectSearchComponent {
                    attrs {
                        onConfirmSearch = { query ->
                            props.onSearchQueryChanged(query)
                            refreshObjects()
                        } //onSearchQueryChanged
                    }
                }
            }

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
    }

    private fun setSubjectsFilter(subjects: List<SubjectData?>) = setState {
        filterBySubject = filterBySubject.copy(subjects = subjects)
    }


    interface State : RState {
        var objects: List<ObjectLazyViewModel>
        var filterBySubject: ObjectsFilter
    }
}

fun RBuilder.objectsViewModel(block: RHandler<ObjectsViewApiConsumerProps>) = child(ObjectTreeViewModelComponent::class, block)
