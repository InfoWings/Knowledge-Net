package com.infowings.catalog.data.aspect.update

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectEmptyChangeException
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertTrue
import org.hamcrest.core.Is.`is` as Is

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AspectUpdateTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    lateinit var aspect: AspectData

    private fun saveSimpleAspect(): AspectData {
        val ad = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, emptyList())
        return aspectService.save(ad, username).copy(measure = Metre.name)
    }

    private fun <T> checkTimestampUpdate(id: String, block: () -> T): T {
        val timestampBefore = aspectService.findById(id).lastChangeTimestamp!!
        Thread.sleep(1)
        val result = block()
        val timestampAfter = aspectService.findById(id).lastChangeTimestamp!!
        assertTrue(timestampAfter > timestampBefore, "Aspect timestamp should be updated after operation")
        return result
    }

    @Test
    fun `after aspect update its timestamp should be updated`() {
        val aspect = saveSimpleAspect().copy(measure = Metre.name)

        checkTimestampUpdate(aspect.id!!) { aspectService.save(aspect, username) }
    }

    @Test
    fun `after aspect property update its timestamp should be updated`() {
        val baseAspect = saveSimpleAspect()

        val property = AspectPropertyData("", "p", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData(
            id = "",
            name = randomName(),
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(property)
        )
        val complexAspect = aspectService.save(ad2, username)
        val propertyData = AspectPropertyData("", "p2", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)
        val dataForUpdate = complexAspect.copy(properties = complexAspect.properties + propertyData)

        checkTimestampUpdate(complexAspect.id!!) { aspectService.save(dataForUpdate, username) }
    }

    @Test
    fun `after aspect property delete its timestamp should be updated`() {
        val baseAspect = saveSimpleAspect()

        val property = AspectPropertyData("", "p", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData(
            id = "",
            name = randomName(),
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(property)
        )
        val complexAspect = aspectService.save(ad2, username)
        val aspectPropertyId = complexAspect.properties.first().id
        checkTimestampUpdate(complexAspect.id!!) { aspectService.removeProperty(aspectPropertyId, username, true) }
    }

    @Test
    fun `after aspect soft delete its timestamp should be updated`() {
        var aspect = saveSimpleAspect()
        val aspectId = aspect.idStrict()
        val p1 = AspectPropertyData("", "", aspectId, aspect.guidSoft(), PropertyCardinality.ONE.name, null)
        val ad = AspectData("", "testDeleteLinkedByAspect-aspectLinked", Metre.name, null, null, listOf(p1))
        aspectService.save(ad, username)

        aspect = aspectService.findById(aspectId)

        checkTimestampUpdate(aspect.id!!) { aspectService.remove(aspect, username, true) }
    }

    @Test
    fun testChangeAspectNameMeasure() {
        val ad = AspectData("", "testChangeAspectNameMeasure-aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username)

        val ad2 = AspectData(aspect.id, "testChangeAspectNameMeasure-new Aspect", Metre.name, null, BaseType.Decimal.name, emptyList(), 1)
        val newAspect = aspectService.save(ad2, username)

        assertThat("aspect should have new name", newAspect.name, Is("testChangeAspectNameMeasure-new Aspect"))
        assertThat("aspect should have new measure", newAspect.measure, Is(Metre.name))
    }

    @Test
    fun testChangeAspectMeasureSameGroup() {
        val ad = AspectData("", "testChangeAspectMeasureSameGroup-aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username).copy(measure = Metre.name)

        val newAspect = aspectService.save(aspect, username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Metre.name)

        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal.name)
    }

    @Test
    fun testMeasureBaseTypeManipulating() {
        val ad = AspectData("", "testMeasureBaseTypeManipulating-aspect", Litre.name, null, null, emptyList())
        val aspect = aspectService.save(ad, username)

        Assert.assertTrue("base type should be decimal", aspect.baseType == BaseType.Decimal.name)
        Assert.assertTrue("measure should be litre", aspect.measure == Litre.name)

        val ad2 = AspectData(aspect.id, "testMeasureBaseTypeManipulating-aspect", null, null, BaseType.Boolean.name, emptyList(), aspect.version)
        val aspect2 = aspectService.save(ad2, username)

        Assert.assertTrue("base type should be boolean", aspect2.baseType == BaseType.Boolean.name)
        Assert.assertTrue("measure should be null", aspect2.measure == null)

        val ad3 = AspectData(
            aspect.id,
            "aspect",
            Metre.name,
            null,
            BaseType.Decimal.name,
            emptyList(),
            aspect2.version
        )
        val aspect3 = aspectService.save(ad3, username)

        Assert.assertTrue("base type should be decimal", aspect3.baseType == BaseType.Decimal.name)
        Assert.assertTrue("measure should be metre", aspect3.measure == Metre.name)
    }

    @Test
    fun testChangeAspectMeasureOtherGroupOtherBaseType() {
        val ad = AspectData("", "testChangeAspectMeasureOtherGroupOtherBaseType-aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username)

        // We have not measures with different from Decimal BaseType. So, little hack
        val field = Gram::class.java.getDeclaredField("baseType")
        field.isAccessible = true
        field.set(Gram, BaseType.Boolean)

        val newAspect = aspectService.save(aspect.copy(measure = Gram.name, baseType = Gram.baseType.name), username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Gram.name)

        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Boolean.name)

        field.set(Gram, BaseType.Decimal)
    }

    @Test
    fun testUpdateSameData() {
        val ad = AspectData("", "testUpdateSameData-aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad, username)

        val ad2 = aspectService.getAspects().first()
        try {
            aspectService.save(ad2, username)
        } catch (e: AspectEmptyChangeException) {
        }
        val newAspect = aspectService.findById(ad2.id!!)
        Assert.assertEquals("Same data shouldn't be rewritten", ad2.version, newAspect.version)
    }
}