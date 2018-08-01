package com.infowings.catalog.data.objekt

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.history.HistoryAware
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
    private val validator: ObjectValidator = TrimmingObjectValidator(MainObjectValidator(this, subjectService, measureService, refBookService, dao, aspectDao))

    fun fetch(): List<ObjectTruncated> = dao.getTruncatedObjects()

    // TODO: KS-168 - possible bottleneck
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
                    //TODO: KS-168 Maybe performance bottleneck
                    val values = it.values.map {
                        ValueTruncated (
                            it.id,
                            it.toObjectPropertyValue().value.toObjectValueData().toDTO(),
                            it.description,
                            it.aspectProperty?.id,
                            it.children.map { it.id }
                        )
                    }
                    ObjectPropertyEditDetailsResponse(
                        it.id,
                        it.name,
                        it.description,
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
            val propertyInfo = validator.checkedForUpdating(objectPropertyVertex, request)

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

    private data class DeleteValueContext(
        val root: ObjectPropertyValueVertex,
        val values: Set<ObjectPropertyValueVertex>,
        val valueBlockers: Map<ORID, Set<ORID>>,
        val historyContext: HistoryContext
    )

    private fun deleteValue(id: String, username: String, deleteOp: (DeleteValueContext) -> Unit) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            val rootVertex = findPropertyValueById(id)
            val values: Set<ObjectPropertyValueVertex> = dao.getSubValues(rootVertex.id).plus(rootVertex)
            val valueIds = values.map { it.identity }.toSet()

            val valueBlockers = dao.linkedFrom(valueIds, setOf(OBJECT_VALUE_REF_OBJECT_VALUE_EDGE), valueIds)

            deleteOp(DeleteValueContext(root = rootVertex, values = values, valueBlockers = valueBlockers, historyContext = context))

            return@transaction
        }
    }

    fun deleteValue(id: String, username: String) {
        fun removeOrThrow(context: DeleteValueContext) {
            if (context.valueBlockers.isNotEmpty()) {
                throw ObjectValueIsLinkedException(context.valueBlockers.map { it.toString() }.toList())
            }

            context.values.forEach { historyService.storeFact(it.toDeleteFact(context.historyContext)) }

            dao.deleteAll(context.values.toList())

        }

        deleteValue(id, username, ::removeOrThrow)
    }

    fun softDeleteValue(id: String, username: String) {
        fun removeOrMark(context: DeleteValueContext) {
            val blockerIds = context.valueBlockers.keys.toSet()
            val rootSet = setOf(context.root.identity)
            val rootBlockerSet = if (blockerIds.contains(context.root.identity)) setOf(context.root) else emptySet()
            val valuesToKeep = dao.valuesBetween(blockerIds, rootSet).plus(rootBlockerSet)

            val valuesToDelete = context.values - valuesToKeep

            valuesToDelete.forEach { historyService.storeFact(it.toDeleteFact(context.historyContext)) }
            valuesToKeep.forEach { historyService.storeFact(it.toSoftDeleteFact(context.historyContext)) }

            dao.deleteAll(valuesToDelete.toList())
            valuesToKeep.forEach {
                dao.softDelete(it)
            }
        }

        deleteValue(id, username, ::removeOrMark)
    }

    private data class DeletePropertyContext(
        val propVertex: ObjectPropertyVertex,
        val values: Set<ObjectPropertyValueVertex>,
        val propIsLinked: Boolean,
        val valueBlockers: Map<ORID, Set<ORID>>,
        val historyContext: HistoryContext
    )

    private fun deleteProperty(id: String, username: String, deleteOp: (DeletePropertyContext) -> Unit) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            val propVertex = findPropertyById(id)
            val values = dao.valuesOfProperty(id)

            val valueBlockers = dao.linkedFrom(values.map { it.identity }.toSet(), setOf(OBJECT_VALUE_REF_OBJECT_VALUE_EDGE))
            val propLinks = dao.linkedFrom(setOf(propVertex.identity), setOf(OBJECT_VALUE_REF_OBJECT_PROPERTY_EDGE))
            val propLinksExt = propLinks.mapValues { it.value.minus(values) }.filterValues { it.isNotEmpty() }
            val propIsLinked = propLinksExt.isNotEmpty()

            deleteOp(
                DeletePropertyContext(
                    propVertex = propVertex, values = values, propIsLinked = propIsLinked,
                    valueBlockers = valueBlockers, historyContext = context
                )
            )

            return@transaction
        }
    }

    fun deleteProperty(id: String, username: String) {
        fun removeOrThrow(context: DeletePropertyContext) {
            if (context.valueBlockers.isNotEmpty() || context.propIsLinked) {
                throw ObjectPropertyIsLinkedException(
                    context.valueBlockers.map { it.toString() }.toList(),
                    if (context.propIsLinked) context.propVertex.id else null
                )
            }

            context.values.forEach { historyService.storeFact(it.toDeleteFact(context.historyContext)) }
            historyService.storeFact(context.propVertex.toDeleteFact(context.historyContext))

            dao.deleteAll(context.values.toList().plus(context.propVertex))

        }

        deleteProperty(id, username, ::removeOrThrow)
    }

    fun softDeleteProperty(id: String, username: String) {
        fun removeOrMark(context: DeletePropertyContext) {
            val rootValues = context.values.filter { it.parentValue == null }

            val valuesToKeep = dao.valuesBetween(context.valueBlockers.values.flatten().toSet(), rootValues.map {it.identity}.toSet())

            val valuesToDelete = context.values - valuesToKeep

            valuesToDelete.forEach { historyService.storeFact(it.toDeleteFact(context.historyContext)) }
            valuesToKeep.forEach { historyService.storeFact(it.toSoftDeleteFact(context.historyContext)) }

            if (context.propIsLinked || valuesToKeep.isNotEmpty()) {
                historyService.storeFact(context.propVertex.toSoftDeleteFact(context.historyContext))
                dao.softDelete(context.propVertex)
            } else {
                historyService.storeFact(context.propVertex.toDeleteFact(context.historyContext))
                dao.delete(context.propVertex)
            }

            dao.deleteAll(valuesToDelete.toList())
        }

        deleteProperty(id, username, ::removeOrMark)
    }

    private data class DeleteObjectContext(
        val objVertex: ObjectVertex,
        val properties: Set<ObjectPropertyVertex>,
        val values: Set<ObjectPropertyValueVertex>,
        val objIsLinked: Boolean,
        val propertyBlockers: Map<ORID, Set<ORID>>,
        val valueBlockers: Map<ORID, Set<ORID>>,
        val historyContext: HistoryContext
    )

    private fun deleteObject(id: String, username: String, deleteOp: (DeleteObjectContext) -> Unit) {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        transaction(db) {
            val objVertex = findById(id)
            val properties = dao.propertiesOfObject(id)

            val values = dao.valuesOfProperties(properties.map { it.identity })

            val valueBlockers = dao.linkedFrom(values.map { it.identity }.toSet(), setOf(OBJECT_VALUE_REF_OBJECT_VALUE_EDGE))
            val propertyBlockers = dao.linkedFrom(properties.map { it.identity }.toSet(), setOf(OBJECT_VALUE_REF_OBJECT_PROPERTY_EDGE))
            val objLinks = dao.linkedFrom(setOf(objVertex.identity), setOf(OBJECT_VALUE_OBJECT_EDGE))
            val objLinksExt = objLinks.mapValues { it.value.minus(values) }.filterValues { it.isNotEmpty() }
            val objIsLinked = objLinksExt.isNotEmpty()

            deleteOp(
                DeleteObjectContext(
                    objVertex = objVertex, properties = properties, values = values, objIsLinked = objIsLinked,
                    propertyBlockers = propertyBlockers, valueBlockers = valueBlockers, historyContext = context
                )
            )

            return@transaction
        }

    }

    fun deleteObject(id: String, username: String) {
        fun removeOrThrow(context: DeleteObjectContext) {
            if (context.valueBlockers.isNotEmpty() || context.propertyBlockers.isNotEmpty() || context.objIsLinked) {
                throw ObjectIsLinkedException(
                    context.valueBlockers.map { it.toString() }.toList(),
                    context.propertyBlockers.map { it.toString() }.toList(),
                    if (context.objIsLinked) context.objVertex.id else null
                )
            }

            context.values.forEach { historyService.storeFact(it.toDeleteFact(context.historyContext)) }
            context.properties.forEach { historyService.storeFact(it.toDeleteFact(context.historyContext)) }
            historyService.storeFact(context.objVertex.toDeleteFact(context.historyContext))

            dao.deleteAll((context.values + context.properties + context.objVertex).toList())
        }

        deleteObject(id, username, ::removeOrThrow)
    }

    fun softDeleteObject(id: String, username: String) {
        fun removeOrMark(context: DeleteObjectContext) {
            val rootValues = context.values.filter { it.parentValue == null }

            val valuesToKeep = dao.valuesBetween(context.valueBlockers.values.flatten().toSet(), rootValues.map {it.identity}.toSet())
            val propertiesToKeep = context.properties.filter { context.propertyBlockers.contains(it.identity) }.toSet() +
                    valuesToKeep.map { it.objectProperty ?: throw IllegalStateException("no property for value ${it.identity}") }

            val valuesToDelete = context.values - valuesToKeep
            val propertiesToDelete = context.properties - propertiesToKeep

            valuesToDelete.forEach { historyService.storeFact(it.toDeleteFact(context.historyContext)) }
            propertiesToDelete.forEach { historyService.storeFact(it.toDeleteFact(context.historyContext)) }

            valuesToKeep.forEach { historyService.storeFact(it.toSoftDeleteFact(context.historyContext)) }
            propertiesToKeep.forEach { historyService.storeFact(it.toSoftDeleteFact(context.historyContext)) }

            if (context.objIsLinked || propertiesToKeep.isNotEmpty()) {
                historyService.storeFact(context.objVertex.toSoftDeleteFact(context.historyContext))
                dao.softDelete(context.objVertex)
            } else {
                historyService.storeFact(context.objVertex.toDeleteFact(context.historyContext))
                dao.delete(context.objVertex)
            }

            dao.deleteAll((propertiesToDelete + valuesToDelete).toList())
        }

        deleteObject(id, username, ::removeOrMark)
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
