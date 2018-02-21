package com.infowings.catalog.data

import com.infowings.catalog.common.BaseType

/**
 * домен аспекта - допустимое множество значений (например может быть аспект длина с типом вещественное число и доменом значение > 0
 */
sealed class AspectDomain(val baseType: BaseType) {
    abstract fun contains(value: Any?): Boolean
}

/**
 * все возможные значения типа,  проверяем только соответствие типа
 * todo: не null-able типы и сложные типы
 */
class OpenDomain(baseType: BaseType) : AspectDomain(baseType) {
    override fun contains(value: Any?): Boolean {
        if (value == null || value !is BaseType)
            return true

        return BaseType.getTypeClass(baseType.name) == BaseType.getTypeClass(value.name)
    }

    override fun toString(): String {
        return "OpenDomain of $baseType"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenDomain

        return this.baseType == other.baseType
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

//todo: пока для простоты
fun restoreAspectDomain(name: String, baseType: BaseType): AspectDomain = OpenDomain(baseType)