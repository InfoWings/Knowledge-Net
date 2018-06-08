package com.infowings.catalog.components.reference

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.layout.header
import com.infowings.catalog.subjects.getSubjectByName
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.toMap
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

class ReferencePage : RComponent<RouteSuppliedProps, ReferencePage.State>() {

    override fun componentDidMount() {
        setState {
            loading = true
        }

        launch {
            val subject = getSubjectByName(props.match.params.toMap()["subjectName"]!!)
            setState {
                this.subject = subject
                loading = false
            }
        }
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