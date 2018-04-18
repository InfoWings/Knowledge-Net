package com.infowings.catalog.history

import com.infowings.catalog.common.HistoryData
import com.infowings.catalog.layout.header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinext.js.invoke
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.div
import react.setState

class HistoryPage : RComponent<RouteSuppliedProps, HistoryPage.State>() {

    companion object {
        init {
            kotlinext.js.require("styles/historyList.scss")
        }
    }

    override fun State.init() {
        data = emptyList()
        loading = true
    }

    override fun componentDidMount() {
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