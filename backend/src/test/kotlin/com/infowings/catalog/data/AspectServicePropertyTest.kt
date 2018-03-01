package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Kilometre
import org.hamcrest.core.Is
import org.junit.Assert.assertThat
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
class AspectServicePropertyTest {

    @Autowired
    lateinit var aspectService: AspectService

    lateinit var complexAspect: Aspect
    lateinit var baseAspect: Aspect

    @Before
    fun addAspectWithProperty() {
        val ad = AspectData("", "base", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        baseAspect = aspectService.createAspect(ad)

        val property = AspectPropertyData("", "p", baseAspect.id, AspectPropertyPower.INFINITY.name)

        val ad2 = AspectData("", "complex", Kilometre.name, null, BaseType.Decimal.name, listOf(property))
        complexAspect = aspectService.createAspect(ad2)
    }

    @Test
    fun testAspectWithProperties() {
        val loaded = aspectService.findById(complexAspect.id)

        assertThat("aspect property should be saved and restored", loaded, Is.`is`(complexAspect))

        val all = aspectService.getAspects()
        assertThat("There should be 2 aspects in db", all.size, Is.`is`(2))
    }

    @Test
    fun testAddAspectPropertiesToAspect() {
        val propertyData = AspectPropertyData("", "p2", baseAspect.id, AspectPropertyPower.INFINITY.name)
        val updatedAspect = aspectService.addProperty(complexAspect.id, propertyData)

        assertThat("aspect should have 2 properties", updatedAspect.properties.size, Is.`is`(2))
    }

    @Test
    fun testCreateAspectWithTwoPropertiesDifferentNames() {
        val property = AspectPropertyData("", "p", complexAspect.id, AspectPropertyPower.INFINITY.name)
        val property2 = AspectPropertyData("", "p2", complexAspect.id, AspectPropertyPower.INFINITY.name)

        val ad2 = AspectData("", "new", Kilometre.name, null, BaseType.Decimal.name, listOf(property, property2))
        val loaded = aspectService.createAspect(ad2)

        assertThat("aspect properties should be saved and restored", aspectService.findById(loaded.id), Is.`is`(loaded))

        assertThat("aspect should have corresponding properties",
                loaded.properties.map { it.name },
                Is.`is`(listOf(property, property2).map { it.name }))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateAspectWithTwoPropertiesSameNames() {
        val property = AspectPropertyData("", "p", complexAspect.id, AspectPropertyPower.INFINITY.name)
        val property2 = AspectPropertyData("", "p", complexAspect.id, AspectPropertyPower.INFINITY.name)

        val ad2 = AspectData("", "new", Kilometre.name, null, BaseType.Decimal.name, listOf(property, property2))
        aspectService.createAspect(ad2)
    }

    @Test
    fun testCorrectChangeAspectPropertyName() {
        val propertyId = complexAspect.properties[0].id
        val updatedProperty = aspectService.changePropertyName(propertyId, "new Name")

        assertThat("returned from changePropertyName property should be the same as found by loadAspectProperty",
                aspectService.loadAspectProperty(propertyId), Is.`is`(updatedProperty))

        assertThat("aspect property should have new name", updatedProperty.name, Is.`is`("new Name"))
    }

    @Test(expected = AspectPropertyModificationException::class)
    fun testUnCorrectChangeAspectPropertyName() {
        val propertyId = complexAspect.properties[0].id

        val propertyData = AspectPropertyData("", "p2", baseAspect.id, AspectPropertyPower.INFINITY.name)
        aspectService.addProperty(complexAspect.id, propertyData)

        aspectService.changePropertyName(propertyId, propertyData.name)
    }

    @Test
    fun testChangePowerAspectProperty() {
        val property = complexAspect.properties[0]
        val updatedProperty = aspectService.changePropertyPower(property.id, AspectPropertyPower.ONE)

        assertThat("returned from changePropertyPower property should be the same as found by loadAspectProperty",
                aspectService.loadAspectProperty(property.id), Is.`is`(updatedProperty))

        assertThat("aspect property should have new power", updatedProperty.power, Is.`is`(AspectPropertyPower.ONE))
    }

    // todo: tests for cycle detection
    // todo: in case no values for aspect
    @Test
    fun testChangeAspectForAspectProperty() {
        val ad = AspectData("", "new", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.createAspect(ad)

        val property = complexAspect.properties[0]
        val updatedProperty = aspectService.changePropertyAspect(property.id, createAspect.id)

        assertThat("returned from changePropertyAspect property should be the same as found by loadAspectProperty",
                aspectService.loadAspectProperty(property.id), Is.`is`(updatedProperty))

        assertThat("aspect property should have new linked aspect", updatedProperty.aspect, Is.`is`(createAspect))
    }
}


