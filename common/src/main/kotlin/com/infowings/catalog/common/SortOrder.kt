package com.infowings.catalog.common

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.min

enum class Direction(val dir: kotlin.Int) {
    ASC(1), DESC(-1)
}

enum class SortField {
    NAME, SUBJECT
}

@Serializable
data class SortOrder(val name: SortField, val direction: Direction) {
    companion object {
        fun listOf(orders: List<String>, directions: List<String>) = directions.zip(orders).map { (direction, order) ->
            SortOrder(SortField.valueOf(order), Direction.valueOf(direction))
        }
    }
}

class CompareString(val value: String, val direction: Direction) : Comparable<CompareString> {
    override fun compareTo(other: CompareString): Int =
        direction.dir * value.toLowerCase().compareTo(other.value.toLowerCase())
}

private const val defaultPageSize = 20

@Serializable
data class PaginationData(val pageSize: Int, val current: Int, val totalItems: Int) {
    val offset: Int
        get() = min(pageSize * (current - 1), totalItems)

    val limit: Int = pageSize

    val totalPages: Int
        get() = ceil(totalItems.toDouble() / pageSize).toInt()

    companion object {
        // totalItems set to defaultPageSize to prevent all items loading from server
        // if it is set to 0 we cannot set correct limit for request
        val emptyPage = PaginationData(defaultPageSize, 1, totalItems = defaultPageSize)

        val allItems = PaginationData(defaultPageSize, 1, totalItems = Int.MAX_VALUE)
    }

}