package com.infowings.catalog.common

interface GuidAware {
    val guid: String?
    fun guidSoft(): String = guid ?: "???"
}