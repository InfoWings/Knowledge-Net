package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.json.JSON
import react.*

suspend fun getAllSubjects(): SubjectsList =
    JSON.parse(get("/api/subject/all"))

suspend fun createSubject(body: SubjectData): SubjectData =
    JSON.parse(post("/api/subject/create", JSON.stringify(body)))

suspend fun updateSubject(body: SubjectData): SubjectData =
    JSON.parse(post("/api/subject/update", JSON.stringify(body)))

suspend fun getSuggestedSubject(subjectText: String, aspectText: String): SubjectsList =
    JSON.parse(get("/api/search/subject/suggestion?text=$subjectText&aspectText=$aspectText"))


interface SubjectApiReceiverProps : RProps {
    var data: Array<SubjectData>
    var loading: Boolean
    var onSubjectUpdate: (sd: SubjectData) -> Unit
    var onSubjectsCreate: (sd: SubjectData) -> Unit
    var onFetchData: (filterParam: Map<String, String>) -> Unit
}

class SubjectApiMiddleware : RComponent<RProps, SubjectApiMiddleware.State>() {

    override fun State.init() {
        data = mutableMapOf()
        loading = true
    }

    override fun componentDidMount() {
        launch {
            val subjects = getAllSubjects()
            setState {
                data = subjects.subject.map { it.id to it }.toMap().toMutableMap()
                loading = false
            }
        }
    }

    private fun handleCreateSubject(subjectData: SubjectData) {
        launch {
            val res = createSubject(subjectData)
            setState {
                data[res.id] = res
                loading = false
            }

        }
    }

    private fun handleUpdateSubject(subjectData: SubjectData) {
        launch {
            val res = updateSubject(subjectData)
            setState {
                data[res.id] = res
                loading = false
            }
        }
    }

    private fun handleFetchData(filterParam: Map<String, String>) {
        launch {
            val subjects = getSuggestedSubject(filterParam["name"] ?: "", filterParam["aspects"] ?: "")
            setState {
                data = subjects.subject.map { it.id to it }.toMap().toMutableMap()
                loading = false
                renew = true
            }
        }

    }

    override fun RBuilder.render() {
        child(SubjectsTable::class) {
            attrs {
                console.log(" SubjectApiMiddleware state.data: ", state.data)
                data = state.data.values.toTypedArray()
                loading = state.loading
                onSubjectUpdate = ::handleUpdateSubject
                onSubjectsCreate = ::handleCreateSubject
                onFetchData = ::handleFetchData
            }
        }
    }

    interface State : RState {
        var data: MutableMap<String?, SubjectData>
        var loading: Boolean
        var renew: Boolean
    }

}



