package com.infowings.catalog.units

import kotlinx.html.ButtonType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.button
import react.dom.input


class SearchBarProperties(var onFilterTextChange: (filterText: String) -> Unit) : RProps

class SearchBar(props: SearchBarProperties) : RComponent<SearchBarProperties, SearchBar.State>(props) {

    override fun RBuilder.render() {
        input(classes = "input") {
            attrs {
                type = kotlinx.html.InputType.text
                placeholder = "Search ..."
                onChangeFunction = {
                    value = it.target.asDynamic().value
                    updateFilterText(value)
                    if (value.isEmpty()) {
                        props.onFilterTextChange("")
                    }
                }
            }
        }
        button(classes = "button") {
            attrs {
                type = ButtonType.button
                onClickFunction = { props.onFilterTextChange(state.filterText) }
            }
            +"Search"
        }
    }

    private fun updateFilterText(filterText: String) {
        setState {
            this.filterText = filterText
        }
    }

    interface State : RState {
        var filterText: String
    }
}
