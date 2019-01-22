package com.infowings.catalog.history

import com.infowings.catalog.common.HistoryData
import com.infowings.catalog.layout.header
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.div
import react.setState

class HistoryPage : RComponent<RouteSuppliedProps, HistoryPage.State>(), JobCoroutineScope by JobSimpleCoroutineScope() {

    companion object {
        init {
            kotlinext.js.require("styles/historyList.scss")
        }
    }

    override fun State.init() {
        data = emptyList()
        loading = true
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun componentDidMount() {
        job = Job()
        launch {
            val response = getAllEvents()
            setState {
                data = response
                loading = false
            }
        }
    }

    override fun RBuilder.render() {
        header {
            attrs.location = props.location.pathname
            attrs.history = props.history
        }

        if (!state.loading) {
            div("history-group") {
                state.data.forEach {
                    historyEventComponent {
                        attrs.historyData = it
                    }
                }
            }
        }
    }

    interface State : RState {
        var data: List<HistoryData<*>>
        var loading: Boolean
    }
}