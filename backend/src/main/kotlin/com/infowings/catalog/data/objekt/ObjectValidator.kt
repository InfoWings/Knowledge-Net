package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.common.ObjectPropertyData
import com.infowings.catalog.common.ObjectPropertyValueData
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectNotFoundException
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectService
import com.orientechnologies.orient.core.id.ORecordId

/* По опыту предыдущих сущностей, концепция валидатора модифицирована:
 * по итогам валидации создаем Objekt, готовый к записи в базу
 *
 * Без этого получается дупликация вызовов. Например, приделся сначала вызвать
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
    private val aspectService: AspectService) {
    fun checkedForCreation(data: ObjectData): Objekt {
        val subjectVertex = subjectService.findById(data.subjectId)

        if (data.id != null) {
            throw IllegalStateException("id must be null for creation: $data")
        }

        if (subjectVertex == null) {
            throw SubjectNotFoundException(data.subjectId)
        }

        if (data.propertyIds.isNotEmpty()) {
            throw IllegalStateException("there should be no properties for object creation: $data")
        }

        val trimmedName = data.name.trim()
        if (trimmedName.isEmpty()) {
            throw EmptyObjectNameException(data)
        }

        return Objekt(
            data.id ?.let { ORecordId(it) },
            trimmedName,
            data.description,
            subjectVertex,
            emptyList()
        )
    }

    fun checkedForCreation(data: ObjectPropertyData): ObjectProperty {
        val objectVertex = objectService.findById(data.objectId)
        val aspectVertex = aspectService.findVertexById(data.aspectId)

        if (data.id != null) {
            throw IllegalStateException("id must be null for creation: $data")
        }

        if (objectVertex == null) {
            throw ObjectNotFoundException(data.objectId)
        }

        if (aspectVertex == null) {
            throw AspectDoesNotExist(data.aspectId)
        }

        if (data.valueIds.isNotEmpty()) {
            throw IllegalStateException("there should be no values for object creation: $data")
        }

        val trimmedName = data.name.trim()
        if (trimmedName.isEmpty()) {
            throw EmptyObjectPropertyNameException(data)
        }

        return ObjectProperty(
            data.id ?.let { ORecordId(it) },
            trimmedName,
            data.cardinality,
            objectVertex,
            aspectVertex,
            emptyList()
        )
    }

    fun checkedForCreation(data: ObjectPropertyValueData): CheckedObjectValue {
        val objectPropertyVertex = objectService.findPropertyById(data.objectPropertyId)

        if (data.id != null) {
            throw IllegalStateException("id must be null for creation: $data")
        }

        val characteristics = data.characteristics.map {
            val aspectVertex = aspectService.findVertexById(it.aspectId)
            val aspectPropertyVertex = aspectService.findPropertyVertexById(it.aspectPropertyId)
            val measurePropertyVertex = measureService.findById(it.measureId)

            Characteristic(aspectVertex, aspectPropertyVertex, measurePropertyVertex)
        }

        /*
         * Здесь мы собрали данные про характеристики. Но создавать вершины для характеристик мы не будем
         * Потому что здесь не место
         *
         * А поле characteristics в ObjectPropertyValue - для вершин. Поэтому мы возвращаем здесь
         * пару <Value, список характеристик>
         *
         */

        return Pair(ObjectPropertyValue(
            data.id ?.let { ORecordId(it) },
            data.value,
            data.range,
            data.precision,
            objectPropertyVertex,
            emptyList()
        ), characteristics)
    }

    fun checkBusinessKey(property: ObjectProperty) {

    }
}

typealias CheckedObjectValue =  Pair<ObjectPropertyValue, List<Characteristic>>
