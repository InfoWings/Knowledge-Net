package com.infowings.catalog.data.aspect

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.serializer
import org.hamcrest.core.IsNot
import org.hamcrest.text.IsEmptyString
import org.junit.Assert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AspectServiceRestTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    private lateinit var wac: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    private val authorities =
        SecurityMockMvcRequestPostProcessors.user("admin").authorities(SimpleGrantedAuthority("ADMIN"))

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    @Test
    fun createAspectTest() {
        val baseAspectData = AspectData("", "createAspectTest", Gram.name, null, BaseType.Decimal.name)
        val baseAspect = aspectService.save(baseAspectData, username)

        val testProperty1 = AspectPropertyData("", "p1", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.ONE.name, null)
        val testProperty2 = AspectPropertyData("", "p2", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)

        val testData =
            AspectData(
                id = "",
                name = "createAspectTest-t1",
                measure = Metre.name,
                domain = null,
                baseType = BaseType.Decimal.name,
                properties = listOf(testProperty1, testProperty2)
            )

        val post = MockMvcRequestBuilders.post("/api/aspect/create").with(authorities)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(fromObject(testData))

        val result = mockMvc.perform(post)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString.toObject<AspectData>()

        assertThat("response aspect has id", result.id, IsNot.not(IsEmptyString()))
        assertThat("response aspect has two properties", result.properties.size, Is(2))
        assertThat("response aspect properties have not empty id", result.properties.all { it.id != "" }, Is(true))
    }

    @Test
    fun updateAspectTest() {

        val baseAspectData = AspectData("", "updateAspectTest", Gram.name, null, BaseType.Decimal.name)
        val baseAspect = aspectService.save(baseAspectData, username)

        val testProperty1 = AspectPropertyData("", "p1", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.ONE.name, null)
        val testProperty2 = AspectPropertyData("", "p2", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)

        val testData =
            AspectData(
                id = "",
                name = "updateAspectTest-t1",
                measure = Metre.name,
                domain = null,
                baseType = BaseType.Decimal.name,
                properties = listOf(testProperty1, testProperty2)
            )

        val saved = aspectService.save(testData, username)

        val newProperty = AspectPropertyData("", "p3", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)
        val updatedProperty = testProperty2.copy(name = "p4", cardinality = PropertyCardinality.ZERO.name)

        val propertyByName = saved.properties.groupBy { it.name }

        val savedP1 = propertyByName["p1"]?.get(0) ?: throw IllegalStateException("p1 not saved")
        val savedP2 = propertyByName["p2"]?.get(0) ?: throw IllegalStateException("p2 not saved")

        val updateData = AspectData(
            id = saved.id,
            name = "t2",
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(
                savedP1,
                newProperty,
                updatedProperty.copy(id = savedP2.id, version = savedP2.version)
            ),
            version = saved.version
        )

        val post = MockMvcRequestBuilders.post("/api/aspect/update").with(authorities)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(fromObject(updateData))

        val result = mockMvc.perform(post)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString.toObject<AspectData>()

        assertThat("returned aspect has new name", result.name, Is(updateData.name))
        assertThat("returned aspect has new measure", result.measure, Is(updateData.measure))
        assertThat(
            "returned aspect has correct property list size",
            result.properties.size,
            Is(updateData.properties.size)
        )
        assertThat(
            "returned aspect has correct property id for all props",
            result.properties.all { it.id != "" },
            Is(true)
        )
    }

    @Test
    fun createAspectDoubleTest() {
        val baseAspectData = AspectData("", "createAspectDoubleTest", Gram.name, null, BaseType.Decimal.name)
        val baseAspect = aspectService.save(baseAspectData, username)

        val testProperty1 = AspectPropertyData("", "p1", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.ONE.name, null)
        val testProperty2 = AspectPropertyData("", "p2", baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)

        val testData =
            AspectData(
                id = "",
                name = "createAspectDoubleTest-t1",
                measure = Metre.name,
                domain = null,
                baseType = BaseType.Decimal.name,
                properties = listOf(testProperty1, testProperty2)
            )

        val post = MockMvcRequestBuilders.post("/api/aspect/create").with(authorities)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(fromObject(testData))

        val result1 = mockMvc.perform(post)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString.toObject<AspectData>()

        assertThat("response aspect has id", result1.id, IsNot.not(IsEmptyString()))
        assertThat("response aspect has two properties", result1.properties.size, Is(2))
        assertThat(
            "response aspect properties have not empty id",
            result1.properties.all { it.id != "" },
            Is(true)
        )

        mockMvc.perform(post)
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

}

@UseExperimental(ImplicitReflectionSerializer::class)
private inline fun <reified T : Any> fromObject(obj: T): String = JSON.stringify(T::class.serializer(), obj)

@UseExperimental(ImplicitReflectionSerializer::class)
private inline fun <reified T : Any> String.toObject(): T = JSON.parse(T::class.serializer(), this)