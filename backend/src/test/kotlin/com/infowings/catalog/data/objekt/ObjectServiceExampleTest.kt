package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.Aspect
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
class  ObjectServiceExampleTest {
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

    private lateinit var aspect: Aspect

    private lateinit var complexAspect: Aspect

    private val username = "admin"

    @Before
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(
            AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username)
        val property = AspectPropertyData("", "p", aspect.id, PropertyCardinality.INFINITY.name)
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

        val propertyStage1 = AspectPropertyData("", "1-й ступени ток", aspectStage1.id, PropertyCardinality.ONE.name)
        val propertyStage2 = AspectPropertyData("", "2-й ступени ток", aspectStage2.id, PropertyCardinality.ONE.name)
        val propertyMaxTempr =
            AspectPropertyData("", "Max температура", aspectMaxTempr.id, PropertyCardinality.ONE.name)

        val aspectChargeMode = aspectService.save(
            AspectData(
                name = "сharge",
                baseType = BaseType.Text.name,
                properties = listOf(propertyStage1, propertyStage2, propertyMaxTempr)
            ), username
        )

        val chargeModePropertyByAspectId = aspectChargeMode.properties.map {it.aspect.id to it}.toMap()

        fun chargeModeProperty(aspectId: String): String =
            chargeModePropertyByAspectId[aspectId]?.id ?: throw IllegalStateException("Not found property: $aspectStage1")


        val propertyChargeMode = AspectPropertyData("", "Режим заряда", aspectChargeMode.id, PropertyCardinality.INFINITY.name)
        val aspectChargeCharacteristic = aspectService.save(
            AspectData(
                name = "сharge-characteristic",
                baseType = BaseType.Text.name,
                properties = listOf(propertyChargeMode)
            ), username
        )


        val refBook = refBookService.createReferenceBook("rb-charge", aspectChargeMode.id, username)
        val refBookItemIds = listOf("Ускоренный", "Номинальный", "Глубокий").map {
            val item = ReferenceBookItem(aspectChargeMode.id, refBook.root.id, "", it, emptyList(), false, 0)
            refBookService.addReferenceBookItem(item, "admin")
        }
        refBookService.getReferenceBook(aspectChargeMode.id)

        val subjectData = SubjectData(
            null, name = "ПАО \"Сатурн\"", description = "С 2005 года в ПАО \"Сатурн\"" +
                    " ведутся работы...."
        )
        val subjectSaturn = subjectService.createSubject(subjectData, username)


        val objectData = ObjectData(null, "ЛИГП-10", "some descr", subjectSaturn.id, emptyList())

        val savedObject = objectService.create(objectData, username)

        val objectPropertyData = ObjectPropertyData(
            null, "name", PropertyCardinality.INFINITY,
            savedObject.id.toString(), aspectChargeCharacteristic.id, emptyList()
        )

        val savedObjectProperty = objectService.create(objectPropertyData, username)

        val refValue11Data = LinkValueData(LinkTypeGroup.DOMAIN_ELEMENT, refBookItemIds[0])
        val value11Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Link(refValue11Data),
            savedObjectProperty.id.toString(),
            aspectChargeCharacteristic.properties[0].id,
            null,
            null
        )
        val savedValue11 = objectService.create(value11Data, username)

        val refValue12Data = LinkValueData(LinkTypeGroup.DOMAIN_ELEMENT, refBookItemIds[1])
        val value12Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Link(refValue12Data),
            savedObjectProperty.id.toString(),
            aspectChargeCharacteristic.properties[0].id,
            null,
            null
        )
        val savedValue12 = objectService.create(value12Data, username)

        val refValue13Data = LinkValueData(LinkTypeGroup.DOMAIN_ELEMENT, refBookItemIds[2])
        val value13Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Link(refValue13Data),
            savedObjectProperty.id.toString(),
            aspectChargeCharacteristic.properties[0].id,
            null,
            null
        )
        val savedValue13 = objectService.create(value13Data, username)

        val value111Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Scalar(ScalarValue.IntegerValue(3), null, null),
            savedObjectProperty.id.toString(),
            chargeModeProperty(aspectStage1.id),
            savedValue11.id.toString(),
            null
        )
        val savedValue111 = objectService.create(value111Data, username)

        val value112Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Scalar(ScalarValue.IntegerValue(75), null, null),
            savedObjectProperty.id.toString(),
            chargeModeProperty(aspectMaxTempr.id),
            savedValue11.id.toString(),
            null
        )
        val savedValue112 = objectService.create(value112Data, username)


        val ampereMeasure = measureService.findMeasure("Ampere")

        val value121Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Scalar(ScalarValue.StringValue("0.8"), null, null),
            /* возможно, нужен еще тип с фиксированной точкой */
            savedObjectProperty.id.toString(),
            chargeModeProperty(aspectStage1.id),
            savedValue12.id.toString(),
            ampereMeasure?.id
        )
        val savedValue121 = objectService.create(value121Data, username)

        val value131Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Scalar(ScalarValue.StringValue("1.2"), null, null),
            savedObjectProperty.id.toString(),
            aspectChargeMode.id,
            savedValue13.id.toString(),
            ampereMeasure?.id
        )
        val savedValue131 = objectService.create(value131Data, username)

        val value132Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Scalar(ScalarValue.StringValue("0.3"), null, null),
            savedObjectProperty.id.toString(),
            chargeModeProperty(aspectStage2.id),
            savedValue13.id.toString(),
            ampereMeasure?.id
        )
        val savedValue132 = objectService.create(value132Data, username)

        val value133Data = ObjectPropertyValueData(
            null,
            ObjectValueData.Scalar(ScalarValue.IntegerValue(45), null, null),
            savedObjectProperty.id.toString(),
            chargeModeProperty(aspectMaxTempr.id),
            savedValue13.id.toString(),
            null
        )
        val savedValue133 = objectService.create(value133Data, username)

        if (savedObject.id == null) {
            fail("id of saved object is null")
        }

        val savedObjectId: String? = savedObject.id?.toString()

        if (savedObjectId != null) {
            val foundObject = objectService.findById(savedObjectId)

            assertEquals(foundObject.name, objectData.name, "name is incorrect")
            transaction(db) {
                assertEquals(1, foundObject.properties.size, "1 property is expected")
            }

            val foundObjectProperty = transaction(db) {
                foundObject.properties.first()
            }

            transaction(db) {
                assertEquals(9, foundObjectProperty.values.size, "9 properties are expected")

                foundObjectProperty.values.forEach {
                    val property = it.objectProperty
                    if (property == null) {
                        fail("object property is null for value with id ${it.id}")
                    } else {
                        assertEquals(foundObjectProperty.id, property.id, "")
                    }
                }
            }

            assertEquals(objectPropertyData.name, foundObjectProperty.name, "name is unexpecxted")
        }
    }
}
