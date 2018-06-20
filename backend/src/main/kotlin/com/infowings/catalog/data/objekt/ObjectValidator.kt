package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectPropertyDoesNotExist
import com.infowings.catalog.data.reference.book.ReferenceBookService
import java.math.BigDecimal

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
    fun checkedForCreation(request: ObjectCreateRequest): ObjectCreateInfo {
        val subjectVertex = subjectService.findByIdStrict(request.subjectId)

        val trimmedName: String = request.name.trim()
        if (trimmedName.isEmpty()) {
            throw EmptyObjectNameException(request)
        }

        return ObjectCreateInfo(
            name = trimmedName,
            description = request.description,
            subject = subjectVertex
        )
    }

    fun checkedForCreation(request: PropertyCreateRequest): PropertyWriteInfo {
        val objectVertex = objectService.findById(request.objectId)
        val aspectVertex = aspectDao.getAspectVertex(request.aspectId) ?: throw AspectDoesNotExist(request.aspectId)

        return PropertyWriteInfo(
            request.name,
            PropertyCardinality.valueOf(request.cardinality),
            objectVertex,
            aspectVertex
        )
    }

    fun checkedForCreation(request: ValueCreateRequest): ValueWriteInfo {
        val objectPropertyVertex = objectService.findPropertyById(request.objectPropertyId)

        val aspectPropertyVertex = request.aspectPropertyId?.let {
            aspectDao.getAspectPropertyVertex(it)
                    ?: throw AspectPropertyDoesNotExist(it)
        }

        val parentValueVertex = request.parentValueId?.let { objectService.findPropertyValueById(it) }


        val dataValue = request.value

        val value = when (dataValue) {
            is ObjectValueData.Link -> {
                val refValueVertex = dataValue.value.let {
                    when (it) {
                        is LinkValueData.Subject ->
                            LinkValueVertex.SubjectValue(subjectService.findByIdStrict(it.id))
                        is LinkValueData.Object ->
                            LinkValueVertex.ObjectValue(objectService.findById(it.id))
                        is LinkValueData.DomainElement ->
                            LinkValueVertex.DomainElementValue(refBookService.getReferenceBookItemVertex(it.id))
                    }
                }
                ObjectValue.Link(refValueVertex)
            }
            is ObjectValueData.IntegerValue ->
                ObjectValue.IntegerValue(dataValue.value, dataValue.precision)
            is ObjectValueData.StringValue ->
                ObjectValue.StringValue(dataValue.value)
            is ObjectValueData.BooleanValue ->
                ObjectValue.BooleanValue(dataValue.value)
            is ObjectValueData.RangeValue ->
                ObjectValue.RangeValue(dataValue.range)
            is ObjectValueData.DecimalValue ->
                ObjectValue.DecimalValue(BigDecimal(dataValue.valueRepr))
            is ObjectValueData.NullValue ->
                ObjectValue.NullValue
        }

        val measureVertex = request.measureId?.let { measureService.findById(it) }

        return ValueWriteInfo(
            value,
            objectPropertyVertex,
            aspectPropertyVertex,
            parentValueVertex,
            measureVertex
        )
    }

    //fun checkBusinessKey(property: ObjectProperty) {
    //}
}