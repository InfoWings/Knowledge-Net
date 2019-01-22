package com.infowings.catalog.components.reference

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.layout.header
import com.infowings.catalog.subjects.getSubjectByName
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.utils.decodeURIComponent
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.toMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

class ReferencePage : RComponent<RouteSuppliedProps, ReferencePage.State>(), JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun componentDidMount() {
        job = Job()

        setState {
            loading = true
        }

        launch {
            val subject = getSubjectByName(decodeURIComponent(props.match.params.toMap()["subjectName"]!!))
            setState {
                this.subject = subject
                loading = false
            }
        }
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun RBuilder.render() {
        header {
            attrs {
                location = props.location.pathname
                history = props.history
            }
        }
        val subject = state.subject
        if (!state.loading && subject != null) {
            referenceComponent {
                attrs.subject = subject
            }
        }
    }

    interface State : RState {
        var subject: SubjectData?
        var loading: Boolean
    }
}