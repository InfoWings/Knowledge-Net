
package com.infowings.catalog.data

import com.infowings.catalog.common.GuidAware
import com.infowings.catalog.common.SubjectData

data class Subject(
    val id: String,
    val name: String,
    val version: Int,
    val description: String?,
    val deleted: Boolean,
    override val guid: String?
) : GuidAware

fun Subject.toSubjectData() = SubjectData(
    id = this.id,
    name = this.name,
    version = this.version,
    description = this.description,
    deleted = this.deleted,
    guid = this.guid
)
