package com.infowings.catalog.common

class VersionSkewException(expected: Int, real: Int) : Throwable("Expected to be at least $expected. Real: $real")

interface VersionAware {
    val version: Int

    fun isModified(otherVersion: Int): Boolean {
        if (version > otherVersion) {
            throw VersionSkewException(expected = version, real = otherVersion)
        }

        return version < otherVersion
    }
}