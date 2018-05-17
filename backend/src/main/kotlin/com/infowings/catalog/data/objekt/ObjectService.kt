package com.infowings.catalog.data.objekt

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.common.ObjectPropertyData
import com.infowings.catalog.common.ObjectPropertyValueData
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.OrientDatabase
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
    private val validator = ObjectValidator(this, subjectService, measureService, refBookService, aspectDao)

    fun create(objectData: ObjectData, username: String): Objekt {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)
        return transaction(db) {
            val objekt = validator.checkedForCreation(objectData)

            /* В свете такого описания бизнес ключа не совсем понятно, как простым и эффективным образом обеспечивать
             * его уникальность при каждлом изменении:
             *
             * Комбинация всех сущностей Свойство."Ролевое имя" + "Значение свойства" в виде
             * List[pair(ID аспекта/свойства; элементарное значение/значение ссылочного типа/null)]
             * //формируется на основе Значение_свойств_объекта.Характеристика
             */

            val subjectBefore = objekt.subject.currentSnapshot()



            val newVertex = dao.newObjectVertex()

            val objectVertex = dao.saveObject(newVertex, objekt)

            val newObject = objectVertex.toObjekt()

            historyService.storeFact(newObject.subject.toUpdateFact(context, subjectBefore))
            historyService.storeFact(objectVertex.toCreateFact(context))

            newObject
        }
    }

    fun create(objectPropertyData: ObjectPropertyData, username: String): ObjectProperty {
        val userVertex = userService.findUserVertexByUsername(username)
        return transaction(db) {
            val objectProperty = validator.checkedForCreation(objectPropertyData)

            //validator.checkBusinessKey(objectProperty)

            val newVertex = dao.newObjectPropertyVertex()

            dao.saveObjectProperty(newVertex, objectProperty).toObjectProperty()
        }
    }

    fun create(objectValueData: ObjectPropertyValueData, username: String): ObjectPropertyValue {
        val userVertex = userService.findUserVertexByUsername(username)
        return transaction(db) {
            val objectValue = validator.checkedForCreation(objectValueData)

            //validator.checkBusinessKey(objectProperty)

            val newVertex = dao.newObjectValueVertex()

            dao.saveObjectValue(newVertex, objectValue).toObjectPropertyValue()
        }
    }

    fun findById(id: String): ObjectVertex = dao.getObjectVertex(id) ?: throw ObjectNotFoundException(id)
    fun findPropertyById(id: String): ObjectPropertyVertex =
        dao.getObjectPropertyVertex(id) ?: throw ObjectPropertyNotFoundException(id)
    fun findPropertyValueById(id: String): ObjectPropertyValueVertex =
        dao.getObjectPropertyValueVertex(id) ?: throw ObjectPropertyValueNotFoundException(id)
}