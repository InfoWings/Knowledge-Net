package com.infowings.catalog.data.subject

import com.fasterxml.jackson.databind.ObjectMapper
import com.infowings.catalog.AbstractMvcTest
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.createTestAspect
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.toSubjectData
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.not
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDateTime

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SubjectApiTest : AbstractMvcTest() {

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    private lateinit var aspectService: AspectService

    @Test
    fun getAll() {
        createTestSubject("TestSubject")
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/subject/all").with(authorities)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$", not(empty<Any>())))
    }

    @Test
    fun create() {
        createTestAspect("TestCreateSubjectAspect", aspectService)
        val sd = SubjectData(name = "TestSubject_CreateApi_${LocalDateTime.now()}", description = null)
        val subjectDataJson: String = ObjectMapper().writeValueAsString(sd)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/subject/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(subjectDataJson)
                .with(authorities)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(sd.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNotEmpty)
    }


    @Test
    fun update() {
        val subject = createTestSubject("TestSubjectUpdate")
        val subjectDataJson: String = ObjectMapper().writeValueAsString(subject.toSubjectData().copy(description = "123"))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/subject/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(subjectDataJson)
                .with(authorities)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(subject.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(subject.id))
    }

    @Test
    fun suggestion() {
        val s = createTestSubject("TestSuggestionSubject", listOf("tstAspect1", "tstAspect2"))
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/search/subject/suggestion")
                .with(authorities)
                .param("text", "TestSuggestionSubject")
                .param("aspectText", "Aspect1")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.subject[0].name").value(s.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.subject[0].id").value(s.id))

    }

    private fun createTestSubject(name: String, aspectNames: List<String> = listOf("TestSubjectAspect")): Subject =
        createTestSubject(name, aspectNames, aspectService, subjectService)

}

