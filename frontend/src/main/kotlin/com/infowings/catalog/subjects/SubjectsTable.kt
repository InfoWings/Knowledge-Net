package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.header
import com.infowings.catalog.wrappers.table.FilteringModel
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.html.InputType
import kotlinx.html.js.onBlurFunction
import react.*
import react.dom.defaultValue
import react.dom.input

data class SubjectViewData(
    var name: String = ""
)

class SubjectsTable : RComponent<SubjectApiReceiverProps, SubjectsTable.State>() {

    override fun componentWillUpdate(nextProps: SubjectApiReceiverProps, nextState: State) {
        val size = nextProps.data.size + 1
        state.subjectNames = Array(size, { "" })
    }

    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(column("name", header("name")))
                data = if (props.loading) emptyArray() else subjectToRows()
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
                resizable = false
                collapseOnDataChange = false
                filterable = true
                onFilteredChange = { ewExpanded: dynamic, index: dynamic, event: dynamic ->
                    //filtering in server side
                    props.onFetchData((ewExpanded as Array<FilteringModel>).map { it.id to it.value }.toMap())
                }
                defaultFilterMethod =
                        { filter: dynamic, row: dynamic, column: dynamic -> true } //disable filtering in frontend
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
                        onBlurFunction = {
                            state.subjectNames[props.index] = it.asDynamic().target.value as String
                            onSaveChangedSubjectName(props.index)
                        }
                    }
                }

            }
        }

    private fun onSaveChangedSubjectName(index: Int) {
        setState {
            if (index < props.data.size) {
                if (state.subjectNames[index].isNotBlank()) {
                    val curSubjectData = props.data[index]
                    props.onSubjectUpdate(SubjectData(curSubjectData.id, state.subjectNames[index]))
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
    }

    private fun toSubjectViewData(data: Array<SubjectData>): Array<SubjectViewData> =
        if (data == undefined) emptyArray() else data.map { SubjectViewData(it.name) }.toTypedArray()

}