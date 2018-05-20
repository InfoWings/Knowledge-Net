package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectPropertyDoesNotExist
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.orientechnologies.orient.core.id.ORecordId

/* По опыту предыдущих сущностей, концепция валидатора модифицирована:
 * по итогам валидации создаем Objekt, готовый к записи в базу
 *
 * Без этого получается дупликация вызовов. Например, придется сначала вызвать
 * subjectService.findById внутри валидации, чтобы проверить корректность subjectId
 * а потом - снова его же вызывать в момент сохранения, чтобы узнать, к какому субъекту
 * привязываться.
 * И придется снова что-то делать с тем фактом, что findById возвращает nullabale
 * Придется или игнорировать возможность null, полагаясь на уже проведенную валидацию
 * (вынесенную в валидатор), либо фактически повторять уже сделанную проверку
 */
class ObjectValidator(
    private val objectService: ObjectService,
    private val subjectService: SubjectService,
    private val measureService: MeasureService,
    private val refBookService: ReferenceBookService,
    private val aspectDao: AspectDaoService
) {
    fun checkedForCreation(data: ObjectData): Objekt {
        val subjectVertex = subjectService.findById(data.subjectId)

        data.id?.let {
            throw IllegalStateException("id must be null for creation: $data")
        }

        if (data.propertyIds.isNotEmpty()) {
            throw IllegalStateException("there should be no properties for object creation: $data")
        }

        val trimmedName = data.name.trim()
        if (trimmedName.isEmpty()) {
            throw EmptyObjectNameException(data)
        }

        return Objekt(
            data.id?.let { ORecordId(it) },
            trimmedName,
            data.description,
            subjectVertex,
            emptyList()
        )
    }

    fun checkedForCreation(data: ObjectPropertyData): ObjectProperty {
        val objectVertex = objectService.findById(data.objectId)
        val aspectVertex = aspectDao.getAspectVertex(data.aspectId) ?: throw AspectDoesNotExist(data.aspectId)

        data.id?.let { throw IllegalStateException("id must be null for creation: $data") }

        if (data.valueIds.isNotEmpty()) {
            throw IllegalStateException("there should be no values for object creation: $data")
        }

        val trimmedName = data.name.trim()
        if (trimmedName.isEmpty()) {
            throw EmptyObjectPropertyNameException(data)
        }

        return ObjectProperty(
            data.id?.let { ORecordId(it) },
            trimmedName,
            data.cardinality,
            objectVertex,
            aspectVertex,
            emptyList()
        )
    }

    fun checkedForCreation(data: ObjectPropertyValueData): ObjectPropertyValue {
        val objectPropertyVertex = objectService.findPropertyById(data.objectPropertyId)

        data.id?.let { throw IllegalStateException("id must be null for creation: $data") }

        val aspectVertex = aspectDao.getAspectPropertyVertex(data.aspectPropertyId)
                ?: throw AspectPropertyDoesNotExist(data.aspectPropertyId)
        val parentValueVertex = data.parentValueId?.let { objectService.findPropertyValueById(it) }

        val dataValue = data.value

        val value = when (dataValue) {
            is ObjectValueData.Link -> {
                val refValueVertex = dataValue.value.let {
                    when (it.typeGroup) {
                        LinkTypeGroup.SUBJECT ->
                            LinkValueVertex.SubjectValue(subjectService.findById(it.id))
                        LinkTypeGroup.OBJECT ->
                            LinkValueVertex.ObjectValue(objectService.findById(it.id))
                        LinkTypeGroup.DOMAIN_ELEMENT ->
                            LinkValueVertex.DomainElementValue(refBookService.getReferenceBookItemVertex(it.id))
                    }
                }
                ObjectValue.Link(refValueVertex)
            }
            else ->
                fromData(dataValue)
        }

        val measureVertex = data.measureId?.let { measureService.findById(it) }

        return ObjectPropertyValue(
            data.id?.let { ORecordId(it) },
            value,
            objectPropertyVertex,
            aspectVertex,
            parentValueVertex,
            measureVertex
        )
    }

    fun checkBusinessKey(property: ObjectProperty) {
    }
}