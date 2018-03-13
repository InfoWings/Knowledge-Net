package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.header
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.html.InputType
import kotlinx.html.contentEditable
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.i
import react.dom.input

data class SubjectViewData(var name: String = "", var aspectNames: List<String>? = null)

class SubjectsTable : RComponent<SubjectApiReceiverProps, SubjectsTable.State>() {

    override fun State.init(props: SubjectApiReceiverProps) {
        data = toSubjectViewData(props.data)
    }

    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                    column("name", header("Subject name")),
                    RTableColumnDescriptor {
                        this.accessor = "aspectNames"
                        this.Header = header("Aspect names")
                    }/*,
                    RTableColumnDescriptor {
                        this.accessor = "pending"
                        this.Header = addNewSubjectHeaderEnabled()
                        this.width = 55.0
                    }*/
                )
                if (!props.loading && state.data == undefined) {
                    state.data = subjectToRows()
                }
                data = if (props.loading) emptyArray() else state.data
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
                resizable = false
                collapseOnDataChange = false
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
                        value = props.value?.toString() ?: ""
                        contentEditable = true
                        onChangeFunction = { onSubjectChanged(props.index, it.asDynamic().target.value) }
                        onBlurFunction = { onSaveChanged(props.index, it.asDynamic().target.value) }
                    }
                }
            }
        }

    private fun onSubjectChanged(index: Int, value: dynamic) {
        setState {
            if (index < data.size) {
                data[index] = SubjectViewData(value, data[index].aspectNames)
            } else {
                editable[0] = SubjectViewData(value)
            }
        }
    }

    private fun onSaveChanged(index: Int, value: dynamic) {
        if (index < props.data.size) {
            val curSubjectData = props.data[index]
            props.onSubjectUpdate(SubjectData(curSubjectData.id, state.data[index].name, curSubjectData.aspectIds))
        } else {
            props.onSubjectsCreate(SubjectData(name = state.editable[0].name))
        }
    }

    /**
     * Component that represents green "+" sign when there is no new aspect being edited.
     * Receives callback on click.
     */
    private fun addNewSubjectHeaderEnabled(): RClass<RTableRendererProps> =
        rFunction("CheckboxHeaderEnabled") {
            div(classes = "create-new-aspect-container") {
                i(classes = "fas fa-plus") {}
                attrs.onClickFunction = { startCreatingNewSubject() }
            }
        }

    private fun startCreatingNewSubject() {
        //TODO scroll down and focus to last row!
    }

    private fun subjectToRows(): Array<SubjectViewData> {
        if (state.editable == undefined) {
            state.editable = arrayOf(SubjectViewData())
        }
        return toSubjectViewData(props.data) + state.editable
    }


    interface State : RState {
        var data: Array<SubjectViewData>
        var editable: Array<SubjectViewData>
    }

    private fun toSubjectViewData(data: Array<SubjectData>): Array<SubjectViewData> =
        if (data == undefined) {
            emptyArray()
        } else {
            data.map { SubjectViewData(it.name, it.aspectIds) }.toTypedArray()
        }
}