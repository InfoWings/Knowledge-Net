package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.subjects.getSuggestedSubject
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import react.*
import react.dom.div

private interface SubjectOption : SelectOption {
    var subject: String
    var subjectId: String
}

private fun subjectOption(optionName: String, optionId: String?) = jsObject<SubjectOption> {
    subject = optionName
    subjectId = optionId ?: ""
}

class AspectSubjectInput : RComponent<AspectSubjectInput.Props, RState>() {

    private fun handleSubjectSelected(option: SubjectOption) {
        props.onChange(option.subject, option.subjectId)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--aspect-input-container") {
            label(classes = "aspect-edit-console--input-label") {
                +"Subject"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                asyncSelect<SubjectOption> {
                    attrs {
                        className = "aspect-table-select"
                        value = props.value ?: ""
                        labelKey = "subject"
                        valueKey = "subject"
                        onChange = ::handleSubjectSelected
                        cache = false
                        onSelectResetsInput = false
                        clearable = true
                        resetValue = ""
                        options = if (props.value.isNullOrEmpty()) emptyArray()
                        else arrayOf(subjectOption(props.value!!, props.subjectId))
                        autoBlur = true
                        loadOptions = { input, callback ->
                            if (input.isNotEmpty()) {
                                launch {
                                    val subjectsList: SubjectsList? = withTimeoutOrNull(500) {
                                        getSuggestedSubject(input, "")
                                    }
                                    callback(null, jsObject {
                                        options = subjectsList?.subject?.map {
                                            subjectOption(
                                                it.name,
                                                it.id
                                            )
                                        }?.toTypedArray() ?: emptyArray()
                                    })
                                }
                            } else {
                                callback(null, jsObject {
                                    options = if (props.value.isNullOrEmpty()) emptyArray()
                                    else arrayOf(subjectOption(props.value!!, props.subjectId))
                                })
                            }
                            false // Hack to not return Unit from the function that is considered true if placed in `if (Unit)` in javascript
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var value: String?
        var subjectId: String?
        var onChange: (name: String, id: String) -> Unit
    }

}

fun RBuilder.aspectSubjectInput(block: RHandler<AspectSubjectInput.Props>) = child(AspectSubjectInput::class, block)