package com.infowings.catalog.common

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.max
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

data class PaginationData(val pageSize: Int, val current: Int, val totalItems: Int) {
    val offset: Int
        get() = min(pageSize * (current - 1), totalItems)

    val limit: Int = pageSize

    val totalPages: Int
        get() = totalPages(totalItems)

    // total pages is always > 0 even for empty items list we have 1 page
    private fun totalPages(items: Int) = max(1, ceil(items.toDouble() / pageSize).toInt())

    fun updateTotal(newTotal: Int): PaginationData {
        require(newTotal >= 0) { "$newTotal is less than 0 but total elements count should be greater than or equal 0" }
        val newCurrent = min(totalPages(newTotal), current)
        return copy(totalItems = newTotal, current = newCurrent)
    }

    companion object {
        // totalItems set to defaultPageSize to prevent all items loading from server
        // if it is set to 0 we cannot set correct limit for request
        val emptyPage = PaginationData(defaultPageSize, 1, totalItems = defaultPageSize)

        val allItems = PaginationData(defaultPageSize, 1, totalItems = Int.MAX_VALUE)
    }

}