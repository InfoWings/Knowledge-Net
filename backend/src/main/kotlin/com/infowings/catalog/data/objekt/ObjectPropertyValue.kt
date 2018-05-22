package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex
import java.math.BigDecimal


/* В структурах этого файла используются ссылки на vertex-объекты.
   Они нужны по следующим соображениям:

   1. Во избежание двойных поисков при валидации и сохранении новосозданного объекта.
   При создании нужно создать ребро. Для создания ребра нужно знать vertex.
   В то же время, при валидации мы ищем vertex, чтобы проверить корректность id
   Чтобы не делать двойных поисков, валидация возвращает структуру, в которой сохраняются
   найденные vertex-ы

   2. Структуры уровня бизнес-логики для объектов проблематично создать в форме системы наивно вложенных data-классов
   (здесь 'data' - в котлиновском смысле) - структура получится большая и ветвистая, как минимум, и, возможно,
    цикличная (потому что ObjectPropertyValue может пониматься как часть ObjectProperty, который есть часть Objekt,
    а, с другой стороные, ObjectProprtyValue может содержать сылку на объект.
    Ссылка на vertex является формой привнесения некоторой "ленивости" с структуру.

    С другой стороны, это не очень здорово - показывать в структуре уровня бизнес-логики ссылки на vertex-ы
    Поэтому хорошо бы сделать так, чтобы для целей, описанных в п. 1, использовалась какая-то своя внутрисервисная
    структура, не видная снаружи, а для бизнес логики был бы какой-то свой фасадный который бы закулисами работал
    бы с теми же самыми vertex-ами, но не показывал бы их своим клиентам
 */

/* Бекенд-структура для представления ссылочного значения.
 * Здесь id недостаточно, лучше иметь дело с vertex
 */
sealed class LinkValueVertex {
    abstract val vertex: OVertex
    abstract fun toData(): LinkValueData

    class SubjectValue(override val vertex: SubjectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Subject(vertex.id)
    }

    class ObjectValue(override val vertex: ObjectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Object(vertex.id)
    }

    class DomainElementValue(override val vertex: ReferenceBookItemVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.DomainElement(vertex.id)
    }
}

/* Это бекендный аналог структуры ObjectValueData

   Часть подтипов совсем такие же, как в ObjectValue, но Decimal и Link отличаются
 */
sealed class ObjectValue {
    data class IntegerValue(val value: Int, val precision: Int?) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.IntegerValue(value, precision)
    }

    data class StringValue(val value: String) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.StringValue(value)
    }

    data class RangeValue(val range: Range) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.RangeValue(range)
    }

    data class DecimalValue(val value: BigDecimal) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.DecimalValue(value.toString())
    }

    data class Link(val value: LinkValueVertex) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.Link(value.toData())
    }

    abstract fun toObjectValueData(): ObjectValueData
}

/**
 * Object property value data representation for use in backend context.
 * It can use Orient data structures but it is detached from database - updated to it does not lead to
 * updates in database
 */
data class ObjectPropertyValue(
    val id: ORID?,
    val value: ObjectValue,
    val objectProperty: ObjectPropertyVertex,
    val aspectProperty: AspectPropertyVertex,
    val parentValue: ObjectPropertyValueVertex?,
    val measure: OVertex?
) {
    fun toObjectPropertyValueData() = ObjectPropertyValueData(
        id?.toString(),
        value.toObjectValueData(),
        objectProperty.id,
        aspectProperty.id,
        parentValue?.id,
        measure?.id
    )
}
