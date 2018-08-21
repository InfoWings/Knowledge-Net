package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.transaction
import junit.framework.Assert.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals


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
    private lateinit var measureService: MeasureService
    @Autowired
    private lateinit var objectService: ObjectService
    @Autowired
    private lateinit var refBookService: ReferenceBookService

    private lateinit var subject: Subject

    private lateinit var aspect: AspectData

    private lateinit var complexAspect: AspectData

    private val username = "admin"

    @BeforeEach
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(
            AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username
        )
        val property = AspectPropertyData("", "p", aspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            "",
            "complex",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property)
        )
        complexAspect = aspectService.save(complexAspectData, username)
    }

    @Test
    fun createExampleTest() {
        val aspectStage1 = aspectService.save(
            AspectData(name = "current1", baseType = BaseType.Decimal.name), username
        )
        val aspectStage2 = aspectService.save(
            AspectData(name = "current2", baseType = BaseType.Decimal.name), username
        )
        val aspectMaxTempr = aspectService.save(
            AspectData(
                name = "temperature",
                description = "При нормальныхз условиях окружающей среды: T воздуха = 20 гр. C",
                baseType = BaseType.Integer.name
            ), username
        )

        val propertyStage1 =
            AspectPropertyData("", "1-й ступени ток", aspectStage1.idStrict(), PropertyCardinality.ONE.name, null)
        val propertyStage2 =
            AspectPropertyData("", "2-й ступени ток", aspectStage2.idStrict(), PropertyCardinality.ONE.name, null)
        val propertyMaxTempr =
            AspectPropertyData("", "Max температура", aspectMaxTempr.idStrict(), PropertyCardinality.ONE.name, null)

        val aspectChargeMode = aspectService.save(
            AspectData(
                name = "сharge",
                baseType = BaseType.Text.name,
                properties = listOf(propertyStage1, propertyStage2, propertyMaxTempr)
            ), username
        )


        val chargeModePropertyByAspectId = aspectChargeMode.properties.map { it.aspectId to it }.toMap()

        fun chargeModeProperty(aspectId: String): String =
            chargeModePropertyByAspectId[aspectId]?.id
                    ?: throw IllegalStateException("Not found property: $aspectStage1")


        val propertyChargeMode =
            AspectPropertyData("", "Режим заряда", aspectChargeMode.idStrict(), PropertyCardinality.INFINITY.name, null)
        val aspectChargeCharacteristic = aspectService.save(
            AspectData(
                name = "сharge-characteristic",
                baseType = BaseType.Text.name,
                properties = listOf(propertyChargeMode)
            ), username
        )

        val refBook = refBookService.createReferenceBook("rb-charge", aspectChargeMode.idStrict(), username)
        val refBookItemIds = listOf("Ускоренный", "Номинальный", "Глубокий").map {
            val item = ReferenceBookItem(aspectChargeMode.idStrict(), it, "descr of $it", emptyList(), false, 0)
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

        val refValue11Data = LinkValueData.DomainElement(refBookItemIds[0])
        val value11Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue11Data),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            measureId = null,
            parentValueId = topValueId
        )
        val valueCreateResponse11 = objectService.create(value11Request, username)

        val refValue12Data = LinkValueData.DomainElement(refBookItemIds[1])
        val value12Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue12Data),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            measureId = null,
            parentValueId = topValueId
        )
        val valueCreateResponse12 = objectService.create(value12Request, username)

        val refValue13Data = LinkValueData.DomainElement(refBookItemIds[2])
        val value13Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue13Data),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            measureId = null,
            parentValueId = topValueId
        )
        val valueCreateResponse13 = objectService.create(value13Request, username)

        val value111Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue("3.0"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = chargeModeProperty(aspectStage1.idStrict()),
            parentValueId = valueCreateResponse11.id,
            measureId = null
        )
        objectService.create(value111Request, username)

        val value112Request = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(75, null),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = chargeModeProperty(aspectMaxTempr.idStrict()),
            parentValueId = valueCreateResponse11.id,
            measureId = null
        )
        objectService.create(value112Request, username)

        val ampereMeasure = measureService.findMeasure("Ampere")

        val value121Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue("0.8"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = chargeModeProperty(aspectStage1.idStrict()),
            parentValueId = valueCreateResponse12.id,
            measureId = ampereMeasure?.id
        )
        objectService.create(value121Request, username)

        val value131Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue("1.2"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = chargeModeProperty(aspectStage1.idStrict()),
            parentValueId = valueCreateResponse13.id,
            measureId = ampereMeasure?.id
        )
        objectService.create(value131Request, username)

        val value132Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue("0.3"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = chargeModeProperty(aspectStage2.idStrict()),
            parentValueId = valueCreateResponse13.id,
            measureId = ampereMeasure?.id
        )
        objectService.create(value132Request, username)

        val value133Request = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(45, null),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = chargeModeProperty(aspectMaxTempr.idStrict()),
            parentValueId = valueCreateResponse13.id,
            measureId = null
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
