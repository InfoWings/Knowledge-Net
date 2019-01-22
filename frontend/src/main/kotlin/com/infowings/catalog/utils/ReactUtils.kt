package com.infowings.catalog.utils

import react.Component
import react.RBuilder
import react.RProps


/**
 * Helper function to support simplified syntax:
 *
 * ```
 *  fun RBuilder.paginationPanel(builder: Pagination.Props.() -> Unit) = buildWithProperties<Pagination.Props, Pagination>(builder)
 * ```
 *
 *  and then
 *
 *  ```
 *   paginationPanel{
 *    pageSize = 10
 *   }
 *  ```
 */
inline fun <reified T : RProps, reified C : Component<T, *>> RBuilder.buildWithProperties(crossinline builder: T.() -> Unit) = child(C::class) {
    this.attrs { builder.invoke(this) }
}