package com.infowings.catalog.objects.view

import com.infowings.catalog.components.paginationPanel
import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.filter.objectExcludeFilterComponent
import com.infowings.catalog.objects.filter.objectSubjectFilterComponent
import com.infowings.catalog.objects.mergeDetails
import com.infowings.catalog.objects.search.objectSearchComponent
import com.infowings.catalog.objects.toLazyView
import com.infowings.catalog.objects.view.sort.objectSort
import com.infowings.catalog.objects.view.tree.objectLazyTreeView
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinx.coroutines.Job
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
    }

    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun componentWillReceiveProps(nextProps: ObjectsViewApiConsumerProps) {
        if (props.objects.map { it.guid } != nextProps.objects.map { it.guid }) {
            setState {
                objects = nextProps.objects.toLazyView(nextProps.detailedObjectsView)
            }
        } else {
            setState {
                objects = objects.mergeDetails(nextProps.detailedObjectsView)
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
        objectLazyTreeView {
            attrs {
                objects = state.objects
                objectTreeViewModel = this@ObjectTreeViewModelComponent
                history = props.history
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
                            props.refreshObjects()
                        }
                    }
                }
                objectSearchComponent {
                    attrs {
                        onConfirmSearch = { query ->
                            props.onSearchQueryChanged(query)
                            props.refreshObjects()
                        } //onSearchQueryChanged
                    }
                }
            }

            div(classes = "object-header__text-filters") {
                objectSubjectFilterComponent {
                    attrs {
                        subjectsFilter = props.objectsFilter.subjects
                        onChange = {
                            props.onFilterChanged(props.objectsFilter.copy(subjects = it))
                        }
                    }
                }

                objectExcludeFilterComponent {
                    attrs {
                        selected = props.objectsFilter.excluded
                        onChange = {
                            props.onFilterChanged(props.objectsFilter.copy(excluded = it))
                        }
                    }
                }
            }

            div("object-header__refresh") {
                Button {
                    attrs {
                        icon = "refresh"
                        onClick = { props.refreshObjects() }
                    }
                }
            }
        }
    }

    interface State : RState {
        var objects: List<ObjectLazyViewModel>
    }
}

fun RBuilder.objectsViewModel(block: RHandler<ObjectsViewApiConsumerProps>) = child(ObjectTreeViewModelComponent::class, block)
