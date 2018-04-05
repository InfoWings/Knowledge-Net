package com.infowings.catalog.components.searchbar

import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.html.js.onChangeFunction
import react.*
import react.dom.div
import react.dom.input


class SearchBarProperties(var filterText: String, var onFilterTextChange: (filterText: String) -> Unit) : RProps

class SearchBar(props: SearchBarProperties) : RComponent<SearchBarProperties, RState>(props) {

    companion object {
        init {
            require("styles/aspect-edit-console.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "search-bar") {
            div(classes = "aspect-edit-console--aspect-input-container") {
                div(classes = "aspect-edit-console--input-wrapper") {
                    input(classes = "aspect-edit-console--input") {
                        this.attrs {
                            type = kotlinx.html.InputType.text
                            placeholder = "Search ..."
                            value = props.filterText
                            onChangeFunction = { props.onFilterTextChange(it.target.asDynamic().value as String) }
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.searchBar(block: RHandler<SearchBarProperties>) {
    child(SearchBar::class, block)
}