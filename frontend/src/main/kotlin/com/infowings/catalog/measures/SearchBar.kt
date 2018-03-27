package com.infowings.catalog.measures

import kotlinext.js.invoke
import kotlinx.html.js.onChangeFunction
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div
import react.dom.input


class SearchBarProperties(var filterText: String, var onFilterTextChange: (filterText: String) -> Unit) : RProps

class SearchBar(props: SearchBarProperties) : RComponent<SearchBarProperties, RState>(props) {

    companion object {
        init {
            kotlinext.js.require("styles/aspect-edit-console.scss") // Styles regarding aspect console
        }
    }

    override fun RBuilder.render() {
        div(classes = "measure-search-bar") {
            div(classes = "aspect-edit-console--aspect-input-container") {
                div(classes = "aspect-edit-console--input-wrapper") {
                    input(classes = "aspect-edit-console--input") {
                        this.attrs {
                            type = kotlinx.html.InputType.text
                            placeholder = "Search ..."
                            value = props.filterText
                            onChangeFunction = { props.onFilterTextChange(it.target.asDynamic().value) }
                        }
                    }
                }
            }
        }
    }
}
