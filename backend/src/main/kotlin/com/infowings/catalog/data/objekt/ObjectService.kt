package com.infowings.catalog.data.objekt

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID

class ObjectService(
    private val db: OrientDatabase,
    private val dao: ObjectDaoService,
    subjectService: SubjectService,
    private val aspectDao: AspectDaoService,
    measureService: MeasureService,
    refBookService: ReferenceBookService,
    private val userService: UserService,
    private val historyService: HistoryService
) {
    private val validator = ObjectValidator(this, subjectService, measureService, refBookService, aspectDao)

    fun fetch(): List<ObjectTruncated> = dao.getTruncatedObjects()

    // TODO: KS-168 - possible bottleneck
    fun getDetailedObject(id: String) =
        transaction(db) {
            val objectVertex = dao.getObjectVertex(id) ?: throw ObjectNotFoundException(id)
            val subjectVertex = objectVertex.subject ?: throw ObjectWithoutSubjectException(objectVertex)
            val objectPropertyVertexes = objectVertex.properties

            return@transaction DetailedObjectResponse(
                objectVertex.id,
                objectVertex.name,
                objectVertex.description,
                subjectVertex.name,
                objectPropertyVertexes.size,
                objectPropertyVertexes.map(this::fetchPropertyValues)
            )
        }

    private fun fetchPropertyValues(propertyVertex: ObjectPropertyVertex): DetailedObjectPropertyResponse {
        val values = dao.getPropertyValues(propertyVertex)
        return DetailedObjectPropertyResponse(
            propertyVertex.id,
            propertyVertex.name,
            propertyVertex.description,
            propertyVertex.aspect?.toAspectData() ?: throw ObjectPropertyWithoutAspectException(propertyVertex.id),
            propertyVertex.cardinality.name,
            values
        )
    }

    fun getDetailedObjectForEdit(id: String) =
        transaction(db) {
            val objectVertex = dao.getObjectVertex(id) ?: throw ObjectNotFoundException(id)
            val objectSubject = objectVertex.subject ?: throw IllegalStateException("Object in database does not have a subject")
            val objectProperties = objectVertex.properties

            return@transaction ObjectEditDetailsResponse(
                objectVertex.id,
                objectVertex.name,
                objectVertex.description,
                objectSubject.name,
                objectSubject.id,
                objectProperties.map {
                    //TODO: Maybe performance bottleneck
                    val values = it.values.map {
                        ValueTruncated (
                            it.id,
                            it.toObjectPropertyValue().value.toObjectValueData().toDTO(),
                            it.aspectProperty?.id,
                            it.children.map { it.id }
                        )
                    }
                    ObjectPropertyEditDetailsResponse(
                        it.id,
                        it.name,
                        it.description,
                        it.cardinality,
                        values.filter { it.propertyId == null },
                        values,
                        aspectDao.getAspectTreeForProperty(it.identity)
                    )
                }
            )
        }

    fun create(request: ObjectCreateRequest, username: String): String {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)

        val createdVertex = transaction(db) {
            val objectInfo: ObjectCreateInfo = validator.checkedForCreation(request)

            /* В свете такого описания бизнес ключа не совсем понятно, как простым и эффективным образом обеспечивать
             * его уникальность при каждлом изменении:
             *
             * Комбинация всех сущностей Свойство."Ролевое имя" + "Значение свойства" в виде
             * List[pair(ID аспекта/свойства; элементарное значение/значение ссылочного типа/null)]
             * //формируется на основе Значение_свойств_объекта.Характеристика
             */

            val subjectBefore = objectInfo.subject.currentSnapshot()

            val newVertex = dao.newObjectVertex()

            val objectVertex = dao.saveObject(newVertex, objectInfo, emptyList())

            val createdObject = objectVertex.toObjekt()

            historyService.storeFact(createdObject.subject.toUpdateFact(context, subjectBefore))
            historyService.storeFact(objectVertex.toCreateFact(context))

            objectVertex
        }

        return createdVertex.id
    }

    fun create(request: PropertyCreateRequest, username: String): String {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        val propertyVertex = transaction(db) {
            val propertyInfo = validator.checkedForCreation(request)

            //validator.checkBusinessKey(objectProperty)

            val objectBefore = propertyInfo.objekt.currentSnapshot()

            val newVertex = dao.newObjectPropertyVertex()

            val propertyVertex: ObjectPropertyVertex = dao.saveObjectProperty(newVertex, propertyInfo, emptyList())

            historyService.storeFact(propertyVertex.toCreateFact(context))
            historyService.storeFact(propertyInfo.objekt.toUpdateFact(context, objectBefore))

            return@transaction propertyVertex
        }

        return propertyVertex.id
    }

    fun create(request: ValueCreateRequest, username: String): ObjectPropertyValue {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        return transaction(db) {
            val valueInfo: ValueWriteInfo = validator.checkedForCreation(request)

            val toTrack = listOfNotNull(valueInfo.objectProperty, valueInfo.parentValue)

            val valueVertex = historyService.trackUpdates(toTrack, context) {
                val newVertex = dao.newObjectValueVertex()
                dao.saveObjectValue(newVertex, valueInfo)
            }
            historyService.storeFact(valueVertex.toCreateFact(context))

            return@transaction valueVertex.toObjectPropertyValue()
        }
    }

    fun findByNameAndSubject(name: String, subjectId: String): ObjectVertex? = dao.getObjectVertexByNameAndSubject(name, subjectId)

    // Можно и отдельной sql но свойст не должно быть настолько запредельное количество, чтобы ударило по performance
    fun findPropertyByObjectAndAspect(objectId: String, aspectId: String): List<ObjectPropertyVertex> = transaction(db) {
        return@transaction dao.getObjectVertex(objectId)?.properties?.filter { it.aspect?.id == aspectId } ?: emptyList()
    }

    fun findById(id: String): ObjectVertex = dao.getObjectVertex(id) ?: throw ObjectNotFoundException(id)
    fun findPropertyById(id: String): ObjectPropertyVertex =
        dao.getObjectPropertyVertex(id) ?: throw ObjectPropertyNotFoundException(id)

    fun findPropertyValueById(id: String): ObjectPropertyValueVertex =
        dao.getObjectPropertyValueVertex(id) ?: throw ObjectPropertyValueNotFoundException(id)
}

class ObjectPropertyWithoutAspectException(id: String) : ObjectPropertyException("Object property with id $id does not have associated aspect")