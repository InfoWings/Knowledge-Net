package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.set
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.OVertex
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(SpringExtension::class)
@SpringBootTest
class AspectServicePropertyTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectDaoService: AspectDaoService
    @Autowired
    lateinit var aspectService: AspectService
    @Autowired
    lateinit var orientDatabase: OrientDatabase

    lateinit var complexAspect: AspectData
    lateinit var baseAspect: AspectData

    /**
     * complexAspect
     *     -> property
     *             -> baseAspect
     */
    @BeforeEach
    fun addAspectWithProperty() {
        val ad = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, emptyList())
        baseAspect = aspectService.save(ad, username)

        val property = AspectPropertyData("", "p", baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData(
            id = "",
            name = randomName(),
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(property)
        )
        complexAspect = aspectService.save(ad2, username)
    }

    @Test
    fun testNotVirtualPropertyId() {
        assertTrue("Property Ids are not virtual",
            aspectService.getAspects().flatMap { it.properties }.all { !it.id.contains("-") }
        )
    }

    @Test
    fun testWithAspectField() {
        assertEquals("p , ${baseAspect.name}", aspectDaoService.findPropertyStrict(complexAspect.properties[0].id).nameWithAspect)
    }

    @Test
    fun testAspectWithProperties() {
        val loaded = aspectService.findById(complexAspect.idStrict())

        assertThat("aspect linked with aspect property should be saved", loaded, Is(complexAspect))

        val all = aspectService.getAspects()
        assertTrue("there should be at least 2 aspects in db", all.size > 2)
    }

    @Test
    fun testAspectSortByNameAsc() {
        // .toLowerCase() is important here because java sorting differs from orient
        val all: List<AspectData> = aspectService.getAspects(listOf(AspectOrderBy(AspectSortField.NAME, Direction.ASC)))
        assertThat("Aspects should be sorted be Name ascending", all.map { it.name.toLowerCase() }, Is(all.map { it.name.toLowerCase() }.sorted()))
    }

    @Test
    fun testAspectSortByNameDesc() {
        // .toLowerCase() is important here because java sorting differs from orient
        val all = aspectService.getAspects(listOf(AspectOrderBy(AspectSortField.NAME, Direction.DESC)))
        assertThat("Aspects should be sorted be Name descending", all.map { it.name.toLowerCase() }, Is(all.map { it.name.toLowerCase() }.sortedDescending()))
    }


    @Test
    fun testAddAspectPropertiesToAspect() {
        val propertyData = AspectPropertyData("", "p2", baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val dataForUpdate = complexAspect.copy(properties = complexAspect.properties.plus(propertyData))
        val updatedAspect = aspectService.save(dataForUpdate, username)

        assertThat("aspect should have 2 properties", updatedAspect.properties.size, Is(2))
    }

    @Test
    fun testCreateAspectWithTwoPropertiesDifferentNames() {
        val property = AspectPropertyData("", "p", complexAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val property2 = AspectPropertyData("", "p2", complexAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData(
            id = "",
            name = "testCreateAspectWithTwoPropertiesDifferentNames",
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(property, property2)
        )
        val loaded = aspectService.save(ad2, username)
        val loadedId = loaded.id ?: throw IllegalArgumentException("No id for aspect")

        assertThat("aspect properties should be saved", aspectService.findById(loadedId), Is(loaded))

        assertThat(
            "aspect should have correct properties",
            loaded.properties.map { it.name },
            Is(listOf(property, property2).map { it.name })
        )
    }

    @Test
    fun testCreateAspectWithTwoPropertiesSameNamesSameAspect() {
        val property = AspectPropertyData("", "p", complexAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val property2 = AspectPropertyData("", "p", complexAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData(
            id = "",
            name = "testCreateAspectWithTwoPropertiesSameNamesSameAspect",
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(property, property2)
        )
        assertThrows<AspectInconsistentStateException> {
            aspectService.save(ad2, username)
        }
    }

    @Test
    fun testCreateAspectWithTwoPropertiesSameNamesDifferentAspect() {
        val property = AspectPropertyData("", "p", complexAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val property2 = AspectPropertyData("", "p", baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData(
            id = "",
            name = "testCreateAspectWithTwoPropertiesSameNamesDifferentAspect",
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(property, property2)
        )
        val saved = aspectService.save(ad2, username)

        assertThat(
            "aspect should have two properties with same name",
            saved.properties.count { it.name == "p" },
            Is(2)
        )
    }

    @Test
    fun testCorrectChangeAspectPropertyName() {
        val propertyList = complexAspect.properties.toMutableList()
        propertyList[0] = propertyList[0].copy(name = "new Name")
        val dataForUpdate = complexAspect.copy(properties = propertyList)

        val updated = aspectService.save(dataForUpdate, username)

        assertTrue("aspect property should have new name", updated.properties.map { it.name }.any { it == "new Name" })
    }

    @Test
    fun testUnCorrectChangeAspectPropertyName() {

        val propertyList = complexAspect.properties.toMutableList()
        propertyList.add(propertyList[0].copy(id = "", name = "new Name"))
        val dataForUpdate = complexAspect.copy(properties = propertyList)

        val saved = aspectService.save(dataForUpdate, username)

        var propertyList2 = saved.properties.toMutableList()
        propertyList2 = propertyList2.map { if (it.name == "new Name") it.copy(name = "p") else it }.toMutableList()
        val dataForUpdate2 = saved.copy(properties = propertyList2)

        assertThrows<AspectInconsistentStateException> { aspectService.save(dataForUpdate2, username) }
    }

    @Test
    fun testChangePowerAspectProperty() {
        val propertyList = complexAspect.properties.toMutableList()
        propertyList[0] = propertyList[0].copy(cardinality = PropertyCardinality.ONE.name)
        val dataForUpdate = complexAspect.copy(properties = propertyList)

        val saved = aspectService.save(dataForUpdate, username)

        assertThat(
            "aspect property should have new cardinality",
            aspectService.findById(saved.idStrict()).properties.find { it.name == propertyList[0].name }?.cardinality,
            Is(propertyList[0].cardinality)
        )
    }

    @Test
    fun testChangeAspectForAspectProperty() {
        val ad = AspectData("", "new", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: AspectData = aspectService.save(ad, username)
        val createAspectId = createAspect.id ?: throw IllegalArgumentException("no id for aspect")

        val propertyList = complexAspect.properties.toMutableList()
        propertyList[0] = propertyList[0].copy(aspectId = createAspectId)
        val dataForUpdate = complexAspect.copy(properties = propertyList)

        val saved = aspectService.save(dataForUpdate, username)

        assertThat(
            "aspect property should have new linked aspect",
            aspectService.findById(saved.properties[0].aspectId),
            Is(createAspect.copy(version = createAspect.version + 1))
        )
    }

    @Test
    fun testPropertyOldVersion() {
        transaction(orientDatabase) {
            val property = complexAspect.properties[0]
            val vertex = orientDatabase.getVertexById(property.id)!!
            vertex["name"] = "name"
            vertex.save<OVertex>()
        }
        assertThrows<AspectConcurrentModificationException> { aspectService.save(complexAspect, username) }
    }
}


