package com.infowings.catalog.components.searchbar

import kotlinext.js.require
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.div
import react.dom.input


class SearchBar : RComponent<SearchBar.Props, RState>() {

    companion object {
        init {
            require("styles/search-bar.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "search-bar${props.className?.let { " $it" } ?: ""}") {
            input(classes = "search-bar--input pt-input") {
                this.attrs {
                    type = InputType.text
                    placeholder = "Search ..."
                    value = props.filterText
                    onChangeFunction = { props.onFilterTextChange((it.target as HTMLInputElement).value) }
                }
            }
        }
    }

    interface Props : RProps {
        var className: String?
        var filterText: String
        var onFilterTextChange: (filterText: String) -> Unit
    }
}

fun RBuilder.searchBar(block: RHandler<SearchBar.Props>) {
    child(SearchBar::class, block)
}