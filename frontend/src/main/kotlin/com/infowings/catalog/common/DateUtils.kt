package com.infowings.catalog.common

import kotlin.js.Date

private fun Date.getDateWithPadding() = this.getDate().toString().padStart(2, '0')
private fun Date.getMonthWithPadding() = this.getMonth().inc().toString().padStart(2, '0')
private fun Date.getHoursWithPadding() = this.getHours().toString().padStart(2, '0')
private fun Date.getMinutesWithPadding() = this.getMinutes().toString().padStart(2, '0')
fun Date.tableFormat() =
    "${this.getDateWithPadding()}.${this.getMonthWithPadding()} at ${this.getHoursWithPadding()}:${this.getMinutesWithPadding()}"
