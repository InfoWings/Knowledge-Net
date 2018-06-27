package com.infowings.catalog.data.aspect

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
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

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AspectServiceRestTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    private val wac: WebApplicationContext? = null

    private lateinit var mockMvc: MockMvc

    private val authorities =
        SecurityMockMvcRequestPostProcessors.user("admin").authorities(SimpleGrantedAuthority("ADMIN"))

    @Before
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    @Test
    fun createAspectTest() {
        val baseAspectData = AspectData("", "base", Gram.name, null, BaseType.Decimal.name)
        val baseAspect = aspectService.save(baseAspectData, username)

        val testProperty1 = AspectPropertyData("", "p1", baseAspect.idStrict(), PropertyCardinality.ONE.name, null)
        val testProperty2 = AspectPropertyData("", "p2", baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val testData =
            AspectData(
                "",
                "t1",
                Metre.name,
                null,
                BaseType.Decimal.name,
                listOf(testProperty1, testProperty2)
            )

        val result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create").with(authorities)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fromObject(testData))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString.let { toObject<AspectData>(it) }

        assertThat("response aspect has id", result.id, Is.`is`(IsNot.not(IsEmptyString())))
        assertThat("response aspect has two properties", result.properties.size, Is.`is`(2))
        assertThat("response aspect properties have not empty id", result.properties.all { it.id != "" }, Is.`is`(true))
    }

    @Test
    fun updateAspectTest() {

        val baseAspectData = AspectData("", "base", Gram.name, null, BaseType.Decimal.name)
        val baseAspect = aspectService.save(baseAspectData, username)

        val testProperty1 = AspectPropertyData("", "p1", baseAspect.idStrict(), PropertyCardinality.ONE.name, null)
        val testProperty2 = AspectPropertyData("", "p2", baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val testData =
            AspectData(
                "",
                "t1",
                Metre.name,
                null,
                BaseType.Decimal.name,
                listOf(testProperty1, testProperty2)
            )

        val saved = aspectService.save(testData, username)

        val newProperty = AspectPropertyData("", "p3", baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val updatedProperty = testProperty2.copy(name = "p4", cardinality = PropertyCardinality.ZERO.name)

        val propertyByName = saved.properties.groupBy { it.name }

        val savedP1 = propertyByName["p1"]?.get(0) ?: throw IllegalStateException("p1 not saved")
        val savedP2 = propertyByName["p2"]?.get(0) ?: throw IllegalStateException("p2 not saved")

        val updateData = AspectData(
            saved.id,
            "t2",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(
                savedP1,
                newProperty,
                updatedProperty.copy(id = savedP2.id, version = savedP2.version)
            ),
            saved.version
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/update").with(authorities)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fromObject(updateData))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString.let { toObject<AspectData>(it) }

        assertThat("returned aspect has new name", result.name, Is.`is`(updateData.name))
        assertThat("returned aspect has new measure", result.measure, Is.`is`(updateData.measure))
        assertThat(
            "returned aspect has correct property list size",
            result.properties.size,
            Is.`is`(updateData.properties.size)
        )
        assertThat(
            "returned aspect has correct property id for all props",
            result.properties.all { it.id != "" },
            Is.`is`(true)
        )
    }

    @Test
    fun createAspectDoubleTest() {
        val baseAspectData = AspectData("", "base", Gram.name, null, BaseType.Decimal.name)
        val baseAspect = aspectService.save(baseAspectData, username)

        val testProperty1 = AspectPropertyData("", "p1", baseAspect.idStrict(), PropertyCardinality.ONE.name, null)
        val testProperty2 = AspectPropertyData("", "p2", baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val testData =
            AspectData(
                "",
                "t1",
                Metre.name,
                null,
                BaseType.Decimal.name,
                listOf(testProperty1, testProperty2)
            )

        val result1 = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create").with(authorities)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fromObject(testData))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString.let { toObject<AspectData>(it) }

        assertThat("response aspect has id", result1.id, Is.`is`(IsNot.not(IsEmptyString())))
        assertThat("response aspect has two properties", result1.properties.size, Is.`is`(2))
        assertThat(
            "response aspect properties have not empty id",
            result1.properties.all { it.id != "" },
            Is.`is`(true)
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create").with(authorities)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fromObject(testData))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

}

private inline fun <reified T : Any> fromObject(obj: T): String = JSON.stringify(obj)

private inline fun <reified T : Any> toObject(json: String): T = JSON.parse(json)