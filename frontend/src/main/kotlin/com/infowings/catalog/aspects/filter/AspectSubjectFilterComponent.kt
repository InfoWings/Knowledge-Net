package com.infowings.catalog.aspects.filter

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import kotlinext.js.require
import react.*

private interface SubjectOption : SelectOption {
    var subjectName: String
    var subjectData: SubjectData?
}

private fun subjectOption(subjectData: SubjectData?) = jsObject<SubjectOption> {
    this.subjectName = subjectData?.name ?: "Global"
    this.subjectData = subjectData
}

class AspectSubjectFilterComponent : RComponent<AspectSubjectFilterComponent.Props, RState>() {

    companion object {
        init {
            require("styles/aspect-filter.scss")
        }
    }

    override fun RBuilder.render() {
        commonSelect<SubjectOption> {
            attrs {
                className = "aspect-subject-filter"
                multi = true
                placeholder = "Filter by subject..."
                value = props.subjectsFilter.map { it?.name ?: "Global" }.toTypedArray()
                labelKey = "subjectName"
                valueKey = "subjectName"
                onChange = {
                    props.onChange(it.unsafeCast<Array<SubjectOption>>().map { it.subjectData })
                }
                options = props.subjectsToFilter.map { subjectOption(it) }.toTypedArray()
                clearable = false
            }
        }
    }

    interface Props : RProps {
        var subjectsFilter: Collection<SubjectData?>
        var subjectsToFilter: List<SubjectData?>
        var onChange: (List<SubjectData?>) -> Unit
    }
}

fun RBuilder.aspectSubjectFilterComponent(block: RHandler<AspectSubjectFilterComponent.Props>) =
    child(AspectSubjectFilterComponent::class, block)