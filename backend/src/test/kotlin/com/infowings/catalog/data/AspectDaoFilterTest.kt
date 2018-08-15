package com.infowings.catalog.data

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
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

    @BeforeEach
    fun saveAspectTree() {
        var heightAspectData = AspectData(null, "Height", Metre.name, null, null)
        heightAspectData = aspectService.save(heightAspectData, username)
        var widthAspectData = AspectData(null, "Width", Metre.name, null, null)
        widthAspectData = aspectService.save(widthAspectData, username)
        val dimensionAspectData = AspectData(
            null, "Dimensions", null, null, BaseType.Text.name, properties = listOf(
                AspectPropertyData("", "", heightAspectData.id!!, PropertyCardinality.ONE.name, ""),
                AspectPropertyData("", "", widthAspectData.id!!, PropertyCardinality.ONE.name, "")
            )
        )
        aspectService.save(dimensionAspectData, username)
        val chargeAspectData = AspectData(null, "Charge", Ampere.name, null, null)
        aspectService.save(chargeAspectData, username)
    }

    @Test
    fun retrieveAspectsByQuery() {
        val foundAspects = aspectDao.findTransitiveByNameQuery("Hei")
        assertThat("Retrieved set contains 2 aspects", foundAspects.size, Is.`is`(2))
    }

}

