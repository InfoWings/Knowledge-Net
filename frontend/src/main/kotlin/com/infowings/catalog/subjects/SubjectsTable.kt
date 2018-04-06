package com.infowings.catalog.subjects

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.emptySubjectData
import com.infowings.catalog.common.header
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.units.SearchBar
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.html.InputType
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onKeyPressFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.defaultValue
import react.dom.div
import react.dom.input
import kotlin.browser.window

data class SubjectRowData(
    val data: SubjectData
)

class SubjectsTable : RComponent<SubjectApiReceiverProps, SubjectsTable.State>() {

    private var timer: Int = 0
    private val subjectRows
        get() = props.data.map { SubjectRowData(it) }.toTypedArray()

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
                columns = arrayOf(
                    subjectColumn("data", header("Subject"))
                )
                data = if (props.loading) emptyArray() else subjectRows + SubjectRowData(emptySubjectData)
                showPagination = false
                pageSize = data.count() + 1
                minRows = 0
                sortable = false
                showPageJump = false
                resizable = false
            }
        }
    }

    private fun subjectColumn(
        accessor: String,
        header: RClass<RTableRendererProps>
    ) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = header
            this.Cell = rFunction("SubjectField") { props ->
                val subjectData = props.value as SubjectData
                div(classes = "subject-row") {
                    input(type = InputType.text, classes = "rtable-input") {
                        attrs {
                            defaultValue = subjectData.name
                            onBlurFunction = {
                                val inputValue = it.target.unsafeCast<HTMLInputElement>().value
                                updateSubjectName(inputValue, subjectData)
                            }
                            onKeyPressFunction = {
                                if (it.unsafeCast<KeyboardEvent>().key == "Enter") {
                                    val inputValue = it.target.unsafeCast<HTMLInputElement>().value
                                    updateSubjectName(inputValue, subjectData)
                                }
                            }
                        }
                    }
                    descriptionComponent(
                        className = "subject-description",
                        description = subjectData.description,
                        onNewDescriptionConfirmed = { updateSubjectDescription(it, subjectData) },
                        onEditStarted = null
                    )
                }
            }
        }

    private fun updateSubjectName(newName: String, subjectData: SubjectData) {
        if (subjectData.name != newName && newName.isNotBlank()) {
            submitSubject(subjectData.copy(name = newName))
        }
    }

    private fun updateSubjectDescription(newDescription: String, subjectData: SubjectData) {
        if (subjectData.description != newDescription) {
            submitSubject(subjectData.copy(description = newDescription))
        }
    }

    private fun submitSubject(subjectData: SubjectData) {
        when (subjectData.id) {
            null -> props.onSubjectsCreate(subjectData)
            else -> props.onSubjectUpdate(subjectData)
        }
    }

    interface State : RState {
        var filterText: String
    }

}