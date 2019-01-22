package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.utils.*
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JSON
import react.RBuilder
import react.RComponent
import react.RState
import react.setState
import kotlin.collections.set

private external fun encodeURIComponent(component: String): String = definedExternally

suspend fun getAllSubjects(): SubjectsList =
    JSON.parse(SubjectsList.serializer(), get("/api/subject/all"))

suspend fun getSubjectByName(name: String): SubjectData =
    JSON.parse(SubjectData.serializer(), get("/api/subject/get/${encodeURIComponent(name)}"))

suspend fun getSubjectById(id: String): SubjectData {
    val encoded = encodeURIComponent(id)
    return JSON.parse(SubjectData.serializer(), get("/api/subject/$encoded"))
}

suspend fun createSubject(body: SubjectData): SubjectData =
    JSON.parse(SubjectData.serializer(), post("/api/subject/create", JSON.stringify(SubjectData.serializer(), body)))

suspend fun updateSubject(body: SubjectData): SubjectData =
    JSON.parse(SubjectData.serializer(), post("/api/subject/update", JSON.stringify(SubjectData.serializer(), body)))

suspend fun getSuggestedSubject(subjectText: String, aspectText: String): SubjectsList =
    JSON.parse(SubjectsList.serializer(), get("/api/search/subject/suggestion?text=$subjectText&aspectText=$aspectText"))

suspend fun deleteSubject(body: SubjectData, force: Boolean) {
    if (force) {
        post("/api/subject/forceRemove", JSON.stringify(SubjectData.serializer(), body))
    } else {
        post("/api/subject/remove", JSON.stringify(SubjectData.serializer(), body))
    }
}


interface SubjectApiReceiverProps : RouteSuppliedProps {
    var data: Array<SubjectData>
    var loading: Boolean
    var onSubjectUpdate: suspend (SubjectData) -> Unit
    var onSubjectsCreate: suspend (SubjectData) -> Unit
    var onSubjectDelete: suspend (SubjectData, force: Boolean) -> Unit
    var onFetchData: (filterParam: Map<String, String>) -> Unit
    var refreshSubjects: () -> Unit
}

class SubjectApiMiddleware : RComponent<RouteSuppliedProps, SubjectApiMiddleware.State>(), JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun State.init() {
        data = mutableMapOf()
        loading = true
    }

    override fun componentDidMount() {
        job = Job()
        loadAllData()
    }

    private fun loadAllData() {
        setState {
            loading = true
        }
        job.cancelChildren()
        launch {
            val subjects = getAllSubjects()
            setState {
                data = subjects.subject.map { it.id to it }.toMap(mutableMapOf())
                loading = false
            }
        }
    }

    private suspend fun handleCreateSubject(subjectData: SubjectData) {
        setState {
            loading = true
        }
        val res = createSubject(subjectData)
        setState {
            data[res.id] = res
            loading = false
        }
    }

    private suspend fun handleUpdateSubject(subjectData: SubjectData) {
        setState {
            loading = true
            data[subjectData.id] = subjectData
        }

        val res = try {
            updateSubject(subjectData)
        } catch (e: NotModifiedException) {
            val subjectId = subjectData.id
            if (subjectId != null) getSubjectById(subjectId) else subjectData
        }

        setState {
            data[res.id] = res
            loading = false
        }
    }

    private fun handleFetchData(filterParam: Map<String, String>) {
        if (filterParam["name"].isNullOrEmpty()) {
            loadAllData()
            return
        }
        job.cancelChildren()
        setState {
            loading = true
        }
        job = launch {
            val subjects = getSuggestedSubject(filterParam["name"] ?: "", "")
            setState {
                data = subjects.subject.map { it.id to it }.toMap().toMutableMap()
                loading = false
            }
        }

    }

    private suspend fun handleDeleteSubject(subjectData: SubjectData, force: Boolean) {
        deleteSubject(subjectData, force)
        setState {
            data.remove(subjectData.id)
        }
    }

    override fun RBuilder.render() {
        child(SubjectsListComponent::class) {
            attrs {
                history = props.history
                location = props.location
                match = props.match
                data = state.data.values.toTypedArray()
                loading = state.loading
                onSubjectUpdate = { handleUpdateSubject(it) }
                onSubjectsCreate = { handleCreateSubject(it) }
                onSubjectDelete = { subjectData, force -> handleDeleteSubject(subjectData, force) }
                onFetchData = ::handleFetchData
                refreshSubjects = ::loadAllData
            }
        }
    }

    interface State : RState {
        var data: MutableMap<String?, SubjectData>
        var loading: Boolean
    }

}