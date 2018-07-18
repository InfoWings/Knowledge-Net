package com.infowings.catalog.data.aspect.update

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectModificationException
import com.infowings.catalog.data.aspect.AspectPropertyModificationException
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

    private lateinit var aspectLinkedOtherAspect: AspectData
    private lateinit var aspectWithObjectProperty: AspectData
    private lateinit var aspectWithValue: AspectData
    private lateinit var objectPropertyId: String

    @Before
    fun init() {
        initAspectLinkedOtherAspect()
        initAspectWithObjectProperty()
    }

    @Test
    fun testChangeBaseTypeLinkedByAspect() {
        val aspect = aspectService.save(aspectLinkedOtherAspect.copy(measure = null, baseType = BaseType.Text.name), username)

        Assert.assertEquals("aspect should change base type", aspect.baseType, BaseType.Text.name)
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeBaseTypeHasValue() {
        createValue()
        aspectService.save(aspectWithObjectProperty.copy(measure = null, baseType = BaseType.Text.name), username)
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeBaseTypePropHasValue() {
        aspectService.save(aspectWithValue.copy(measure = null, baseType = BaseType.Text.name), username)
    }

    @Test
    fun testChangeAspectMeasureOtherGroupLinkedByAspect() {

        val newAspect = aspectService.save(aspectLinkedOtherAspect.copy(measure = Litre.name), username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Litre.name)

        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal.name)
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeAspectMeasureOtherGroupPropHasValue() {
        aspectService.save(aspectWithValue.copy(measure = Litre.name), username)
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeAspectMeasureOtherGroupHasValue() {
        createValue()
        aspectService.save(aspectWithObjectProperty.copy(measure = Litre.name), username)
    }

    @Test
    fun testChangeAspectMeasureOtherGroupLinkedNoValue() {
        val res = aspectService.save(aspectWithObjectProperty.copy(measure = Litre.name), username)
        Assert.assertEquals("aspect change measure", res.measure, Litre.name)
    }

    @Test(expected = AspectPropertyModificationException::class)
    fun testEditPropertyHasValue() {
        val otherAspect = aspectService.save(AspectData(name = "other", measure = Metre.name), username)
        val newProperty = aspectWithObjectProperty.properties[0].copy(aspectId = otherAspect.idStrict())
        aspectService.save(aspectWithObjectProperty.copy(properties = listOf(newProperty, aspectWithObjectProperty.properties[1])), username)
    }

    @Test
    fun testEditOtherPropertyHasValue() {
        val otherAspect = aspectService.save(AspectData(name = "other", measure = Metre.name), username)
        val newProperty = aspectWithObjectProperty.properties[1].copy(aspectId = otherAspect.idStrict())
        val edited = aspectService.save(aspectWithObjectProperty.copy(properties = listOf(newProperty, aspectWithObjectProperty.properties[0])), username)

        Assert.assertEquals("property has new aspectId", otherAspect.id, edited.properties[1].aspectId)
    }

    @Test
    fun testRemovePropertyHasValue() {
        val withValueProp = aspectWithObjectProperty.properties[0]
        val simpleProp = aspectWithObjectProperty.properties[1]
        val saved = aspectService.save(
            aspectWithObjectProperty.copy(properties = listOf(withValueProp.copy(deleted = true), simpleProp.copy(deleted = true))),
            username
        )

        Assert.assertEquals("with value prop exist", withValueProp.id, saved.properties[0].id)
        Assert.assertEquals("with value prop has deleted flag", true, saved.properties[0].deleted)
        Assert.assertTrue("simple prop not exist", saved.properties.none { it.id == simpleProp.id })
    }

    private fun initAspectWithObjectProperty() {
        val ad2 = AspectData("", "aspectWithObjectProperty", Second.name, null, BaseType.Decimal.name, emptyList())
        aspectWithValue = aspectService.save(ad2, username)
        val ap2 = AspectPropertyData(name = "ap1", cardinality = PropertyCardinality.ONE.name, aspectId = aspectWithValue.id!!, id = "", description = "")
        val ap3 = AspectPropertyData(name = "ap2", cardinality = PropertyCardinality.ONE.name, aspectId = aspectWithValue.id!!, id = "", description = "")
        val ad3 = AspectData("", "parent", Kilometre.name, null, BaseType.Decimal.name, listOf(ap2, ap3))
        aspectWithObjectProperty = aspectService.save(ad3, username)

        val subject = subjectService.createSubject(SubjectData(name = "subject", description = null), username)
        val obj = objectService.create(ObjectCreateRequest("obj", null, subject.id, subject.version), username)
        objectPropertyId = objectService.create(PropertyCreateRequest(obj, "prop", null, aspectWithObjectProperty.id!!), username)
        createValue(aspectWithObjectProperty.properties[0].id, 124)

        aspectWithObjectProperty = aspectService.findById(aspectWithObjectProperty.id!!)
        aspectWithValue = aspectService.findById(aspectWithValue.id!!)
    }

    private fun initAspectLinkedOtherAspect() {
        val ad0 = AspectData("", "leaf", Metre.name, null, BaseType.Decimal.name, emptyList())
        aspectLinkedOtherAspect = aspectService.save(ad0, username)
        val ap = AspectPropertyData(name = "ad", cardinality = PropertyCardinality.ONE.name, aspectId = aspectLinkedOtherAspect.id!!, id = "", description = "")
        val ad1 = AspectData("", "aspectLinkedOtherAspect", Kilometre.name, null, BaseType.Decimal.name, listOf(ap))
        aspectService.save(ad1, username)
        aspectLinkedOtherAspect = aspectService.findById(aspectLinkedOtherAspect.id!!)
    }

    private fun createValue(aspectPropertyId: String? = null, value: Int = 123) {
        val objPropertyValueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(value, null),
            objectPropertyId = objectPropertyId,
            aspectPropertyId = aspectPropertyId,
            measureId = null,
            parentValueId = null
        )
        objectService.create(objPropertyValueRequest, username)
    }
}