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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner


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
            AspectData(name = "current1", baseType = BaseType.Decimal.name), username)
        val aspectStage2 = aspectService.save(
            AspectData(name = "current2", baseType = BaseType.Decimal.name), username)
        val aspectMaxTempr = aspectService.save(
            AspectData(
                name = "temperature",
                description = "При нормальныхз условиях окружающей среды: T воздуха = 20 гр. C",
                baseType = BaseType.Decimal.name
            ), username)

        val propertyStage1 = AspectPropertyData("", "1-й ступени ток", aspectStage1.id, PropertyCardinality.ONE.name)
        val propertyStage2 = AspectPropertyData("", "2-й ступени ток", aspectStage2.id, PropertyCardinality.ONE.name)
        val propertyMaxTempr = AspectPropertyData("", "Max температура", aspectMaxTempr.id, PropertyCardinality.ONE.name)

        val aspectChargeMode = aspectService.save(
            AspectData(
                name = "charge",
                baseType = BaseType.Text.name,
                properties = listOf(propertyStage1, propertyStage2, propertyMaxTempr)
            ), username )

        val refBook = refBookService.createReferenceBook("rb-charge", aspectChargeMode.id, username)
        listOf("Ускоренный", "Номинальный", "Глубокий").forEach {
            val item = ReferenceBookItem(aspectChargeMode.id, refBook.root.id, "", it, emptyList(), false, 0)
            val addedItem = refBookService.addReferenceBookItem(item, "admin")
        }
        refBookService.getReferenceBook(aspectChargeMode.id)

        val subjectData = SubjectData(null, name = "ПАО \"Сатурн\"", description = "С 2005 года в ПАО \"Сатурн\"" +
                " ведутся работы....")
        val subjectSaturn = subjectService.createSubject(subjectData, username)

        println("subject: $subjectSaturn")

        println("RB-3: ${refBookService.getReferenceBookItem(refBook.root.id)}")

        val objectData = ObjectData(null, "ЛИГП-10", "some descr", subjectSaturn.id, emptyList())

        val savedObject = objectService.create(objectData, username)

        val objectPropertyData = ObjectPropertyData(null, "name", PropertyCardinality.INFINITY,
            savedObject.id.toString(), aspectChargeMode.id, emptyList())

        val savedObjectProperty = objectService.create(objectData, username)

        println("SAVED PROP: " + savedObjectProperty)
    }

}
