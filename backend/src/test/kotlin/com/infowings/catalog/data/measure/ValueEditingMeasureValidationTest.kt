package com.infowings.catalog.data.measure

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.storage.OrientClass
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import io.kotlintest.should
import io.kotlintest.shouldThrow
import kotlinx.serialization.json.JSON
import org.junit.jupiter.api.AfterEach
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
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.util.NestedServletException

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("UnsafeCallOnNullableType")
class ValueEditingMeasureValidationTest {

    @Autowired
    lateinit var db: OrientDatabase

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    private val authorities = SecurityMockMvcRequestPostProcessors.user("admin").authorities(SimpleGrantedAuthority("ADMIN"))

    private lateinit var knetSubject: SubjectData
    private lateinit var aspectHeight: AspectData
    private lateinit var tubeObject: ObjectChangeResponse
    private lateinit var tubeObjectHeightProperty: PropertyCreateResponse

    @BeforeEach
    fun initializeData() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()

        val subjectDataRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/subject/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(SubjectData(null, "Knowledge Net Demo", 0, null, false)))
        ).andReturn()
        knetSubject = JSON.parse(subjectDataRequestResult.response.contentAsString)

        val aspectHeightRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(AspectData(name = "Height", measure = Millimetre.name, baseType = BaseType.Decimal.name)))
        ).andReturn()
        aspectHeight = JSON.parse(aspectHeightRequestResult.response.contentAsString)

        val tubeObjectRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(ObjectCreateRequest("Tube", null, knetSubject.id!!)))
        ).andReturn()
        tubeObject = JSON.parse(tubeObjectRequestResult.response.contentAsString)

        val tubeObjectPropertyRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/createProperty")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(PropertyCreateRequest(tubeObject.id, null, null, aspectHeight.id!!)))
        ).andReturn()
        tubeObjectHeightProperty = JSON.parse(tubeObjectPropertyRequestResult.response.contentAsString)
        tubeObject = tubeObject.copy(version = tubeObjectHeightProperty.obj.version)
    }

    @Test
    fun `Create value that requires measure without measure triggers exception`() {
        val exception = shouldThrow<NestedServletException> {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/objects/updateValue")
                    .with(authorities)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        JSON.stringify(
                            ValueUpdateRequest(
                                tubeObjectHeightProperty.rootValue.id,
                                ObjectValueData.DecimalValue("42"),
                                null,
                                null,
                                tubeObjectHeightProperty.rootValue.version
                            ).toDTO()
                        )
                    )
            )
        }
        exception.cause should { it != null && it is IllegalArgumentException }
    }

    @Test
    fun `Create value that requires measure with measure from another group triggers exception`() {
        val exception = shouldThrow<NestedServletException> {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/objects/updateValue")
                    .with(authorities)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        JSON.stringify(
                            ValueUpdateRequest(
                                tubeObjectHeightProperty.rootValue.id,
                                ObjectValueData.DecimalValue("42"),
                                Ampere.name,
                                null,
                                tubeObjectHeightProperty.rootValue.version
                            ).toDTO()
                        )
                    )
            )
        }
        exception.cause should { it != null && it is IllegalArgumentException }
    }

    @AfterEach
    fun tearDownAllVertices() {
        transaction(db) {
            db.command("DELETE VERTEX ${OrientClass.ASPECT.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.ASPECT_PROPERTY.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.SUBJECT.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.OBJECT.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.OBJECT_PROPERTY.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.OBJECT_VALUE.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.REFBOOK_ITEM.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.HISTORY_ADD_LINK.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.HISTORY_ELEMENT.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.HISTORY_EVENT.extName}") {}
            db.command("DELETE VERTEX ${OrientClass.HISTORY_REMOVE_LINK.extName}") {}

        }
    }

}