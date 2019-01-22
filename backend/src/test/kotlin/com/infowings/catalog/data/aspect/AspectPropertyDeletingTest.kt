package com.infowings.catalog.data.aspect

import com.infowings.catalog.AbstractMvcTest
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import io.kotlintest.shouldBe
import kotlinx.serialization.json.JSON
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Suppress("UnsafeCallOnNullableType", "StringLiteralDuplication", "MagicNumber")
class AspectPropertyDeletingTest : AbstractMvcTest() {

    @BeforeEach
    fun setUpCommonData() {
        val subjectDataRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/subject/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(SubjectData.serializer(), SubjectData(null, "Knowledge Net Demo", 0, null, false)))
        ).andReturn()
        val knetSubject = JSON.parse(SubjectData.serializer(), subjectDataRequestResult.response.contentAsString)

        val aspectHeightRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(AspectData.serializer(), AspectData(name = "Height", measure = Millimetre.name, baseType = BaseType.Decimal.name)))
        ).andReturn()
        val aspectHeight = JSON.parse(AspectData.serializer(), aspectHeightRequestResult.response.contentAsString)

        val aspectWidthRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(AspectData.serializer(), AspectData(name = "Width", measure = Millimetre.name, baseType = BaseType.Decimal.name)))
        ).andReturn()
        val aspectWidth = JSON.parse(AspectData.serializer(), aspectWidthRequestResult.response.contentAsString)

        val aspectDepthRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(AspectData.serializer(), AspectData(name = "Depth", measure = Millimetre.name, baseType = BaseType.Decimal.name)))
        ).andReturn()
        val aspectDepth = JSON.parse(AspectData.serializer(), aspectDepthRequestResult.response.contentAsString)

        val aspectDimensionsRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JSON.stringify(
                        AspectData.serializer(),
                        AspectData(
                            name = "Dimensions",
                            measure = Millimetre.name,
                            baseType = BaseType.Decimal.name,
                            properties = listOf(
                                AspectPropertyData("", null, aspectHeight.idStrict(), aspectHeight.guidSoft(), PropertyCardinality.ONE.name, null),
                                AspectPropertyData("", null, aspectWidth.idStrict(), aspectWidth.idStrict(), PropertyCardinality.ONE.name, null),
                                AspectPropertyData("", null, aspectDepth.idStrict(), aspectDepth.guidSoft(), PropertyCardinality.ONE.name, null)
                            )
                        )
                    )
                )
        ).andReturn()
        val aspectDimensions = JSON.parse(AspectData.serializer(), aspectDimensionsRequestResult.response.contentAsString)

        val objectBoxRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(ObjectCreateRequest.serializer(), ObjectCreateRequest("Box", null, knetSubject.id!!)))
        ).andReturn()
        val objectBoxCreateResponse = JSON.parse(ObjectChangeResponse.serializer(), objectBoxRequestResult.response.contentAsString)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/createProperty")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JSON.stringify(
                        PropertyCreateRequest.serializer(),
                        PropertyCreateRequest(objectBoxCreateResponse.id, null, null, aspectDimensions.id!!)
                    )
                )
        ).andReturn()
    }

    @Test
    fun `Deleting aspect property that is not linked should result in correct response`() {
        val aspectList = fetchAspectList()
        val aspectDimensions = aspectList.find { it.name == "Dimensions" }!!
        val aspectHeight = aspectList.find { it.name == "Height" }!!
        val aspectPropertyHeight = aspectDimensions.properties.find { it.aspectId == aspectHeight.id }!!

        val propertyHeightDeleteResponse: AspectPropertyDeleteResponse
        val deleteAspectPropertyResult = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/aspect/property/{id}?force={force}", aspectPropertyHeight.id, false)
                .with(authorities)
        ).andReturn()
        propertyHeightDeleteResponse = JSON.parse(AspectPropertyDeleteResponse.serializer(), deleteAspectPropertyResult.response.contentAsString)

        assertAll(
            "Delete response contains correct structure",
            { propertyHeightDeleteResponse.id shouldBe aspectPropertyHeight.id },
            { propertyHeightDeleteResponse.cardinality.name shouldBe aspectPropertyHeight.cardinality },
            { propertyHeightDeleteResponse.name shouldBe aspectPropertyHeight.name },
            { propertyHeightDeleteResponse.parentAspect.id shouldBe aspectDimensions.id },
            { propertyHeightDeleteResponse.childAspect.id shouldBe aspectHeight.id }
        )
    }

    @Test
    fun `Deleting aspect property that is not linked should result in absence of the property in next response`() {
        val setupAspectList = fetchAspectList()
        val setupAspectDimensions = setupAspectList.find { it.name == "Dimensions" }!!
        val setupAspectHeight = setupAspectList.find { it.name == "Height" }!!
        val setupAspectPropertyHeight = setupAspectDimensions.properties.find { it.aspectId == setupAspectHeight.id }!!

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/aspect/property/{id}?force={force}", setupAspectPropertyHeight.id, false)
                .with(authorities)
        ).andReturn()

        val newAspectList = fetchAspectList()
        val newAspectDimensions = newAspectList.find { it.name == "Dimensions" }!!
        val newAspectHeight = newAspectList.find { it.name == "Height" }!!
        val newAspectWidth = newAspectList.find { it.name == "Width" }!!
        val newAspectDepth = newAspectList.find { it.name == "Depth" }!!

        assertAll(
            "Dimensions aspect after property deletion should not contain deleted property",
            { newAspectDimensions.properties.size shouldBe 2 },
            { assertNotNull(newAspectDimensions.properties.find { it.aspectId == newAspectWidth.id }) },
            { assertNotNull(newAspectDimensions.properties.find { it.aspectId == newAspectDepth.id }) },
            { assertNull(newAspectDimensions.properties.find { it.aspectId == newAspectHeight.id }) }
        )
    }

    @Test
    fun `Non-force deletion of linked aspect property should result in exception`() {
        val objectBoxEditDetails = fetchObjectBoxEditDetails()
        val boxDimensionsPropertyId = objectBoxEditDetails.properties.single().id
        val boxDimensionsValueId = objectBoxEditDetails.properties.single().rootValues.single().id
        val aspectPropertyHeightId = objectBoxEditDetails.properties.single().aspectDescriptor.properties.find { it.aspect.name == "Height" }!!.id

        linkAspectHeightWithValue(boxDimensionsPropertyId, aspectPropertyHeightId, boxDimensionsValueId)

        val deleteHeightPropertyRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/aspect/property/{id}?force={force}", aspectPropertyHeightId, false)
                .with(authorities)
        ).andReturn()

        assertAll(
            "Delete linked aspect response should be of status 400 and contain BadRequest information in body",
            { deleteHeightPropertyRequestResult.response.status shouldBe 400 },
            { JSON.parse(BadRequest.serializer(), deleteHeightPropertyRequestResult.response.contentAsString).code shouldBe BadRequestCode.NEED_CONFIRMATION }
        )
    }

    @Test
    fun `Force deletion of linked aspect property should result successful response`() {
        val objectBoxEditDetails = fetchObjectBoxEditDetails()
        val boxDimensionsPropertyId = objectBoxEditDetails.properties.single().id
        val boxDimensionsValueId = objectBoxEditDetails.properties.single().rootValues.single().id
        val boxDimensionsValueAspectId = objectBoxEditDetails.properties.single().aspectDescriptor.id
        val aspectPropertyHeight = objectBoxEditDetails.properties.single().aspectDescriptor.properties.find { it.aspect.name == "Height" }!!

        linkAspectHeightWithValue(boxDimensionsPropertyId, aspectPropertyHeight.id, boxDimensionsValueId)

        val deleteHeightPropertyRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/aspect/property/{id}?force={force}", aspectPropertyHeight.id, true)
                .with(authorities)
        ).andReturn()
        val deleteHeightPropertyResponse = JSON.parse(AspectPropertyDeleteResponse.serializer(), deleteHeightPropertyRequestResult.response.contentAsString)

        assertAll(
            "Delete response contains correct structure",
            { deleteHeightPropertyResponse.id shouldBe aspectPropertyHeight.id },
            { deleteHeightPropertyResponse.cardinality shouldBe aspectPropertyHeight.cardinality },
            { deleteHeightPropertyResponse.name shouldBe aspectPropertyHeight.name },
            { deleteHeightPropertyResponse.parentAspect.id shouldBe boxDimensionsValueAspectId },
            { deleteHeightPropertyResponse.childAspect.id shouldBe aspectPropertyHeight.aspect.id }
        )
    }

    private fun fetchAspectList(): List<AspectData> {
        val getAspectListResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/aspect/all?orderFields=&direct=&q=")
                .with(authorities)
        ).andReturn()
        return JSON.parse(AspectsList.serializer(), getAspectListResult.response.contentAsString).aspects
    }

    private fun fetchObjectBoxEditDetails(): ObjectEditDetailsResponse {
        val objectsTruncatedList: List<ObjectGetResponse>
        val objectsTruncatedListRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/objects?orderFields=&direct=")
                .with(authorities)
        ).andReturn()
        objectsTruncatedList = JSON.parse(ObjectsResponse.serializer(), objectsTruncatedListRequestResult.response.contentAsString).objects
        val boxObjectTruncated = objectsTruncatedList.find { it.name == "Box" }!!

        val objectBoxEditDetailsRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/objects/{id}/editdetails", boxObjectTruncated.id)
                .with(authorities)
        ).andReturn()
        return JSON.parse(ObjectEditDetailsResponse.serializer(), objectBoxEditDetailsRequestResult.response.contentAsString)
    }

    private fun linkAspectHeightWithValue(objectPropertyId: String, aspectPropertyId: String, parentValueId: String) {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/createValue")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JSON.stringify(
                        ValueCreateRequestDTO.serializer(),
                        ValueCreateRequest(
                            ObjectValueData.DecimalValue.single("42.5"),
                            null,
                            objectPropertyId,
                            Millimetre.name,
                            aspectPropertyId,
                            parentValueId
                        ).toDTO()
                    )
                )
        )
    }
}