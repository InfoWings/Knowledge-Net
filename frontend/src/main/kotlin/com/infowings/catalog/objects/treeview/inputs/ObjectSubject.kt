package com.infowings.catalog.objects.treeview.inputs

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.subjects.getSuggestedSubject
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import react.RBuilder

private interface SubjectOption : SelectOption {
    var subjectName: String
    var subjectData: SubjectData
}

private fun subjectOption(subjectData: SubjectData) = jsObject<SubjectOption> {
    this.subjectName = subjectData.name
    this.subjectData = subjectData
}

fun RBuilder.objectSubject(value: SubjectData?, onSelect: (SubjectData) -> Unit, onOpen: () -> Unit, disabled: Boolean = false) =
    asyncSelect<SubjectOption> {
        attrs {
            className = "object-input-subject"
            placeholder = "Select Subject"
            this.value = value?.name ?: ""
            onChange = { onSelect(it.subjectData) }
            labelKey = "subjectName"
            valueKey = "subjectName"
            cache = false
            clearable = false
            options = value?.let { arrayOf(subjectOption(it)) } ?: emptyArray()
            this.onOpen = onOpen
            this.disabled = disabled
            loadOptions = { input, callback ->
                if (input.isNotEmpty()) {
                    launch {
                        val subjectsList: SubjectsList? = withTimeoutOrNull(500) {
                            getSuggestedSubject(input, "")
                        }
                        callback(null, jsObject {
                            options = subjectsList?.subject?.map {
                                subjectOption(it)
                            }?.toTypedArray() ?: emptyArray()
                        })
                    }
                } else {
                    callback(null, jsObject {
                        options = value?.let { arrayOf(subjectOption(it)) } ?: emptyArray()
                    })
                }
                false
            }
        }
    }