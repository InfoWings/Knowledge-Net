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

    @Before
    fun addAspectWithProperty() {
        val ad = AspectData("", "base", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.createAspect(ad)

        val property = AspectPropertyData("", "p", createAspect.id, AspectPropertyPower.INFINITY.name)

        val ad2 = AspectData("", "complex", Kilometre.name, null, BaseType.Decimal.name, listOf(property))
        complexAspect = aspectService.createAspect(ad2)
    }

    @Test
    fun testAddAspectProperties() {
        val loaded = aspectService.findById(complexAspect.id)

        assertThat("aspect property should be saved and restored", loaded, Is.`is`(complexAspect))

        val all = aspectService.getAspects()
        assertThat("There should be 2 aspects in db", all.size, Is.`is`(2))
    }

    @Test
    fun testChangeAspectPropertyName() {
        val property = complexAspect.properties[0]
        val updatedProperty = aspectService.changePropertyName(complexAspect.properties[0].id, "new Name")

        assertThat("returned from changePropertyName property should be the same as found by loadAspectProperty",
                aspectService.loadAspectProperty(property.id), Is.`is`(updatedProperty))

        assertThat("aspect property should have new name", updatedProperty.name, Is.`is`("new Name"))
    }

    @Test
    fun testChangePowerAspectProperty() {
        val property = complexAspect.properties[0]
        val updatedProperty = aspectService.changePropertyPower(complexAspect.properties[0].id, AspectPropertyPower.ONE)

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
        val updatedProperty = aspectService.changePropertyAspect(complexAspect.properties[0].id, createAspect.id)

        assertThat("returned from changePropertyAspect property should be the same as found by loadAspectProperty",
                aspectService.loadAspectProperty(property.id), Is.`is`(updatedProperty))

        assertThat("aspect property should have new linked aspect", updatedProperty.aspect, Is.`is`(createAspect))
    }
}


