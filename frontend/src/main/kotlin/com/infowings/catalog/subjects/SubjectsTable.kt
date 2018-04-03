package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.header
import com.infowings.catalog.components.searchbar.SearchBar
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
    var name: String = ""
)

class SubjectsTable : RComponent<SubjectApiReceiverProps, SubjectsTable.State>() {

    override fun componentWillUpdate(nextProps: SubjectApiReceiverProps, nextState: State) {
        val size = nextProps.data.size + 1
        state.subjectNames = Array(size, { "" })
    }

    private var timer: Int = 0

    override fun RBuilder.render() {
        child(SearchBar::class) {
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
                columns = arrayOf(column("name", header("name")))
                data = if (props.loading) emptyArray() else subjectToRows()
                showPagination = false
                pageSize = data.count()
                minRows = 0
                sortable = false
                showPageJump = false
                resizable = false
/*
                filterable = true
                onFilteredChange = { ewExpanded: dynamic, index: dynamic, event: dynamic ->
                    //filtering in server side
                    props.onFetchData((ewExpanded as Array<FilteringModel>).map { it.id to it.value }.toMap())
                }
                defaultFilterMethod =
                        { filter: dynamic, row: dynamic, column: dynamic -> true } //disable filtering in frontend
*/
            }
        }
    }

    private fun column(accessor: String, header: RClass<RTableRendererProps>) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = header
            this.Cell = rFunction("SubjectField") { props ->
                input(type = InputType.text, classes = "rtable-input") {
                    attrs {
                        defaultValue = props.value?.toString() ?: ""
                        autoFocus = true
                        onBlurFunction = {
                            updateSubject(props, it)
                        }
                        onKeyPressFunction = {
                            if (it.unsafeCast<KeyboardEvent>().key == "Enter") {
                                updateSubject(props, it)
                            }
                        }
                    }
                }

            }
        }

    private fun updateSubject(props: RTableRendererProps, event: Event) {
        state.subjectNames[props.index] = event.asDynamic().target.value as String
        onSaveChangedSubjectName(props.index)
    }

    private fun onSaveChangedSubjectName(index: Int) {
        setState {
            if (index < props.data.size) {
                if (state.subjectNames[index].isNotBlank()) {
                    val curSubjectData = props.data[index]
                    if (curSubjectData.name != state.subjectNames[index]) {
                        props.onSubjectUpdate(SubjectData(curSubjectData.id, state.subjectNames[index]))
                    }
                }
            } else if (state.subjectNames.last().isNotBlank()) {
                props.onSubjectsCreate(SubjectData(name = state.subjectNames.last()))
                state.subjectNames += ""
            }
        }
    }

    private fun subjectToRows(): Array<SubjectViewData> {
        return toSubjectViewData(props.data) + arrayOf(SubjectViewData())
    }

    interface State : RState {
        var subjectNames: Array<String>
        var filterText: String
    }

    private fun toSubjectViewData(data: Array<SubjectData>): Array<SubjectViewData> =
        if (data == undefined) emptyArray() else data.map { SubjectViewData(it.name) }.toTypedArray()

}