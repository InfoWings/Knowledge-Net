package com.infowings.catalog.history

import com.infowings.catalog.common.AspectDataView
import com.infowings.catalog.common.EventKind
import com.infowings.catalog.common.HistoryData
import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.utils.userIcon
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.a
import react.dom.div
import react.dom.span
import kotlin.js.Date

class HistoryEventComponent : RComponent<HistoryEventComponent.Props, HistoryEventComponent.State>() {

    override fun State.init(props: HistoryEventComponent.Props) {
        showFullVersion = false
        showDiff = false
    }

    private fun openFullVersion() {
        setState {
            showFullVersion = true
        }
    }

    private fun openDiffVersion() {
        setState {
            showDiff = true
        }
    }

    override fun RBuilder.render() {
        div("history-item") {
            div(classes = "history-item-field history-item--label-user") {
                userIcon("history-item-user-icon") { }
                span {
                    +props.historyData.user
                }
            }
            span(classes = "history-item-field history-item--label-event-${props.historyData.event.color}") {
                +props.historyData.event.name
            }

            span(classes = "history-item-field history-item--label-class") {
                +props.historyData.entityName
            }
            span(classes = "history-item-field history-item--label-guid") {
                a(classes = "no-underline") {
                    attrs.onClickFunction = { openDiffVersion() }
                    +props.historyData.info
                }
            }
            if (props.historyData.deleted) {
                ripIcon("history-item-field aspect-tree-view--rip-icon") {}
            }
            span(classes = "history-item-field history-item--label-timestamp") {
                +Date(props.historyData.timestamp).toDateString()
            }
            span(classes = "history-item-field history-item--label-version") {
                a(classes = "no-underline") {
                    attrs.onClickFunction = { openFullVersion() }
                    +"ver. ${props.historyData.version}"
                }
            }
        }
        if (state.showFullVersion) {
            when (props.historyData.fullData) {
                is AspectDataView -> aspectFullContainer {
                    attrs {
                        view = props.historyData.fullData as AspectDataView
                        onExit = { setState { showFullVersion = false } }
                    }
                }
                else -> Unit
            }
        }
        if (state.showDiff) {
            aspectDiffContainer {
                attrs {
                    changes = props.historyData.changes
                    onExit = { setState { showDiff = false } }
                }
            }
        }
    }

    interface Props : RProps {
        var historyData: HistoryData<*>
    }

    interface State : RState {
        var showFullVersion: Boolean
        var showDiff: Boolean
    }
}

fun RBuilder.historyEventComponent(handler: RHandler<HistoryEventComponent.Props>) =
    child(HistoryEventComponent::class, handler)

val EventKind.color: String
    get() = when (this) {
        EventKind.CREATE -> "green"
        EventKind.UPDATE -> "yellow"
        EventKind.DELETE -> "red"
        EventKind.SOFT_DELETE -> "red"
    }