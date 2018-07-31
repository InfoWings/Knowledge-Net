package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectPropertyDoesNotExist
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORecordId
import java.math.BigDecimal

/* По опыту предыдущих сущностей, концепция валидатора модифицирована:
 * по итогам валидации создаем Objekt, готовый к записи в базу
 *
 * Без этого получается дупликация вызовов. Например, придется сначала вызвать
 * subjectService.findById внутри валидации, чтобы проверить корректность subjectId
 * а потом - снова его же вызывать в момент сохранения, чтобы узнать, к какому субъекту
 * привязываться.
 * И придется снова что-то делать с тем фактом, что findById возвращает nullable
 * Придется или игнорировать возможность null, полагаясь на уже проведенную валидацию
 * (вынесенную в валидатор), либо фактически повторять уже сделанную проверку
 */
class ObjectValidator(
    private val objectService: ObjectService,
    private val subjectService: SubjectService,
    private val measureService: MeasureService,
    private val refBookService: ReferenceBookService,
    private val objectDaoService: ObjectDaoService,
    private val aspectDao: AspectDaoService
) {
    fun checkedForCreation(request: ObjectCreateRequest): ObjectWriteInfo {
        val subjectVertex = subjectService.findByIdStrict(request.subjectId)

        val trimmedName: String = request.name.trim()
        if (trimmedName.isEmpty()) {
            throw EmptyObjectNameException(request)
        }

        objectDaoService.getObjectVertexesByNameAndSubject(request.name, subjectVertex.identity).let {
            if (it.isNotEmpty()) {
                throw ObjectAlreadyExists(request.name)
            }
        }

        //check business key
        if (objectDaoService.getObjectVertexesByNameAndSubject(request.name, ORecordId(request.subjectId)).isNotEmpty()) {
            throw ObjectAlreadyExists(request.name)
        }

        return ObjectWriteInfo(
            name = request.name,
            description = request.description,
            subject = subjectVertex
        )
    }

    fun checkedForUpdating(objectVertex: ObjectVertex, request: ObjectUpdateRequest): ObjectWriteInfo {
        val currentSubjectVertex = objectVertex.subject
        val currentSubjectId = currentSubjectVertex?.identity?.toString() ?: throw ObjectWithoutSubjectException(objectVertex.id)

        val newSubjectVertex = if (currentSubjectId == request.subjectId) currentSubjectVertex else subjectService.findByIdStrict(request.subjectId)
        val newSubjectId = newSubjectVertex.identity

        //check business key
        val existsAnotherObjectSameName = objectDaoService.getObjectVertexesByNameAndSubject(request.name, newSubjectId).any {
            it.id != request.id
        }
        if (existsAnotherObjectSameName) {
            throw ObjectAlreadyExists(request.name)
        }
        return ObjectWriteInfo(request.name, request.description, newSubjectVertex)
    }

    fun checkedForCreation(request: PropertyCreateRequest): PropertyWriteInfo {
        val objectVertex = objectService.findById(request.objectId)
        val aspectVertex = aspectDao.find(request.aspectId) ?: throw AspectDoesNotExist(request.aspectId)

        val trimmedName = request.name?.trim()

        val sameAspectProps = objectService.findPropertyByObjectAndAspect(objectVertex.id, aspectVertex.id).map { it.name }
        if (sameAspectProps.contains(trimmedName)) {
            throw ObjectPropertyAlreadyExistException(trimmedName, objectVertex.id, aspectVertex.id)
        }

        //check business key
        if (objectDaoService.getPropertyVertexesByNameAndAspect(request.name, objectVertex.identity, ORecordId(request.aspectId)).isNotEmpty()) {
            throw ObjectPropertyAlreadyExistException(request.name, objectVertex.id, request.aspectId)
        }

        return PropertyWriteInfo(
            request.name,
            objectVertex,
            aspectVertex
        )
    }

    fun checkForUpdating(property: ObjectPropertyVertex, request: PropertyUpdateRequest): PropertyWriteInfo {
        val objectVertex = property.objekt ?: throw IllegalStateException("Object property must be linked with object")
        val aspectVertex = property.aspect ?: throw IllegalStateException("Object property must be linked with aspect")
        val aspectId = aspectVertex.identity

        // check business key
        val existsAnotherPropertySameName = objectDaoService.getPropertyVertexesByNameAndAspect(request.name, objectVertex.identity, aspectId).any {
            it.id != property.id
        }
        if (existsAnotherPropertySameName) {
            throw ObjectPropertyAlreadyExistException(request.name, objectVertex.id, aspectId.toString())
        }

        return PropertyWriteInfo(request.name, objectVertex, aspectVertex)
    }

    fun checkedForCreation(request: ValueCreateRequest): ValueWriteInfo {
        logger.info("checking value for creation: $request")
        val objectPropertyVertex = objectService.findPropertyById(request.objectPropertyId)

        if (request.parentValueId != null && request.aspectPropertyId == null) {
            throw IllegalArgumentException("No aspect property id for non-root value")
        }

        if (request.parentValueId == null && request.aspectPropertyId != null) {
            throw IllegalArgumentException("There is aspect property ${request.aspectPropertyId} for root value creation")
        }

        val aspectPropertyVertex = request.aspectPropertyId?.let {
            aspectDao.findProperty(it)
                    ?: throw AspectPropertyDoesNotExist(it)
        }

        val parentValueVertex = request.parentValueId?.let { objectService.findPropertyValueById(it) }

        val btStr = if (request.parentValueId != null)
            aspectPropertyVertex?.let { aspectDao.baseType(it) }
        else objectPropertyVertex.let { objectDaoService.baseType(it) }

        val baseType = btStr?.let { BaseType.restoreBaseType(it) }
        if (!request.value.assignableTo(baseType!!)) {
            throw IllegalArgumentException("Value ${request.value} is not compatible with type $baseType")
        }

        val dataValue = request.value

        val value = getObjectValueFromData(dataValue)

        // check business key
        val similarValues = objectDaoService.getValuesByObjectPropertyAndValue(objectPropertyVertex.identity, value).filter {
            it.aspectProperty?.id == request.aspectPropertyId
        }
        if (similarValues.isNotEmpty()) {
            throw ObjectPropertyValueAlreadyExists(value.toObjectValueData())
        }

        val measureVertex = request.measureId?.let { measureService.findById(it) }

        return ValueWriteInfo(value, objectPropertyVertex, aspectPropertyVertex, parentValueVertex, measureVertex)
    }

    fun checkedForUpdating(valueVertex: ObjectPropertyValueVertex, request: ValueUpdateRequest): ValueWriteInfo {
        val objPropertyVertex = valueVertex.objectProperty
                ?: throw IllegalStateException("ObjectPropertyValue ${valueVertex.id} has no linked ObjectProperty")

        val objectPropertyVertex = objectService.findPropertyById(objPropertyVertex.id)
        val aspectPropertyVertex = valueVertex.aspectProperty

        if (aspectPropertyVertex == null && valueVertex.parentValue != null) {
            throw IllegalStateException("ObjectPropertyValue ${valueVertex.id} has no linked Aspect")
        }

        if (aspectPropertyVertex != null && valueVertex.parentValue == null) {
            throw IllegalArgumentException("There is aspect property ${aspectPropertyVertex.id} for root value")
        }

        val btStr = if (valueVertex.parentValue != null)
            aspectPropertyVertex?.let { aspectDao.baseType(it) }
        else objectPropertyVertex.let { objectDaoService.baseType(it) }

        val baseType = btStr?.let { BaseType.restoreBaseType(it) }
        if (!request.value.assignableTo(baseType!!)) {
            throw IllegalArgumentException("Value ${request.value} is not compatible with type $baseType")
        }

        val parentValueVertex = valueVertex.parentValue

        val dataValue = request.value

        val value = getObjectValueFromData(dataValue)
        // check business key
        val existsSameValue = objectDaoService.getValuesByObjectPropertyAndValue(objPropertyVertex.identity, value).any {
            it.id != valueVertex.id
        }
        if (existsSameValue) {
            throw ObjectPropertyValueAlreadyExists(value.toObjectValueData())
        }

        return ValueWriteInfo(
            value,
            objectPropertyVertex,
            aspectPropertyVertex,
            parentValueVertex,
            valueVertex.measure
        )
    }

    private fun getObjectValueFromData(dataValue: ObjectValueData): ObjectValue = when (dataValue) {
        is ObjectValueData.Link -> {
            val refValueVertex = dataValue.value.let {
                when (it) {
                    is LinkValueData.Subject ->
                        LinkValueVertex.Subject(subjectService.findByIdStrict(it.id))
                    is LinkValueData.Object ->
                        LinkValueVertex.Object(objectService.findById(it.id))
                    is LinkValueData.ObjectProperty ->
                        LinkValueVertex.ObjectProperty(objectService.findPropertyById(it.id))
                    is LinkValueData.ObjectValue ->
                        LinkValueVertex.ObjectValue(objectService.findPropertyValueById(it.id))
                    is LinkValueData.DomainElement ->
                        LinkValueVertex.DomainElement(refBookService.getReferenceBookItemVertex(it.id))
                    is LinkValueData.RefBookItem ->
                        LinkValueVertex.RefBookItem(refBookService.getReferenceBookItemVertex(it.id))
                    is LinkValueData.Aspect ->
                        LinkValueVertex.Aspect(aspectDao.findStrict(it.id))
                    is LinkValueData.AspectProperty ->
                        LinkValueVertex.AspectProperty(aspectDao.findPropertyStrict(it.id))
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
}

private val logger = loggerFor<ObjectValidator>()