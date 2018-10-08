package com.infowings.catalog.objects.view

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.filter.ObjectsFilter
import com.infowings.catalog.objects.filter.objectExcludeFilterComponent
import com.infowings.catalog.objects.filter.objectSubjectFilterComponent
import com.infowings.catalog.objects.mergeDetails
import com.infowings.catalog.objects.toLazyView
import com.infowings.catalog.objects.view.tree.objectLazyTreeView
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
        }

        val subjectNames = state.filterBySubject.subjects.mapNotNull { it?.name }.toSet()
        val excludedGuids = state.filterBySubject.excluded.mapNotNull { it.guid }.toSet()

        objectLazyTreeView {
            attrs {
                objects = if (subjectNames.isEmpty()) state.objects else state.objects
                    .filter { it.subjectName in subjectNames || excludedGuids.contains(it.guid) }
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
