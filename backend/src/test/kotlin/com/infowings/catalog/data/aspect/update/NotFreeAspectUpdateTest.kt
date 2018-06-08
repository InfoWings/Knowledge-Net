package com.infowings.catalog.data.aspect.update

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectModificationException
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.objekt.ObjectService
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotFreeAspectUpdateTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService
    @Autowired
    lateinit var objectService: ObjectService
    @Autowired
    lateinit var subjectService: SubjectService

    lateinit var aspectLinkedOtherAspect: AspectData
    lateinit var aspectWithObjectProperty: AspectData

    @Before
    fun init() {
        val ad0 = AspectData("", "leaf", Metre.name, null, BaseType.Decimal.name, emptyList())
        aspectLinkedOtherAspect = aspectService.save(ad0, username).toAspectData()
        val ap = AspectPropertyData(name = "ad", cardinality = PropertyCardinality.ONE.name, aspectId = aspectLinkedOtherAspect.id!!, id = "", description = "")
        val ad1 = AspectData("", "aspectLinkedOtherAspect", Kilometre.name, null, BaseType.Decimal.name, listOf(ap))
        aspectService.save(ad1, username).toAspectData()

        val ad2 = AspectData("", "leaf2", Second.name, null, BaseType.Decimal.name, emptyList())
        val leafAspect = aspectService.save(ad2, username).toAspectData()
        val ap2 = AspectPropertyData(name = "ad", cardinality = PropertyCardinality.ONE.name, aspectId = leafAspect.id!!, id = "", description = "")
        val ad3 = AspectData("", "aspectWithObjectProperty", Kilometre.name, null, BaseType.Decimal.name, listOf(ap2))
        aspectWithObjectProperty = aspectService.save(ad3, username).toAspectData()

        val subject = subjectService.createSubject(SubjectData(name = "subject", description = null), username)
        val obj = objectService.create(ObjectCreateRequest("obj", null, subject.id, subject.version), username)
        val objProperty = objectService.create(PropertyCreateRequest(obj, "prop", PropertyCardinality.ONE.name, aspectWithObjectProperty.id!!), username)
        val objPropertyValueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(123, null),
            objectPropertyId = objProperty,
            aspectPropertyId = aspectWithObjectProperty.properties[0].id,
            measureId = null,
            parentValueId = null
        )
        objectService.create(objPropertyValueRequest, username)
    }

    @Test
    fun testChangeBaseTypeLinkedByAspect() {
        val aspect = aspectService.save(
            aspectLinkedOtherAspect.copy(measure = null, baseType = BaseType.Text.name, version = aspectLinkedOtherAspect.version + 1),
            username
        )

        Assert.assertEquals("aspect should change base type", aspect.baseType, BaseType.Text)
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeBaseTypeHasValue() {
        val newAspect = aspectService.save(
            aspectWithObjectProperty.copy(
                measure = null,
                baseType = BaseType.Text.name,
                version = aspectWithObjectProperty.version + 1,
                properties = aspectWithObjectProperty.properties.map { it.copy(version = it.version + 1) }
            ),
            username
        )
        Assert.assertEquals("aspect should have net base type", newAspect.baseType, BaseType.Text.name)
    }

    @Test
    fun testChangeAspectMeasureOtherGroupLinkedByAspect() {

        val newAspect = aspectService.save(
            aspectLinkedOtherAspect.copy(
                measure = Litre.name,
                version = aspectLinkedOtherAspect.version + 1,
                properties = aspectLinkedOtherAspect.properties.map { it.copy(version = it.version + 1) }), username
        )

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Litre)

        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeAspectMeasureOtherGroupHasValue() {
        aspectService.save(
            aspectWithObjectProperty.copy(
                measure = Litre.name,
                version = aspectWithObjectProperty.version + 1,
                properties = aspectWithObjectProperty.properties.map { it.copy(version = it.version + 1) }), username
        )
    }
}