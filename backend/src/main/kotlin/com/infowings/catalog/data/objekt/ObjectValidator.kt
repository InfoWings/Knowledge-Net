package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectPropertyDoesNotExist
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.toMeasure
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
interface ObjectValidator {
    fun checkedForCreation(request: ObjectCreateRequest): ObjectWriteInfo
    fun checkedForUpdating(objectVertex: ObjectVertex, request: ObjectUpdateRequest): ObjectWriteInfo
    fun checkedForCreation(request: PropertyCreateRequest): PropertyWriteInfo
    fun checkedForUpdating(propertyVertex: ObjectPropertyVertex, request: PropertyUpdateRequest): PropertyWriteInfo
    fun checkedForCreation(request: ValueCreateRequest): ValueWriteInfo
    fun checkedForUpdating(valueVertex: ObjectPropertyValueVertex, request: ValueUpdateRequest): ValueWriteInfo
}

class TrimmingObjectValidator(private val objectValidator: ObjectValidator) : ObjectValidator by objectValidator {

    override fun checkedForCreation(request: ObjectCreateRequest): ObjectWriteInfo {
        return objectValidator.checkedForCreation(
            request.copy(name = if (request.name.isBlank()) throw EmptyObjectCreateNameException(request) else request.name.trim())
        )
    }

    override fun checkedForUpdating(objectVertex: ObjectVertex, request: ObjectUpdateRequest): ObjectWriteInfo {
        return objectValidator.checkedForUpdating(
            objectVertex,
            request.copy(name = if (request.name.isBlank()) throw EmptyObjectUpdateNameException(request) else request.name.trim())
        )
    }

    override fun checkedForCreation(request: PropertyCreateRequest): PropertyWriteInfo {
        return objectValidator.checkedForCreation(
            request.copy(name = if (request.name == null || request.name.isBlank()) null else request.name.trim())
        )
    }

    override fun checkedForUpdating(propertyVertex: ObjectPropertyVertex, request: PropertyUpdateRequest): PropertyWriteInfo {
        return objectValidator.checkedForUpdating(
            propertyVertex,
            request.copy(name = if (request.name == null || request.name.isBlank()) null else request.name.trim())
        )
    }

}

class MainObjectValidator(
    private val objectService: ObjectService,
    private val subjectService: SubjectService,
    private val measureService: MeasureService,
    private val refBookService: ReferenceBookService,
    private val objectDaoService: ObjectDaoService,
    private val aspectDao: AspectDaoService
) : ObjectValidator {
    override fun checkedForCreation(request: ObjectCreateRequest): ObjectWriteInfo {
        val subjectVertex = subjectService.findByIdStrict(request.subjectId)

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

    override fun checkedForUpdating(objectVertex: ObjectVertex, request: ObjectUpdateRequest): ObjectWriteInfo {
        val currentSubjectVertex = objectVertex.subject
        val currentSubjectId = currentSubjectVertex?.id ?: throw ObjectWithoutSubjectException(objectVertex.id)

        val newSubjectVertex = if (currentSubjectId == request.subjectId) currentSubjectVertex else subjectService.findByIdStrict(request.subjectId)
        val newSubjectId = newSubjectVertex.identity

        //check version (Maybe try to #load() or #reload() if supplied version is bigger than existing)
        if (objectVertex.version != request.version) {
            throw ObjectConcurrentEditException(objectVertex.id, objectVertex.name, objectVertex.subject?.name)
        }

        //check business key
        val existsAnotherObjectSameName = objectDaoService.getObjectVertexesByNameAndSubject(request.name, newSubjectId).any {
            it.id != request.id
        }
        if (existsAnotherObjectSameName) {
            throw ObjectAlreadyExists(request.name)
        }
        return ObjectWriteInfo(request.name, request.description, newSubjectVertex)
    }

    override fun checkedForCreation(request: PropertyCreateRequest): PropertyWriteInfo {
        val objectVertex = objectService.findById(request.objectId)
        val aspectVertex = aspectDao.find(request.aspectId) ?: throw AspectDoesNotExist(request.aspectId)

        val sameAspectProps = objectService.findPropertyByObjectAndAspect(objectVertex.id, aspectVertex.id).map { it.name }
        if (sameAspectProps.contains(request.name)) {
            throw ObjectPropertyAlreadyExistException(request.name, objectVertex.id, aspectVertex.id)
        }

        // check business key
        if (objectDaoService.getPropertyVertexesByNameAndAspect(request.name, objectVertex.identity, ORecordId(request.aspectId)).isNotEmpty()) {
            throw ObjectPropertyAlreadyExistException(request.name, objectVertex.id, request.aspectId)
        }

        return PropertyWriteInfo(
            request.name,
            request.description,
            objectVertex,
            aspectVertex
        )
    }

    override fun checkedForUpdating(propertyVertex: ObjectPropertyVertex, request: PropertyUpdateRequest): PropertyWriteInfo {
        val objectVertex = propertyVertex.objekt ?: throw IllegalStateException("Object property must be linked with object")
        val aspectVertex = propertyVertex.aspect ?: throw IllegalStateException("Object property must be linked with aspect")
        val aspectId = aspectVertex.identity

        // check version (Maybe retry if supplied version is bigger than existing)
        if (propertyVertex.version != request.version) {
            throw ObjectPropertyConcurrentEditException(propertyVertex.id, propertyVertex.name)
        }

        // check business key
        val existsAnotherPropertySameName = objectDaoService.getPropertyVertexesByNameAndAspect(request.name, objectVertex.identity, aspectId).any {
            it.id != propertyVertex.id
        }
        if (existsAnotherPropertySameName) {
            throw ObjectPropertyAlreadyExistException(request.name, objectVertex.id, aspectId.toString())
        }

        return PropertyWriteInfo(request.name, request.description, objectVertex, aspectVertex)
    }

    override fun checkedForCreation(request: ValueCreateRequest): ValueWriteInfo {
        logger.info("checking value for creation: $request")
        val objectPropertyVertex = objectService.findPropertyById(request.objectPropertyId)

        if (request.parentValueId != null && request.aspectPropertyId == null) {
            throw IllegalArgumentException("No aspect property id for non-root value")
        }

        if (request.parentValueId == null && request.aspectPropertyId != null) {
            throw IllegalArgumentException("There is aspect property ${request.aspectPropertyId} for root value creation")
        }

        val aspectPropertyVertex = request.aspectPropertyId?.let {
            aspectDao.findProperty(it) ?: throw AspectPropertyDoesNotExist(it)
        }

        val parentValueVertex = request.parentValueId?.let { objectService.findPropertyValueById(it) }

        val btStr = if (request.parentValueId != null)
            aspectPropertyVertex?.let { aspectDao.baseType(it) }
        else objectPropertyVertex.let { objectDaoService.baseType(it) }

        val baseType = btStr?.let { BaseType.restoreBaseType(it) } ?: throw IllegalStateException("Associated aspect has no base type")
        if (!request.value.assignableTo(baseType)) {
            throw IllegalArgumentException("Value ${request.value} is not compatible with type $baseType")
        }

        val measureVertex = request.measureId?.let { measureService.findById(it) }

        val measure = (measureVertex?.toMeasure() ?: when (request.parentValueId) {
            null -> objectPropertyVertex.aspect ?: throw IllegalStateException("Object property has no reference to aspect")
            else -> aspectPropertyVertex?.associatedAspect ?: throw IllegalArgumentException("No aspect property id for non-root value")
        }.measure) as? Measure<DecimalNumber>

        val dataValue = if (request.value is ObjectValueData.DecimalValue && measure != null) {
            val valueRepresentation = BigDecimal(request.value.valueRepr)
            ObjectValueData.DecimalValue(measure.toBase(DecimalNumber(valueRepresentation)).value.toString())
        } else {
            request.value
        }

        val value = getObjectValueFromData(dataValue)


        return ValueWriteInfo(value, request.description, objectPropertyVertex, aspectPropertyVertex, parentValueVertex, measureVertex)
    }

    override fun checkedForUpdating(valueVertex: ObjectPropertyValueVertex, request: ValueUpdateRequest): ValueWriteInfo {
        val objPropertyVertex = valueVertex.objectProperty
                ?: throw IllegalStateException("ObjectPropertyValue ${valueVertex.id} has no linked ObjectProperty")

        val objectPropertyVertex = objectService.findPropertyById(objPropertyVertex.id)
        val aspectPropertyVertex = valueVertex.aspectProperty

        if (valueVertex.version != request.version) {
            throw ObjectPropertyValueConcurrentModificationException(valueVertex.id)
        }

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

        val measure = (valueVertex.measure?.toMeasure() ?: when (parentValueVertex) {
            null -> objectPropertyVertex.aspect ?: throw IllegalStateException("Object property has no reference to aspect")
            else -> aspectPropertyVertex?.associatedAspect ?: throw IllegalArgumentException("No aspect property id for non-root value")
        }.measure) as? Measure<DecimalNumber>

        val dataValue = if (request.value is ObjectValueData.DecimalValue && measure != null) {
            val valueRepresentation = BigDecimal(request.value.valueRepr)
            ObjectValueData.DecimalValue(measure.toBase(DecimalNumber(valueRepresentation)).value.toString())
        } else {
            request.value
        }

        val value = getObjectValueFromData(dataValue)

        return ValueWriteInfo(
            value,
            request.description,
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