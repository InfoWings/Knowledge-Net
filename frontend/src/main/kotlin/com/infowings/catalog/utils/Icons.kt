package com.infowings.catalog.utils

import com.infowings.catalog.wrappers.react.path
import com.infowings.catalog.wrappers.react.svg
import kotlinx.html.SVG
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.title

fun RBuilder.squarePlusIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("Expand")
        path("M16 2h-12c-1.1 0-2 0.9-2 2v12c0 1.1 0.9 2 2 2h12c1.1 0 2-0.9 2-2v-12c0-1.1-0.9-2-2-2zM15 11h-4v4h-2v-4h-4v-2h4v-4h2v4h4v2z")
        block()
    }
}

fun RBuilder.squareMinusIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("Collapse")
        path("M16 2h-12c-1.1 0-2 0.9-2 2v12c0 1.1 0.9 2 2 2h12c1.1 0 2-0.9 2-2v-12c0-1.1-0.9-2-2-2zM15 11h-10v-2h10v2z")
        block()
    }
}

fun RBuilder.addToListIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("Add Property")
        path("M19.4 9h-3.4v-3.4c0-0.6-0.4-0.6-1-0.6s-1 0-1 0.6v3.4h-3.4c-0.6 0-0.6 0.4-0.6 1s0 1 0.6 1h3.4v3.4c0 0.6 0.4 0.6 1 0.6s1 0 1-0.6v-3.4h3.4c0.6 0 0.6-0.4 0.6-1s0-1-0.6-1zM7.4 9h-6.8c-0.6 0-0.6 0.4-0.6 1s0 1 0.6 1h6.8c0.6 0 0.6-0.4 0.6-1s0-1-0.6-1zM7.4 14h-6.8c-0.6 0-0.6 0.4-0.6 1s0 1 0.6 1h6.8c0.6 0 0.6-0.4 0.6-1s0-1-0.6-1zM7.4 4h-6.8c-0.6 0-0.6 0.4-0.6 1s0 1 0.6 1h6.8c0.6 0 0.6-0.4 0.6-1s0-1-0.6-1z")
        block()
    }
}

fun RBuilder.checkIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("Save Aspect")
        path("M8.294 16.998c-0.435 0-0.847-0.203-1.111-0.553l-3.573-4.721c-0.465-0.613-0.344-1.486 0.27-1.951 0.615-0.467 1.488-0.344 1.953 0.27l2.351 3.104 5.911-9.492c0.407-0.652 1.267-0.852 1.921-0.445s0.854 1.266 0.446 1.92l-6.984 11.21c-0.242 0.391-0.661 0.635-1.12 0.656-0.022 0.002-0.042 0.002-0.064 0.002z")
        block()
    }
}

fun RBuilder.crossIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("Discard changes")
        path("M14.348 14.849c-0.469 0.469-1.229 0.469-1.697 0l-2.651-3.030-2.651 3.029c-0.469 0.469-1.229 0.469-1.697 0-0.469-0.469-0.469-1.229 0-1.697l2.758-3.15-2.759-3.152c-0.469-0.469-0.469-1.228 0-1.697s1.228-0.469 1.697 0l2.652 3.031 2.651-3.031c0.469-0.469 1.228-0.469 1.697 0s0.469 1.229 0 1.697l-2.758 3.152 2.758 3.15c0.469 0.469 0.469 1.229 0 1.698z")
        block()
    }
}

fun RBuilder.chevronDownIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("Expand All")
        path("M4.516 7.548c0.436-0.446 1.043-0.481 1.576 0l3.908 3.747 3.908-3.747c0.533-0.481 1.141-0.446 1.574 0 0.436 0.445 0.408 1.197 0 1.615-0.406 0.418-4.695 4.502-4.695 4.502-0.217 0.223-0.502 0.335-0.787 0.335s-0.57-0.112-0.789-0.335c0 0-4.287-4.084-4.695-4.502s-0.436-1.17 0-1.615z")
        block()
    }
}