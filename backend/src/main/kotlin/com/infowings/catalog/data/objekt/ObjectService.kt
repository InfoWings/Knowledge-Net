package com.infowings.catalog.data.objekt

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.common.ObjectPropertyData
import com.infowings.catalog.common.ObjectPropertyValueData
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction

class ObjectService(
    private val db: OrientDatabase,
    private val dao: ObjectDaoService,
    subjectService: SubjectService,
    aspectService: AspectService,
    measureService: MeasureService,
    private val userService: UserService
) {
    private val validator = ObjectValidator(this, subjectService, measureService, aspectService)

    fun create(objectData: ObjectData, username: String): Objekt {
        val userVertex = userService.findUserVertexByUsername(username)
        val resultVertex = transaction(db) {
            val objeckt = validator.checkedForCreation(objectData)

            /* В свете такого описания бизнес ключа не совсем понятно, как простым и эффективным образом обеспечивать
             * его уникальность при каждлом изменении:
             *
             * Комбинация всех сущностей Свойство."Ролевое имя" + "Значение свойства" в виде
             * List[pair(ID аспекта/свойства; элементарное значение/значение ссылочного типа/null)]
             * //формируется на основе Значение_свойств_объекта.Характеристика
             */

            val newVertex = dao.newObjectVertex()

            dao.saveObject(newVertex, objeckt)
        }

        return resultVertex.toObjekt()
    }

    fun create(objectPropertyData: ObjectPropertyData, username: String): ObjectProperty {
        val userVertex = userService.findUserVertexByUsername(username)
        val resultVertex = transaction(db) {
            val objectProperty = validator.checkedForCreation(objectPropertyData)

            //validator.checkBusinessKey(objectProperty)

            val newVertex = dao.newObjectPropertyVertex()

            dao.saveObjectProperty(newVertex, objectProperty)
        }

        return resultVertex.toObjectProperty()
    }

    fun create(objectValueData: ObjectPropertyValueData, username: String): ObjectPropertyValue {
        val userVertex = userService.findUserVertexByUsername(username)
        val resultVertex = transaction(db) {
            val objectValue = validator.checkedForCreation(objectValueData)

            //validator.checkBusinessKey(objectProperty)

            val newVertex = dao.newObjectValueVertex()

            dao.saveObjectValue(newVertex, objectValue)
        }

        return resultVertex.toObjectPropertyValue()
    }

    fun findById(id: String): ObjectVertex = dao.getObjectVertex(id) ?: throw ObjectNotFoundException(id)
    fun findPropertyById(id: String): ObjectPropertyVertex = dao.getObjectPropertyVertex(id) ?: throw ObjectPropertyNotFoundException(id)
}