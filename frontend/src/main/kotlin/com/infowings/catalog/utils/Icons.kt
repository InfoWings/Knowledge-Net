package com.infowings.catalog.utils

import com.infowings.catalog.wrappers.react.path
import com.infowings.catalog.wrappers.react.svg
import kotlinx.html.SVG
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.title

fun RBuilder.squarePlusIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("squared-plus")
        path("M16 2h-12c-1.1 0-2 0.9-2 2v12c0 1.1 0.9 2 2 2h12c1.1 0 2-0.9 2-2v-12c0-1.1-0.9-2-2-2zM15 11h-4v4h-2v-4h-4v-2h4v-4h2v4h4v2z")
        block()
    }
}

fun RBuilder.squareMinusIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("squared-minus")
        path("M16 2h-12c-1.1 0-2 0.9-2 2v12c0 1.1 0.9 2 2 2h12c1.1 0 2-0.9 2-2v-12c0-1.1-0.9-2-2-2zM15 11h-10v-2h10v2z")
        block()
    }
}

fun RBuilder.addToListIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("add-to-list")
        path("M19.4 9h-3.4v-3.4c0-0.6-0.4-0.6-1-0.6s-1 0-1 0.6v3.4h-3.4c-0.6 0-0.6 0.4-0.6 1s0 1 0.6 1h3.4v3.4c0 0.6 0.4 0.6 1 0.6s1 0 1-0.6v-3.4h3.4c0.6 0 0.6-0.4 0.6-1s0-1-0.6-1zM7.4 9h-6.8c-0.6 0-0.6 0.4-0.6 1s0 1 0.6 1h6.8c0.6 0 0.6-0.4 0.6-1s0-1-0.6-1zM7.4 14h-6.8c-0.6 0-0.6 0.4-0.6 1s0 1 0.6 1h6.8c0.6 0 0.6-0.4 0.6-1s0-1-0.6-1zM7.4 4h-6.8c-0.6 0-0.6 0.4-0.6 1s0 1 0.6 1h6.8c0.6 0 0.6-0.4 0.6-1s0-1-0.6-1z")
        block()
    }
}

fun RBuilder.checkIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("check")
        path("M8.294 16.998c-0.435 0-0.847-0.203-1.111-0.553l-3.573-4.721c-0.465-0.613-0.344-1.486 0.27-1.951 0.615-0.467 1.488-0.344 1.953 0.27l2.351 3.104 5.911-9.492c0.407-0.652 1.267-0.852 1.921-0.445s0.854 1.266 0.446 1.92l-6.984 11.21c-0.242 0.391-0.661 0.635-1.12 0.656-0.022 0.002-0.042 0.002-0.064 0.002z")
        block()
    }
}

fun RBuilder.crossIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes) {
        title("cross")
        path("M14.348 14.849c-0.469 0.469-1.229 0.469-1.697 0l-2.651-3.030-2.651 3.029c-0.469 0.469-1.229 0.469-1.697 0-0.469-0.469-0.469-1.229 0-1.697l2.758-3.15-2.759-3.152c-0.469-0.469-0.469-1.228 0-1.697s1.228-0.469 1.697 0l2.652 3.031 2.651-3.031c0.469-0.469 1.228-0.469 1.697 0s0.469 1.229 0 1.697l-2.758 3.152 2.758 3.15c0.469 0.469 0.469 1.229 0 1.698z")
        block()
    }
}

fun RBuilder.ripIcon(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit) {
    svg(classes, "0 0 512 512") {
        title("rip")
        path(
            """M473.043,411.826h-33.391V183.651C439.652,82.224,357.428,0,256.001,0H256C154.572,0,72.348,82.224,72.348,183.651
			v228.175H38.957c-18.441,0-33.391,14.95-33.391,33.391v50.087c0,9.22,7.475,16.696,16.696,16.696h467.478
			c9.22,0,16.696-7.475,16.696-16.696v-50.087C506.435,426.776,491.484,411.826,473.043,411.826z M105.739,183.652
			c0-83.152,67.886-150.737,151.141-150.259c82.914,0.477,149.38,68.996,149.38,151.911v226.521H105.739V183.652z M473.043,478.609
			H38.957v-33.391h434.087V478.609z""".trimIndent()
        )
        path(
            """M322.783,166.957h-50.087V116.87c0-9.22-7.475-16.696-16.696-16.696c-9.22,0-16.696,7.475-16.696,16.696v50.087h-50.087
			c-9.22,0-16.696,7.475-16.696,16.696c0,9.22,7.475,16.696,16.696,16.696h50.087v83.478c0,9.22,7.475,16.696,16.696,16.696
			c9.22,0,16.696-7.475,16.696-16.696v-83.478h50.087c9.22,0,16.696-7.475,16.696-16.696
			C339.478,174.432,332.003,166.957,322.783,166.957z""".trimIndent()
        )
        block()
    }
}
