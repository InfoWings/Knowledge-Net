package com.infowings.catalog.data.measure

import com.infowings.catalog.AbstractMvcTest
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.shouldThrow
import kotlinx.serialization.json.JSON
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.util.NestedServletException

@Suppress("UnsafeCallOnNullableType")
class ValueEditingMeasureValidationTest : AbstractMvcTest() {

    private lateinit var knetSubject: SubjectData
    private lateinit var aspectHeight: AspectData
    private lateinit var tubeObject: ObjectChangeResponse
    private lateinit var tubeObjectHeightProperty: PropertyCreateResponse

    @BeforeEach
    fun initializeData() {
        val subjectDataRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/subject/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(SubjectData.serializer(), SubjectData(null, "Knowledge Net Demo", 0, null, false)))
        ).andReturn()
        knetSubject = JSON.parse(SubjectData.serializer(), subjectDataRequestResult.response.contentAsString)

        val aspectHeightRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(AspectData.serializer(), AspectData(name = "Height", measure = Millimetre.name, baseType = BaseType.Decimal.name)))
        ).andReturn()
        aspectHeight = JSON.parse(AspectData.serializer(), aspectHeightRequestResult.response.contentAsString)

        val tubeObjectRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(ObjectCreateRequest.serializer(), ObjectCreateRequest("Tube", null, knetSubject.id!!)))
        ).andReturn()
        tubeObject = JSON.parse(ObjectChangeResponse.serializer(), tubeObjectRequestResult.response.contentAsString)

        val tubeObjectPropertyRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/createProperty")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(PropertyCreateRequest.serializer(), PropertyCreateRequest(tubeObject.id, null, null, aspectHeight.id!!)))
        ).andReturn()
        tubeObjectHeightProperty = JSON.parse(PropertyCreateResponse.serializer(), tubeObjectPropertyRequestResult.response.contentAsString)
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
                            ValueUpdateRequestDTO.serializer(),
                            ValueUpdateRequest(
                                tubeObjectHeightProperty.rootValue.id,
                                ObjectValueData.DecimalValue.single("42"),
                                null,
                                null,
                                tubeObjectHeightProperty.rootValue.version
                            ).toDTO()
                        )
                    )
            )
        }
        exception.cause.shouldBeTypeOf<IllegalArgumentException>()
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
                            ValueUpdateRequestDTO.serializer(),
                            ValueUpdateRequest(
                                tubeObjectHeightProperty.rootValue.id,
                                ObjectValueData.DecimalValue.single("42"),
                                Ampere.name,
                                null,
                                tubeObjectHeightProperty.rootValue.version
                            ).toDTO()
                        )
                    )
            )
        }
        exception.cause.shouldBeTypeOf<IllegalStateException>()
    }


}