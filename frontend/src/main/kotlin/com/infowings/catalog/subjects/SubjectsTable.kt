package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.header
import com.infowings.catalog.components.searchbar.searchBar
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.html.InputType
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onKeyPressFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.defaultValue
import react.dom.input
import kotlin.browser.window

data class SubjectViewData(
    var name: String = "",
    var description: String? = ""
)

class SubjectsTable : RComponent<SubjectApiReceiverProps, SubjectsTable.State>() {

    override fun componentWillUpdate(nextProps: SubjectApiReceiverProps, nextState: State) {
        val size = nextProps.data.size + 1
        state.subjectNames = Array(size, { SubjectViewData() })
    }

    private var timer: Int = 0

    override fun RBuilder.render() {
        searchBar {
            attrs {
                filterText = state.filterText
                onFilterTextChange = { text ->
                    window.clearTimeout(timer)
                    timer = window.setTimeout({ props.onFetchData(mapOf("name" to text)) }, 200)
                }
            }
        }

        ReactTable {
            attrs {
                columns = arrayOf(
                    column("name", header("name"), ::updateSubjectName),
                    column("description", header("description"), ::updateSubjectDesc)
                )
                data = if (props.loading) emptyArray() else subjectToRows()
                showPagination = false
                pageSize = data.count()
                minRows = 0
                sortable = false
                showPageJump = false
                resizable = false
            }
        }
    }

    private fun column(
        accessor: String,
        header: RClass<RTableRendererProps>,
        updater: (props: RTableRendererProps, event: Event) -> Unit
    ) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = header
            this.Cell = rFunction("SubjectField") { props ->
                input(type = InputType.text, classes = "rtable-input") {
                    attrs {
                        defaultValue = props.value?.toString() ?: ""
                        autoFocus = true
                        onBlurFunction = {
                            updater(props, it)
                        }
                        onKeyPressFunction = {
                            if (it.unsafeCast<KeyboardEvent>().key == "Enter") {
                                updater(props, it)
                            }
                        }
                    }
                }

            }
        }

    private fun updateSubjectName(props: RTableRendererProps, event: Event) {
        state.subjectNames[props.index].name = event.asDynamic().target.value as String
        onSaveChangedSubjectName(props.index)
    }

    private fun updateSubjectDesc(props: RTableRendererProps, event: Event) {
        state.subjectNames[props.index].description = event.asDynamic().target.value as String
        onSaveChangedSubjectName(props.index)
    }

    private fun onSaveChangedSubjectName(index: Int) {
        setState {
            if (index < props.data.size) {
                if (state.subjectNames[index].name.isNotBlank()) {
                    val curSubjectData = props.data[index]
                    if (curSubjectData.name != state.subjectNames[index].name ||
                        curSubjectData.description != state.subjectNames[index].description
                    ) {
                        props.onSubjectUpdate(
                            SubjectData(
                                curSubjectData.id,
                                state.subjectNames[index].name,
                                state.subjectNames[index].description
                            )
                        )
                    }
                }
            } else {
                val last = state.subjectNames.last()
                if (last.name.isNotBlank()) {
                    props.onSubjectsCreate(SubjectData(name = last.name, description = last.description))
                    state.subjectNames += SubjectViewData()
                }
            }
        }
    }

    private fun subjectToRows(): Array<SubjectViewData> {
        return toSubjectViewData(props.data) + arrayOf(SubjectViewData())
    }

    interface State : RState {
        var subjectNames: Array<SubjectViewData>
        var filterText: String
    }

    private fun toSubjectViewData(data: Array<SubjectData>): Array<SubjectViewData> =
        if (data == undefined) emptyArray() else data.map { SubjectViewData(it.name, it.description) }.toTypedArray()

}