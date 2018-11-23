package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.fail

@ExtendWith(SpringExtension::class)
@SpringBootTest
class ObjectServiceExampleTest {
    @Autowired
    private lateinit var db: OrientDatabase
    @Autowired
    private lateinit var subjectService: SubjectService
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var objectService: ObjectService
    @Autowired
    private lateinit var refBookService: ReferenceBookService

    private val username = "admin"

    @Test
    fun createExampleTest() {
        val aspectCurrent = aspectService.save(
            AspectData(name = "Ток", baseType = BaseType.Decimal.name, measure = Ampere.name), username
        )
        val aspectTemperature = aspectService.save(
            AspectData(
                name = "Температура",
                description = "При нормальныхз условиях окружающей среды: T воздуха = 20 гр. C",
                measure = Celsius.name,
                baseType = BaseType.Decimal.name
            ), username
        )

        val propertyCurrentStage1 =
            AspectPropertyData("", "1-й ступени", aspectCurrent.idStrict(), PropertyCardinality.ONE.name, null)
        val propertyCurrentStage2 =
            AspectPropertyData("", "2-й ступени", aspectCurrent.idStrict(), PropertyCardinality.ONE.name, null)
        val propertyMaxTempr =
            AspectPropertyData("", "Max", aspectTemperature.idStrict(), PropertyCardinality.ONE.name, null)

        val aspectChargeMode = aspectService.save(
            AspectData(
                name = "Режим заряда",
                baseType = BaseType.Text.name,
                properties = listOf(propertyCurrentStage1, propertyCurrentStage2, propertyMaxTempr)
            ), username
        )


        val chargeModePropertyByAspectId = aspectChargeMode.properties.map { it.aspectId to it }.toMap()

        fun chargeModeProperty(aspectId: String): String =
            chargeModePropertyByAspectId[aspectId]?.id
                    ?: throw IllegalStateException("Not found property: $aspectId")


        val propertyChargeMode =
            AspectPropertyData("", null, aspectChargeMode.idStrict(), PropertyCardinality.INFINITY.name, null)
        val aspectChargeCharacteristic = aspectService.save(
            AspectData(
                name = "Характеристика заряда",
                measure = null,
                baseType = BaseType.Text.name,
                properties = listOf(propertyChargeMode)
            ), username
        )

        val refBook = refBookService.createReferenceBook("Режимы заряда", aspectChargeMode.idStrict(), username)
        val refBookItemIds = listOf("Ускоренный", "Номинальный", "Глубокий").map {
            val item = ReferenceBookItem(aspectChargeMode.idStrict(), it, "descr of $it", emptyList(), false, 0, null)
            refBookService.addReferenceBookItem(refBook.id, item, "admin")
        }
        refBookService.getReferenceBook(aspectChargeMode.idStrict())

        val subjectData = SubjectData(
            null, name = "ПАО \"Сатурн\"", description = "С 2005 года в ПАО \"Сатурн\"" +
                    " ведутся работы...."
        )
        val subjectSaturn = subjectService.createSubject(subjectData, username)


        val objectRequest = ObjectCreateRequest("ЛИГП-10", "some descr", subjectSaturn.id)
        val createObjectResponse = objectService.create(objectRequest, username)


        val propertyRequest = PropertyCreateRequest(
            objectId = createObjectResponse.id, name = "name", description = null,
            aspectId = aspectChargeCharacteristic.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val topValueId = propertyCreateResponse.rootValue.id

        val refValue11Data = LinkValueData.DomainElement(refBookItemIds[0], "", null)
        val value11Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue11Data),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            parentValueId = topValueId
        )
        val valueCreateResponse11 = objectService.create(value11Request, username)

        val refValue12Data = LinkValueData.DomainElement(refBookItemIds[1], "", null)
        val value12Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue12Data),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            parentValueId = topValueId
        )
        val valueCreateResponse12 = objectService.create(value12Request, username)

        val refValue13Data = LinkValueData.DomainElement(refBookItemIds[2], "", null)
        val value13Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue13Data),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            parentValueId = topValueId
        )
        val valueCreateResponse13 = objectService.create(value13Request, username)

        val value111Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue.single("3.0"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = Ampere.name,
            aspectPropertyId = chargeModeProperty(aspectCurrent.idStrict()),
            parentValueId = valueCreateResponse11.id
        )
        objectService.create(value111Request, username)

        val value112Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue.single("75"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = Celsius.name,
            aspectPropertyId = chargeModeProperty(aspectTemperature.idStrict()),
            parentValueId = valueCreateResponse11.id
        )
        objectService.create(value112Request, username)

        val value121Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue.single("0.8"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = Ampere.name,
            aspectPropertyId = chargeModeProperty(aspectCurrent.idStrict()),
            parentValueId = valueCreateResponse12.id
        )
        objectService.create(value121Request, username)

        val value131Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue.single("1.2"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = Ampere.name,
            aspectPropertyId = chargeModeProperty(aspectCurrent.idStrict()),
            parentValueId = valueCreateResponse13.id
        )
        objectService.create(value131Request, username)

        val value132Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue.single("0.3"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = Ampere.name,
            aspectPropertyId = chargeModeProperty(aspectCurrent.idStrict()),
            parentValueId = valueCreateResponse13.id
        )
        objectService.create(value132Request, username)

        val value133Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue.single("45"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = Celsius.name,
            aspectPropertyId = chargeModeProperty(aspectTemperature.idStrict()),
            parentValueId = valueCreateResponse13.id
        )
        objectService.create(value133Request, username)

        val foundObject = objectService.findById(createObjectResponse.id)

        assertEquals(objectRequest.name, foundObject.name, "name is incorrect")

        transaction(db) {
            assertEquals(1, foundObject.properties.size, "1 property is expected")
        }

        val foundObjectProperty = transaction(db) {
            foundObject.properties.first()
        }

        transaction(db) {
            assertEquals(10, foundObjectProperty.values.size, "9 properties are expected")

            foundObjectProperty.values.forEach {
                val property = it.objectProperty
                if (property == null) {
                    fail("object property is null for value with id ${it.id}")
                } else {
                    assertEquals(foundObjectProperty.id, property.id, "")
                }
            }
        }

        assertEquals(propertyRequest.name, foundObjectProperty.name, "property name is unexpecxted")
    }
}
