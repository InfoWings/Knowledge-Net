package com.infowings.catalog.data

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
        if (value == null)
            return true

        if (baseType.clazz != null) {
            return value::class == baseType.clazz
        }

        return false

}

//todo: пока для простоты
fun restoreAspectDomain(name: String, baseType: BaseType): AspectDomain = OpenDomain(baseType)