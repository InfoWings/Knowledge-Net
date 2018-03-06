package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import kotlinx.coroutines.experimental.launch
import react.*

class SubjectApiMiddleware : RComponent<RProps, SubjectApiMiddleware.State>() {

    override fun componentDidMount() {
        launch {
            val subjects = getAllSubjects()
            setState {
                data = subjects
                loading = false
            }
        }
    }

    override fun RBuilder.render() {
        child(SubjectsTable::class) {
            attrs {
                console.log(" SubjectApiMiddleware state.data: ", state.data)
                data = toSubjectViewData(state.data)
                loading = state.loading
            }
        }
    }

    interface State : RState {
        var data: Array<SubjectData>
        var loading: Boolean
    }

    private fun toSubjectViewData(data: Array<SubjectData>): Array<SubjectViewData> =
        if (state.data == undefined) {
            emptyArray()
        } else {
            state.data.map { SubjectViewData(it.name, it.aspectIds) }.toTypedArray()
        }

}



