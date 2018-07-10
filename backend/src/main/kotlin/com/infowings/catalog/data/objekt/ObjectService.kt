package com.infowings.catalog.data.objekt

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.DetailedObjectPropertyResponse
import com.infowings.catalog.common.DetailedObjectResponse
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.description
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.transaction

class ObjectService(
    private val db: OrientDatabase,
    private val dao: ObjectDaoService,
    subjectService: SubjectService,
    aspectDao: AspectDaoService,
    measureService: MeasureService,
    refBookService: ReferenceBookService,
    private val userService: UserService,
    private val historyService: HistoryService
) {
    private val validator = ObjectValidator(this, subjectService, measureService, refBookService, dao, aspectDao)

    fun fetch(): List<ObjectTruncated> = dao.getTruncatedObjects()

    fun getDetailedObject(id: String) =
        transaction(db) {
            val objectVertex = dao.getObjectVertex(id) ?: throw ObjectNotFoundException(id)
            val subjectVertex = objectVertex.subject ?: throw IllegalStateException("Object ${objectVertex.id} without subject")
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
            propertyVertex.aspect?.toAspectData() ?: throw IllegalStateException("Object property ${propertyVertex.id} without aspect"),
            propertyVertex.cardinality.name,
            values
        )
    }

    fun create(request: ObjectCreateRequest, username: String): String {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)

        val createdVertex = transaction(db) {
            val objectInfo: ObjectWriteInfo = validator.checkedForCreation(request)

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

    fun update(request: ObjectUpdateRequest, username: String): String {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)

        val updatedVertex = transaction(db) {
            var objectVertex = findById(request.id)
            val objectInfo = validator.checkedForUpdating(objectVertex, request)
            val objectBefore = objectVertex.currentSnapshot()
            objectVertex = dao.updateObject(objectVertex, objectInfo)
            historyService.storeFact(objectVertex.toUpdateFact(context, objectBefore))
        }

        return updatedVertex.id
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

    fun update(request: PropertyUpdateRequest, username: String): String {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        val propertyVertex = transaction(db) {
            val objectPropertyVertex = findPropertyById(request.objectPropertyId)
            val propertyInfo = validator.checkForUpdating(objectPropertyVertex, request)

            val objectBefore = propertyInfo.objekt.currentSnapshot()
            val propertyVertex: ObjectPropertyVertex = dao.saveObjectProperty(objectPropertyVertex, propertyInfo, emptyList())

            historyService.storeFact(propertyVertex.toCreateFact(context))
            historyService.storeFact(objectPropertyVertex.toUpdateFact(context, objectBefore))

            return@transaction propertyVertex
        }

        return propertyVertex.id
    }

    fun create(request: ValueCreateRequest, username: String): ObjectPropertyValue {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        return transaction(db) {
            val valueInfo: ValueWriteInfo = validator.checkedForCreation(request)

            val toTrack: List<HistoryAware> = listOfNotNull(valueInfo.objectProperty, valueInfo.parentValue)

            val valueVertex = historyService.trackUpdates(toTrack, context) {
                val newVertex = dao.newObjectValueVertex()
                dao.saveObjectValue(newVertex, valueInfo)
            }
            historyService.storeFact(valueVertex.toCreateFact(context))

            return@transaction valueVertex.toObjectPropertyValue()
        }
    }

    fun update(request: ValueUpdateRequest, username: String): ObjectPropertyValue {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        return transaction(db) {
            var objectPropertyValue = findPropertyValueById(request.valueId)
            val valueInfo: ValueWriteInfo = validator.checkedForUpdating(objectPropertyValue, request)
            val before = objectPropertyValue.currentSnapshot()
            objectPropertyValue = dao.saveObjectValue(objectPropertyValue, valueInfo)
            historyService.storeFact(objectPropertyValue.toUpdateFact(context, before))

            return@transaction objectPropertyValue.toObjectPropertyValue()
        }
    }

    fun deleteValue(id: String, username: String) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            var valueVertex = findPropertyValueById(id)

            historyService.storeFact(valueVertex.toDeleteFact(context))

            val deleteInfo = validator.checkedForRemoval(valueVertex)

            dao.delete(deleteInfo)

            return@transaction
        }
    }

    fun softDeleteValue(id: String, username: String) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            var valueVertex = findPropertyValueById(id)

            historyService.storeFact(valueVertex.toDeleteFact(context))

            dao.softDelete(valueVertex)

            return@transaction
        }
    }

    fun deleteProperty(id: String, username: String) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            var propVertex = findPropertyById(id)

            historyService.storeFact(propVertex.toDeleteFact(context))

            val deleteInfo = validator.checkedForRemoval(propVertex)

            dao.delete(deleteInfo)

            return@transaction
        }
    }

    fun softDeleteProperty(id: String, username: String) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            var propVertex = findPropertyValueById(id)

            historyService.storeFact(propVertex.toDeleteFact(context))

            dao.softDelete(propVertex)

            return@transaction
        }
    }

    fun deleteObject(id: String, username: String) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            var objVertex = findById(id)

            historyService.storeFact(objVertex.toDeleteFact(context))

            val deleteInfo = validator.checkedForRemoval(objVertex)

            dao.delete(deleteInfo)

            return@transaction
        }
    }

    fun softDeleteObject(id: String, username: String) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            var objectVertex = findById(id)

            historyService.storeFact(objectVertex.toDeleteFact(context))

            dao.softDelete(objectVertex)

            return@transaction
        }
    }


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
