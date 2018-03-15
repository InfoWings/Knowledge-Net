package com.infowings.catalog.data.subject

import com.fasterxml.jackson.databind.ObjectMapper
import com.infowings.catalog.AbstractMvcTest
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.not
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers


class SubjectApiTest : AbstractMvcTest() {

    @Autowired
    lateinit var subjectService: SubjectService

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
        val aspect = createTestAspect("TestSubjectAspect")
        val sd = SubjectData(name = "TestSubject_CreateApi", aspects = listOf(AspectData(aspect.id, "tstAspect")))
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
        val sd = SubjectData(id = subject.id, name = "TestSubjectUpdateNewName")
        val subjectDataJson: String = ObjectMapper().writeValueAsString(sd)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/subject/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(subjectDataJson)
                .with(authorities)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(sd.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(subject.id))

    }

    private fun createTestSubject(name: String): Subject {
        val aspect = createTestAspect("TestSubjectAspect")
        val sd = SubjectData(
            name = name,
            aspects = listOf(AspectData(aspect.id, "tstAspect"))
        )
        return subjectService.findByName(name) ?: subjectService.createSubject(sd)
    }

}