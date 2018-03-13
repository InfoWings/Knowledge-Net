package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import com.infowings.catalog.utils.put
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.json.JSON
import react.*

suspend fun getAllSubjects(): Array<SubjectData> =
    kotlin.js.JSON.parse(get("/api/subject/all"))

suspend fun createSubject(body: SubjectData): SubjectData =
    JSON.parse(post("/api/subject/create", JSON.stringify(body)))

suspend fun updateSubject(body: SubjectData): SubjectData =
    JSON.parse(put("/api/subject/update", JSON.stringify(body)))


interface SubjectApiReceiverProps : RProps {
    var data: Array<SubjectData>
    var loading: Boolean
    var onSubjectUpdate: (changedAspect: SubjectData) -> Unit
    var onSubjectsCreate: (newAspect: SubjectData) -> Unit
}

class SubjectApiMiddleware : RComponent<RProps, SubjectApiMiddleware.State>() {

    override fun State.init() {
        data = emptyArray()
        loading = true
    }

    override fun componentDidMount() {
        launch {
            val subjects = getAllSubjects()
            setState {
                data = subjects
                loading = false
            }
        }
    }

    private fun handleCreateSubject(subjectData: SubjectData) {
        launch {
            val res = createSubject(subjectData)
            setState {
                data += res
            }

        }
    }

    private fun handleUpdateSubject(subjectData: SubjectData) {
        launch {
            updateSubject(subjectData)
        }
    }


    override fun RBuilder.render() {
        child(SubjectsTable::class) {
            attrs {
                console.log(" SubjectApiMiddleware state.data: ", state.data)
                data = state.data
                loading = state.loading
                onSubjectUpdate = ::handleUpdateSubject
                onSubjectsCreate = ::handleCreateSubject
            }
        }
    }

    interface State : RState {
        var data: Array<SubjectData>
        var loading: Boolean
    }


}



