package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.objects.edit.SubjectTruncated
import com.infowings.catalog.subjects.getSuggestedSubject
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import react.RBuilder

private interface SubjectOption : SelectOption {
    var subjectName: String
    var subject: SubjectTruncated
}

private fun subjectOption(subjectData: SubjectData) = jsObject<SubjectOption> {
    this.subjectName = subjectData.name
    this.subject = SubjectTruncated(subjectData.id ?: error("Subject without id"), subjectData.name)
}

private fun subjectOption(subjectTruncated: SubjectTruncated) = jsObject<SubjectOption> {
    this.subjectName = subjectTruncated.name
    this.subject = subjectTruncated
}

fun RBuilder.objectSubject(value: SubjectTruncated?, onSelect: (SubjectTruncated) -> Unit, disabled: Boolean) =
    asyncSelect<SubjectOption> {
        attrs {
            className = "object-input-subject"
            placeholder = "Select Subject"
            this.value = value?.let { subjectOption(it) }
            onChange = { onSelect(it.subject) }
            labelKey = "subjectName"
            valueKey = "subjectName"
            cache = false
            clearable = false
            options = emptyArray()
            loadOptions = { input, callback ->
                if (input.isNotEmpty()) {
                    GlobalScope.launch {
                        val subjectsList: SubjectsList? = withTimeoutOrNull(700) {
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
                        options = emptyArray()
                    })
                }
                false
            }
            this.disabled = disabled
        }
    }