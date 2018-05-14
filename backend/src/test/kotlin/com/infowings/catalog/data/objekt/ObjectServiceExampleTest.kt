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
                name = "charge",
                baseType = BaseType.Text.name,
                properties = listOf(propertyStage1, propertyStage2, propertyMaxTempr)
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
            savedObject.id.toString(), aspectChargeMode.id, emptyList()
        )

        val savedObjectProperty = objectService.create(objectPropertyData, username)

        val refValue11Data = ReferenceValueData(ReferenceTypeGroup.DOMAIN_ELEMENT, refBookItemIds[0])
        val value11Data = ObjectPropertyValueData(
            null, null, null, null,
            savedObjectProperty.id.toString(), aspectChargeMode.id, null, refValue11Data, null
        )
        val savedValue11 = objectService.create(value11Data, username)

        val refValue12Data = ReferenceValueData(ReferenceTypeGroup.DOMAIN_ELEMENT, refBookItemIds[1])
        val value12Data = ObjectPropertyValueData(
            null, null, null, null,
            savedObjectProperty.id.toString(), aspectChargeMode.id, null, refValue12Data, null
        )
        val savedValue12 = objectService.create(value12Data, username)

        val refValue13Data = ReferenceValueData(ReferenceTypeGroup.DOMAIN_ELEMENT, refBookItemIds[2])
        val value13Data = ObjectPropertyValueData(
            null, null, null, null,
            savedObjectProperty.id.toString(), aspectChargeMode.id, null, refValue13Data, null
        )
        val savedValue13 = objectService.create(value13Data, username)

        val value111Data = ObjectPropertyValueData(
            null, ScalarValue.IntegerValue(3, "current"), null, null,
            savedObjectProperty.id.toString(), aspectChargeMode.id, savedValue11.id.toString(), null, null)
        val savedValue111 = objectService.create(value111Data, username)

        val value112Data = ObjectPropertyValueData(
            null, ScalarValue.IntegerValue(75, "temperature"), null, null,
            savedObjectProperty.id.toString(), aspectChargeMode.id, savedValue11.id.toString(), null, null)
        val savedValue112 = objectService.create(value112Data, username)


        val ampereMeasure = measureService.findMeasure("Ampere")

        val value121Data = ObjectPropertyValueData(
            null, ScalarValue.StringValue("0.8", "current") /* возможно, нужен еще тип с фиксированной точкой */,
            null, null, savedObjectProperty.id.toString(), aspectChargeMode.id, savedValue12.id.toString(),
            null, ampereMeasure?.id)
        val savedValue121 = objectService.create(value121Data, username)

        val value131Data = ObjectPropertyValueData(
            null, ScalarValue.StringValue("1.2", "current") /* возможно, нужен еще тип с фиксированной точкой */,
            null, null, savedObjectProperty.id.toString(), aspectChargeMode.id, savedValue13.id.toString(),
            null, ampereMeasure?.id)
        val savedValue131 = objectService.create(value131Data, username)

        val value132Data = ObjectPropertyValueData(
            null, ScalarValue.StringValue("0.3", "current") /* возможно, нужен еще тип с фиксированной точкой */,
            null, null, savedObjectProperty.id.toString(), aspectChargeMode.id, savedValue13.id.toString(),
            null, ampereMeasure?.id)
        val savedValue132 = objectService.create(value132Data, username)

        val value133Data = ObjectPropertyValueData(
            null, ScalarValue.IntegerValue(45, "current") /* возможно, нужен еще тип с фиксированной точкой */,
            null, null, savedObjectProperty.id.toString(), aspectChargeMode.id, savedValue13.id.toString(),
            null, ampereMeasure?.id)
        val savedValue133 = objectService.create(value133Data, username)


        if (savedObject.id == null) {
            fail("id of saved object is null")
        }

        val savedObjectId: String? = savedObject.id?.toString()

        if (savedObjectId != null) {
            val foundObject = objectService.findById(savedObjectId)

            assertEquals(foundObject.name, objectData.name, "name is incorrect")
        }

    }
}
