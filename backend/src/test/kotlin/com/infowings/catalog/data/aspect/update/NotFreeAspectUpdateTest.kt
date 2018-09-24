package com.infowings.catalog.data.aspect.update

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectModificationException
import com.infowings.catalog.data.aspect.AspectPropertyModificationException
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.randomName
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("UnsafeCallOnNullableType")
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
    private lateinit var propertyCreateResponse: PropertyCreateResponse

    @BeforeEach
    fun init() {
        initAspectLinkedOtherAspect()
        initAspectWithObjectProperty()
    }

    @Test
    fun testChangeBaseTypeLinkedByAspect() {
        val aspect = aspectService.save(aspectLinkedOtherAspect.copy(measure = null, baseType = BaseType.Text.name), username)

        Assert.assertEquals("aspect should change base type", aspect.baseType, BaseType.Text.name)
    }

    @Test
    fun testChangeBaseTypeHasValue() {
        createRootValue(125, Kilometre.name)
        assertThrows<AspectModificationException> {
            aspectService.save(aspectWithObjectProperty.copy(measure = null, baseType = BaseType.Text.name), username)
        }
    }

    @Test
    fun testChangeBaseTypePropHasValue() {
        assertThrows<AspectModificationException> {
            aspectService.save(aspectWithValue.copy(measure = null, baseType = BaseType.Text.name), username)
        }
    }

    @Test
    fun testChangeAspectMeasureOtherGroupLinkedByAspect() {

        val newAspect = aspectService.save(aspectLinkedOtherAspect.copy(measure = Litre.name), username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Litre.name)
        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal.name)
    }

    @Test
    fun testChangeAspectMeasureOtherGroupPropHasValue() {
        assertThrows<AspectModificationException> {
            aspectService.save(aspectWithValue.copy(measure = Litre.name), username)
        }
    }

    @Test
    fun testChangeAspectMeasureOtherGroupHasValue() {
        createRootValue(125, Kilometre.name)
        assertThrows<AspectModificationException> {
            aspectService.save(aspectWithObjectProperty.copy(measure = Litre.name), username)
        }
    }

    @Test
    fun testChangeAspectMeasureOtherGroupLinkedNoValue() {
        val res = aspectService.save(aspectWithObjectProperty.copy(measure = Litre.name), username)
        Assert.assertEquals("aspect change measure", res.measure, Litre.name)
    }

    @Test
    fun testEditPropertyHasValue() {
        val otherAspect = aspectService.save(AspectData(name = "testEditPropertyHasValue-other", measure = Metre.name), username)
        val newProperty = aspectWithObjectProperty.properties[0].copy(aspectId = otherAspect.idStrict())
        assertThrows<AspectPropertyModificationException> {
            aspectService.save(aspectWithObjectProperty.copy(properties = listOf(newProperty, aspectWithObjectProperty.properties[1])), username)
        }
    }

    @Test
    fun testEditOtherPropertyHasValue() {
        val otherAspect = aspectService.save(AspectData(name = "testEditOtherPropertyHasValue-other", measure = Metre.name), username)
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
        val ad2 = AspectData("", randomName(), Second.name, null, BaseType.Decimal.name, emptyList())
        aspectWithValue = aspectService.save(ad2, username)
        val ap2 = AspectPropertyData(name = "ap1", cardinality = PropertyCardinality.ONE.name, aspectId = aspectWithValue.id!!, id = "", description = "")
        val ap3 = AspectPropertyData(name = "ap2", cardinality = PropertyCardinality.ONE.name, aspectId = aspectWithValue.id!!, id = "", description = "")
        val ad3 = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, listOf(ap2, ap3))
        aspectWithObjectProperty = aspectService.save(ad3, username)

        val subject = subjectService.createSubject(SubjectData(name = randomName(), description = null), username)
        val objCreateResponse = objectService.create(ObjectCreateRequest(randomName(), null, subject.id), username)
        propertyCreateResponse = objectService.create(PropertyCreateRequest(objCreateResponse.id, "prop", null, aspectWithObjectProperty.id!!), username)
        val rootValue = createNullRootValue()
        createChildValue(aspectPropertyId = aspectWithObjectProperty.properties[0].id, parentId = rootValue.id)

        aspectWithObjectProperty = aspectService.findById(aspectWithObjectProperty.id!!)
        aspectWithValue = aspectService.findById(aspectWithValue.id!!)
    }

    private fun initAspectLinkedOtherAspect() {
        val ad0 = AspectData("", randomName(), Metre.name, null, BaseType.Decimal.name, emptyList())
        aspectLinkedOtherAspect = aspectService.save(ad0, username)
        val ap = AspectPropertyData(name = "ad", cardinality = PropertyCardinality.ONE.name, aspectId = aspectLinkedOtherAspect.id!!, id = "", description = "")
        val ad1 = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, listOf(ap))
        aspectService.save(ad1, username)
        aspectLinkedOtherAspect = aspectService.findById(aspectLinkedOtherAspect.id!!)
    }

    private fun createRootValue(value: Int, measureName: String?): ValueChangeResponse {
        val objPropertyValueRequest = ValueCreateRequest(ObjectValueData.DecimalValue.single(value.toString()), null, propertyCreateResponse.id, measureName)
        return objectService.create(objPropertyValueRequest, username)
    }

    private fun createNullRootValue(): ValueChangeResponse {
        val objPropertyValueRequest = ValueCreateRequest(ObjectValueData.NullValue, null, propertyCreateResponse.id)
        return objectService.create(objPropertyValueRequest, username)
    }

    private fun createChildValue(aspectPropertyId: String, parentId: String, value: Int = 124) {
        val objPropertyValueRequest = ValueCreateRequest(
            value = ObjectValueData.DecimalValue.single(value.toString()),
            description = null, objectPropertyId = propertyCreateResponse.id,
            aspectPropertyId = aspectPropertyId,
            measureName = Second.name,
            parentValueId = parentId
        )
        objectService.create(objPropertyValueRequest, username)
    }
}