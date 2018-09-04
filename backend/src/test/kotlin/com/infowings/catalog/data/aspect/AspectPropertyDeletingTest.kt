package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectChangeResponse
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.storage.OrientClass
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import io.kotlintest.shouldBe
import kotlinx.serialization.json.JSON
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("UnsafeCallOnNullableType")
class AspectPropertyDeletingTest {

    @Autowired
    lateinit var db: OrientDatabase

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    private val authorities = SecurityMockMvcRequestPostProcessors.user("admin").authorities(SimpleGrantedAuthority("ADMIN"))

    private fun tearDownAllVertices() {
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

    private fun setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    private fun setUpCommonData() {
        val knetSubject: SubjectData
        val subjectDataRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/subject/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(SubjectData(null, "Knowledge Net Demo", 0, null, false)))
        ).andReturn()
        knetSubject = JSON.parse(subjectDataRequestResult.response.contentAsString)

        val aspectHeight: AspectData
        val aspectHeightRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(AspectData(name = "Height", measure = Millimetre.name, baseType = BaseType.Decimal.name)))
        ).andReturn()
        aspectHeight = JSON.parse(aspectHeightRequestResult.response.contentAsString)

        val aspectWidth: AspectData
        val aspectWidthRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(AspectData(name = "Width", measure = Millimetre.name, baseType = BaseType.Decimal.name)))
        ).andReturn()
        aspectWidth = JSON.parse(aspectWidthRequestResult.response.contentAsString)

        val aspectDepth: AspectData
        val aspectDepthRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(AspectData(name = "Depth", measure = Millimetre.name, baseType = BaseType.Decimal.name)))
        ).andReturn()
        aspectDepth = JSON.parse(aspectDepthRequestResult.response.contentAsString)

        val aspectDimensions: AspectData
        val aspectDimensionsRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/aspect/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(
                    AspectData(
                        name = "Dimensions",
                        measure = Millimetre.name,
                        baseType = BaseType.Decimal.name,
                        properties = listOf(
                            AspectPropertyData("", null, aspectHeight.id!!, PropertyCardinality.ONE.name, null),
                            AspectPropertyData("", null, aspectWidth.id!!, PropertyCardinality.ONE.name, null),
                            AspectPropertyData("", null, aspectDepth.id!!, PropertyCardinality.ONE.name, null)
                        )
                    )
                ))
        ).andReturn()
        aspectDimensions = JSON.parse(aspectDimensionsRequestResult.response.contentAsString)

        val objectBoxCreateResponse: ObjectChangeResponse
        val objectBoxRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/create")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(ObjectCreateRequest("Box", null, knetSubject.id!!)))
        ).andReturn()
        objectBoxCreateResponse = JSON.parse(objectBoxRequestResult.response.contentAsString)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/createProperty")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(PropertyCreateRequest(objectBoxCreateResponse.id, null, null, aspectDimensions.id!!)))
        ).andReturn()
    }

    @BeforeEach
    fun cleanDatabaseBeforeTest() {
        tearDownAllVertices()
        setUpMockMvc()
        setUpCommonData()
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
        propertyHeightDeleteResponse = JSON.parse(deleteAspectPropertyResult.response.contentAsString)

        assertAll(
            "Delete response contains correct structure",
            { propertyHeightDeleteResponse.id               shouldBe aspectPropertyHeight.id },
            { propertyHeightDeleteResponse.cardinality.name shouldBe aspectPropertyHeight.cardinality },
            { propertyHeightDeleteResponse.name             shouldBe aspectPropertyHeight.name },
            { propertyHeightDeleteResponse.parentAspect.id  shouldBe aspectDimensions.id },
            { propertyHeightDeleteResponse.childAspect.id   shouldBe aspectHeight.id }
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

        assertAll (
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
            { JSON.parse<BadRequest>(deleteHeightPropertyRequestResult.response.contentAsString).code shouldBe BadRequestCode.NEED_CONFIRMATION }
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

        val deleteHeightPropertyResponse: AspectPropertyDeleteResponse
        val deleteHeightPropertyRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/aspect/property/{id}?force={force}", aspectPropertyHeight.id, true)
                .with(authorities)
        ).andReturn()
        deleteHeightPropertyResponse = JSON.parse(deleteHeightPropertyRequestResult.response.contentAsString)

        assertAll(
            "Delete response contains correct structure",
            { deleteHeightPropertyResponse.id              shouldBe aspectPropertyHeight.id },
            { deleteHeightPropertyResponse.cardinality     shouldBe aspectPropertyHeight.cardinality },
            { deleteHeightPropertyResponse.name            shouldBe aspectPropertyHeight.name },
            { deleteHeightPropertyResponse.parentAspect.id shouldBe boxDimensionsValueAspectId },
            { deleteHeightPropertyResponse.childAspect.id  shouldBe aspectPropertyHeight.aspect.id }
        )
    }

    private fun fetchAspectList(): List<AspectData> {
        val getAspectListResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/aspect/all?orderFields=&direct=&q=")
                .with(authorities)
        ).andReturn()
        return JSON.parse<AspectsList>(getAspectListResult.response.contentAsString).aspects
    }

    private fun fetchObjectBoxEditDetails(): ObjectEditDetailsResponse {
        val objectsTruncatedList: List<ObjectGetResponse>
        val objectsTruncatedListRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/objects")
                .with(authorities)
        ).andReturn()
        objectsTruncatedList = JSON.parse<ObjectsResponse>(objectsTruncatedListRequestResult.response.contentAsString).objects
        val boxObjectTruncated = objectsTruncatedList.find { it.name == "Box" }!!

        val objectBoxEditDetailsRequestResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/objects/{id}/editdetails", boxObjectTruncated.id)
                .with(authorities)
        ).andReturn()
        return JSON.parse(objectBoxEditDetailsRequestResult.response.contentAsString)
    }

    private fun linkAspectHeightWithValue(objectPropertyId: String, aspectPropertyId: String, parentValueId: String) {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/objects/createValue")
                .with(authorities)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.stringify(
                    ValueCreateRequest(
                        ObjectValueData.DecimalValue("42.5"),
                        null,
                        objectPropertyId,
                        Millimetre.name,
                        aspectPropertyId,
                        parentValueId
                    ).toDTO()
                ))
        )
    }
}