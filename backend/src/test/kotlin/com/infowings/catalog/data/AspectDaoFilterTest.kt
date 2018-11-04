package com.infowings.catalog.data

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
class AspectDaoFilterTest {

    val username = "admin"

    @Autowired
    lateinit var aspectDao: AspectDaoService

    @Autowired
    lateinit var aspectService: AspectService

    val heightAspectName = randomName("Height")

    @BeforeEach
    fun saveAspectTree() {
        var heightAspectData = AspectData(null, heightAspectName, Metre.name, null, null)
        heightAspectData = aspectService.save(heightAspectData, username)
        var widthAspectData = AspectData(null, randomName("Width"), Metre.name, null, null)
        widthAspectData = aspectService.save(widthAspectData, username)
        val dimensionAspectData = AspectData(
            null, randomName("Dimensions"), null, null, BaseType.Text.name, properties = listOf(
                AspectPropertyData("", "", heightAspectData.idStrict(), heightAspectData.guid ?: "???", PropertyCardinality.ONE.name, ""),
                AspectPropertyData("", "", widthAspectData.idStrict(), widthAspectData.guid ?: "???", PropertyCardinality.ONE.name, "")
            )
        )
        aspectService.save(dimensionAspectData, username)
        val chargeAspectData = AspectData(null, randomName("Charge"), Ampere.name, null, null)
        aspectService.save(chargeAspectData, username)
    }

    @Test
    @Suppress("MagicNumber")
    fun retrieveAspectsByQuery() {
        val foundAspects = aspectDao.findTransitiveByNameQuery(heightAspectName.take(10))
        assertThat("Retrieved set contains 2 aspects", foundAspects.size, Is.`is`(2))
    }

}

