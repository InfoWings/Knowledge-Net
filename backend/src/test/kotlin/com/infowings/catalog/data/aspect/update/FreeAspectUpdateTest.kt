package com.infowings.catalog.data.aspect.update

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FreeAspectUpdateTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    lateinit var aspect: AspectData

    @BeforeEach
    fun init() {
        val ad = AspectData("", randomName(), null, null, BaseType.Text.name, emptyList())
        aspect = aspectService.save(ad, username).copy(measure = Litre.name)
    }

    @Test
    fun testChangeBaseType() {
        val newAspect = aspectService.save(aspect.copy(baseType = BaseType.Decimal.name), username)

        Assert.assertEquals("aspect should have new base type", newAspect.baseType, BaseType.Decimal.name)
    }

    @Test
    fun testChangeAspectMeasureOtherGroup() {

        val newAspect = aspectService.save(aspect.copy(measure = Litre.name, baseType = null), username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Litre.name)

        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal.name)
    }

    @Test
    fun testEditProperty() {
        val pd = AspectPropertyData(name = "prop", aspectId = aspect.idStrict(), aspectGuid = aspect.guidSoft(), cardinality = PropertyCardinality.ONE.name, description = null, id = "")
        val ad2 = AspectData("", "testEditProperty-complex", Metre.name, null, BaseType.Decimal.name, listOf(pd))
        val complex = aspectService.save(ad2, username)

        val otherAspect = aspectService.save(AspectData(name = "testEditProperty-other", measure = Metre.name), username)
        val newProperty = complex.properties[0].copy(aspectId = otherAspect.idStrict())
        val edited = aspectService.save(complex.copy(properties = listOf(newProperty)), username)

        Assert.assertEquals("aspect property should change aspectId", edited.properties[0].aspectId, otherAspect.id)
    }
}