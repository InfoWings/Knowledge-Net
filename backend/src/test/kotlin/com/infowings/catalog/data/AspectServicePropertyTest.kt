package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Kilometre
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.set
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.OVertex
import org.hamcrest.core.Is
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
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
    @Autowired
    lateinit var orientDatabase: OrientDatabase

    lateinit var complexAspect: Aspect
    lateinit var baseAspect: Aspect

    @Before
    fun addAspectWithProperty() {
        val ad = AspectData("", "base", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        baseAspect = aspectService.save(ad)

        val property = AspectPropertyData("", "p", baseAspect.id, AspectPropertyCardinality.INFINITY.name)

        val ad2 = AspectData("", "complex", Kilometre.name, null, BaseType.Decimal.name, listOf(property))
        complexAspect = aspectService.save(ad2)
    }

    @Test
    fun testNotVirtualPropertyId() {
        assertThat("Property Ids are not virtual",
                aspectService.getAspects().flatMap { it.properties }.all { !it.id.contains("-") },
                Is.`is`(true))
    }

    @Test
    fun testAspectWithProperties() {
        val loaded = aspectService.findById(complexAspect.id)

        assertThat("aspect linked with aspect property should be saved", loaded, Is.`is`(complexAspect))

        val all = aspectService.getAspects()
        assertThat("there should be 2 aspects in db", all.size, Is.`is`(2))
    }

    @Test
    fun testAddAspectPropertiesToAspect() {
        val propertyData = AspectPropertyData("", "p2", baseAspect.id, AspectPropertyCardinality.INFINITY.name)
        val dataForUpdate = complexAspect.toAspectData().copy(properties = complexAspect.toAspectData().properties.plus(propertyData))
        val updatedAspect = aspectService.save(dataForUpdate)

        assertThat("aspect should have 2 properties", updatedAspect.properties.size, Is.`is`(2))
    }

    @Test
    fun testCreateAspectWithTwoPropertiesDifferentNames() {
        val property = AspectPropertyData("", "p", complexAspect.id, AspectPropertyCardinality.INFINITY.name)
        val property2 = AspectPropertyData("", "p2", complexAspect.id, AspectPropertyCardinality.INFINITY.name)

        val ad2 = AspectData("", "new", Kilometre.name, null, BaseType.Decimal.name, listOf(property, property2))
        val loaded = aspectService.save(ad2)

        assertThat("aspect properties should be saved", aspectService.findById(loaded.id), Is.`is`(loaded))

        assertThat("aspect should have correct properties",
                loaded.properties.map { it.name },
                Is.`is`(listOf(property, property2).map { it.name }))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateAspectWithTwoPropertiesSameNamesSameAspect() {
        val property = AspectPropertyData("", "p", complexAspect.id, AspectPropertyCardinality.INFINITY.name)
        val property2 = AspectPropertyData("", "p", complexAspect.id, AspectPropertyCardinality.INFINITY.name)

        val ad2 = AspectData("", "new", Kilometre.name, null, BaseType.Decimal.name, listOf(property, property2))
        aspectService.save(ad2)
    }

    @Test
    fun testCreateAspectWithTwoPropertiesSameNamesDifferentAspect() {
        val property = AspectPropertyData("", "p", complexAspect.id, AspectPropertyCardinality.INFINITY.name)
        val property2 = AspectPropertyData("", "p", baseAspect.id, AspectPropertyCardinality.INFINITY.name)

        val ad2 = AspectData("", "new", Kilometre.name, null, BaseType.Decimal.name, listOf(property, property2))
        val saved = aspectService.save(ad2)

        assertThat("aspect should have two properties with same name", saved.properties.count { it.name == "p" }, Is.`is`(2))
    }

    @Test
    fun testCorrectChangeAspectPropertyName() {
        val propertyList = complexAspect.toAspectData().properties.toMutableList()
        propertyList[0] = propertyList[0].copy(name = "new Name")
        val dataForUpdate = complexAspect.toAspectData().copy(properties = propertyList)

        val updated = aspectService.save(dataForUpdate)

        assertTrue("aspect property should have new name", updated.properties.map { it.name }.any { it == "new Name" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnCorrectChangeAspectPropertyName() {

        val propertyList = complexAspect.toAspectData().properties.toMutableList()
        propertyList.add(propertyList[0].copy(id = "", name = "new Name"))
        val dataForUpdate = complexAspect.toAspectData().copy(properties = propertyList)

        val saved = aspectService.save(dataForUpdate)

        var propertyList2 = saved.toAspectData().properties.toMutableList()
        propertyList2 = propertyList2.map { if (it.name == "new Name") it.copy(name = "p") else it }.toMutableList()
        val dataForUpdate2 = saved.toAspectData().copy(properties = propertyList2)

        aspectService.save(dataForUpdate2)
    }

    @Test
    fun testChangePowerAspectProperty() {
        val propertyList = complexAspect.toAspectData().properties.toMutableList()
        propertyList[0] = propertyList[0].copy(cardinality = AspectPropertyCardinality.ONE.name)
        val dataForUpdate = complexAspect.toAspectData().copy(properties = propertyList)

        val saved = aspectService.save(dataForUpdate)

        assertThat(
            "aspect property should have new cardinality",
            aspectService.findById(saved.id).properties.find { it.name == propertyList[0].name }?.cardinality?.name,
            Is.`is`(propertyList[0].cardinality)
        )
    }

    @Test
    fun testChangeAspectForAspectProperty() {
        val ad = AspectData("", "new", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.save(ad)

        val propertyList = complexAspect.toAspectData().properties.toMutableList()
        propertyList[0] = propertyList[0].copy(aspectId = createAspect.id)
        val dataForUpdate = complexAspect.toAspectData().copy(properties = propertyList)

        val saved = aspectService.save(dataForUpdate)

        assertThat("aspect property should have new linked aspect",
                saved.properties[0].aspect,
                Is.`is`(createAspect.copy(version = createAspect.version + 1)))
    }

    @Test(expected = AspectModificationException::class)
    fun testPropertyOldVersion() {
        transaction(orientDatabase) {
            val property = complexAspect.properties[0]
            val vertex = orientDatabase.getVertexById(property.id)!!
            vertex["name"] = "name"
            vertex.save<OVertex>()
        }
        aspectService.save(complexAspect.toAspectData())
    }
}


