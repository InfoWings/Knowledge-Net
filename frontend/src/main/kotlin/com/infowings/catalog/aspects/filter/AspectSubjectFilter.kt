package com.infowings.catalog.aspects.filter

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import kotlinext.js.require
import react.*
import react.dom.div

private interface SubjectOption : SelectOption {
    var subjectName: String
    var subjectData: SubjectData?
}

private fun subjectOption(subjectData: SubjectData?) = jsObject<SubjectOption> {
    when (subjectData) {
        null -> {
            this.subjectName = "Global"
            this.subjectData = null
        }
        else -> {
            this.subjectName = subjectData.name
            this.subjectData = subjectData
        }
    }
}

class AspectSubjectFilter : RComponent<AspectSubjectFilter.Props, RState>() {

    companion object {
        init {
            require("styles/aspect-filter.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-subject-filter") {
            commonSelect<SubjectOption> {
                attrs {
                    className = "aspect-subject-filter__input"
                    multi = true
                    placeholder = "Filter by subject..."
                    value = props.subjectsFilter.map { it?.name ?: "Global" }.toTypedArray()
                    labelKey = "subjectName"
                    valueKey = "subjectName"
                    onChange = {
                        props.onChange(it.unsafeCast<Array<SubjectOption>>().map { it.subjectData }.toTypedArray())
                    }
                    options = props.subjectsToFilter.map { subjectOption(it) }.toTypedArray()
                    clearable = false
                }
            }
        }
    }

    interface Props : RProps {
        var subjectsFilter: Collection<SubjectData?>
        var subjectsToFilter: List<SubjectData?>
        var onChange: (Array<SubjectData?>) -> Unit
    }
}

fun RBuilder.aspectSubjectFilter(block: RHandler<AspectSubjectFilter.Props>) = child(AspectSubjectFilter::class, block)