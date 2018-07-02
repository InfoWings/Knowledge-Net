package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.subjects.getSuggestedSubject
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div

private interface SubjectOption : SelectOption {
    var subjectName: String
    var subjectData: SubjectData
}

private fun subjectOption(subjectData: SubjectData) = jsObject<SubjectOption> {
    this.subjectName = subjectData.name
    this.subjectData = subjectData
}

class AspectSubjectInput : RComponent<AspectSubjectInput.Props, RState>() {

    private fun handleSubjectSelected(option: SubjectOption?) {
        option?.let { props.onChange(it.subjectData) } ?: props.onChange(null)
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
                        value = props.value?.name ?: ""
                        labelKey = "subjectName"
                        valueKey = "subjectName"
                        onChange = ::handleSubjectSelected
                        cache = false
                        onSelectResetsInput = false
                        clearable = true
                        resetValue = null
                        options = props.value?.let { arrayOf(subjectOption(it)) } ?: emptyArray()
                        autoBlur = true
                        loadOptions = { input, callback ->
                            if (input.isNotEmpty()) {
                                launch {
                                    val subjectsList: SubjectsList? = getSuggestedSubject(input, "")
                                    callback(null, jsObject {
                                        options = subjectsList?.subject?.map {
                                            subjectOption(it)
                                        }?.toTypedArray() ?: emptyArray()
                                    })
                                }
                            } else {
                                callback(null, jsObject {
                                    options = props.value?.let { arrayOf(subjectOption(it)) } ?: emptyArray()
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
        var value: SubjectData?
        var onChange: (SubjectData?) -> Unit
    }

}

fun RBuilder.aspectSubjectInput(block: RHandler<AspectSubjectInput.Props>) = child(AspectSubjectInput::class, block)