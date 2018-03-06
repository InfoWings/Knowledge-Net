package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.loggerFor
import kotlinx.serialization.json.JSON
import org.hamcrest.core.Is
import org.hamcrest.core.IsNot
import org.hamcrest.text.IsEmptyString
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

private val logger = loggerFor<AspectServiceRestTest>()

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AspectServiceRestTest {

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    private val wac: WebApplicationContext? = null

    private lateinit var mockMvc: MockMvc

    private val authorities = SecurityMockMvcRequestPostProcessors.user("admin").authorities(SimpleGrantedAuthority("ADMIN"))

    @Before
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .build()
    }

    @Test
    fun createAspectTest() {

        val baseAspectData = AspectData("", "base", Gram.name, null, BaseType.Decimal.name)
        val baseAspect = aspectService.saveAspect(baseAspectData)

        val testProperty1 = AspectPropertyData("", "p1", baseAspect.id, AspectPropertyPower.ONE.name)
        val testProperty2 = AspectPropertyData("", "p2", baseAspect.id, AspectPropertyPower.INFINITY.name)

        val testData = AspectData("", "t1", Metre.name, null, BaseType.Decimal.name, listOf(testProperty1, testProperty2))

        val result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/aspect/create").with(authorities)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fromObject(testData)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn().response.contentAsString.let { toObject<AspectData>(it) }

        assertThat("response aspect has id", result.id, Is.`is`(IsNot.not(IsEmptyString())))
        assertThat("response aspect has two properties", result.properties.size, Is.`is`(2))
        assertThat("response aspect properties have not empty id", result.properties.all { it.id != "" }, Is.`is`(true))
    }

    @Test
    fun updateAspectTest() {

        val baseAspectData = AspectData("", "base", Gram.name, null, BaseType.Decimal.name)
        val baseAspect = aspectService.saveAspect(baseAspectData)

        val testProperty1 = AspectPropertyData("", "p1", baseAspect.id, AspectPropertyPower.ONE.name)
        val testProperty2 = AspectPropertyData("", "p2", baseAspect.id, AspectPropertyPower.INFINITY.name)

        val testData = AspectData("", "t1", Metre.name, null, BaseType.Decimal.name, listOf(testProperty1, testProperty2))

        val saved = aspectService.saveAspect(testData)

        val newProperty = AspectPropertyData("", "p3", baseAspect.id, AspectPropertyPower.INFINITY.name)
        val updatedProperty = testProperty2.copy(name = "p4", power = AspectPropertyPower.ZERO.name)

        val updateData = AspectData(
                saved.id,
                "t2",
                Kilometre.name,
                null,
                BaseType.Decimal.name,
                listOf(saved["p1"].toAspectPropertyData(), newProperty, updatedProperty.copy(id = saved["p2"].id, version = saved["p2"].version)),
                saved.version)

        val result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/aspect/update").with(authorities)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fromObject(updateData)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn().response.contentAsString.let { toObject<AspectData>(it) }

        assertThat("returned aspect has new name", result.name, Is.`is`(updateData.name))
        assertThat("returned aspect has new measure", result.measure, Is.`is`(updateData.measure))
        assertThat("returned aspect has correct property list size", result.properties.size, Is.`is`(updateData.properties.size))
        assertThat("returned aspect has correct property id for all props", result.properties.all { it.id != "" }, Is.`is`(true))
    }

}

private inline fun <reified T : Any> fromObject(obj: T): String = JSON.stringify(obj)

private inline fun <reified T : Any> toObject(json: String): T = JSON.parse(json)