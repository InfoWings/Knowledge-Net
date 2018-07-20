package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ObjectServiceExampleTest {
    @Autowired
    private lateinit var db: OrientDatabase
    @Autowired
    private lateinit var dao: ObjectDaoService
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

    @Before
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
                baseType = BaseType.Decimal.name
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


        val objectRequest = ObjectCreateRequest("ЛИГП-10", "some descr", subjectSaturn.id, subjectSaturn.version)
        val createdObjectId = objectService.create(objectRequest, username)


        val propertyRequest = PropertyCreateRequest(
            objectId = createdObjectId, name = "name", description = null,
            aspectId = aspectChargeCharacteristic.idStrict()
        )
        val createdPropertyId: String = objectService.create(propertyRequest, username)

        val topValueRequest = ValueCreateRequest(ObjectValueData.NullValue, createdPropertyId)
        val topValueId = objectService.create(topValueRequest, username).id?.toString()

        val refValue11Data = LinkValueData.DomainElement(refBookItemIds[0])
        val value11Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue11Data),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            measureId = null,
            parentValueId = topValueId
        )
        val createdValue11 = objectService.create(value11Request, username)

        val refValue12Data = LinkValueData.DomainElement(refBookItemIds[1])
        val value12Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue12Data),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            measureId = null,
            parentValueId = topValueId
        )
        val createdValue12 = objectService.create(value12Request, username)

        val refValue13Data = LinkValueData.DomainElement(refBookItemIds[2])
        val value13Request = ValueCreateRequest(
            value = ObjectValueData.Link(refValue13Data),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = aspectChargeCharacteristic.properties[0].id,
            measureId = null,
            parentValueId = topValueId
        )
        val createdValue13 = objectService.create(value13Request, username)

        val value111Request = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(3, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = chargeModeProperty(aspectStage1.idStrict()),
            parentValueId = createdValue11.id.toString(),
            measureId = null
        )
        val createdValue111 = objectService.create(value111Request, username)

        val value112Request = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(75, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = chargeModeProperty(aspectMaxTempr.idStrict()),
            parentValueId = createdValue11.id.toString(),
            measureId = null
        )
        val createdValue112 = objectService.create(value112Request, username)

        val ampereMeasure = measureService.findMeasure("Ampere")

        val value121Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue("0.8"),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = chargeModeProperty(aspectStage1.idStrict()),
            parentValueId = createdValue12.id.toString(),
            measureId = ampereMeasure?.id
        )
        val createdValue121 = objectService.create(value121Request, username)

        val value131Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue("1.2"),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = aspectChargeMode.id,
            parentValueId = createdValue13.id.toString(),
            measureId = ampereMeasure?.id
        )
        val createdValue131 = objectService.create(value131Request, username)

        val value132Request = ValueCreateRequest(
            value = ObjectValueData.DecimalValue("0.3"),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = chargeModeProperty(aspectStage2.idStrict()),
            parentValueId = createdValue13.id.toString(),
            measureId = ampereMeasure?.id
        )
        val createdValue132 = objectService.create(value132Request, username)

        val value133Request = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(45, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = chargeModeProperty(aspectMaxTempr.idStrict()),
            parentValueId = createdValue13.id.toString(),
            measureId = null
        )
        val createdValue133 = objectService.create(value133Request, username)

        if (createdObjectId == null) {
            fail("id of saved object is null")
        } else {
            val foundObject = objectService.findById(createdObjectId)

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
}
