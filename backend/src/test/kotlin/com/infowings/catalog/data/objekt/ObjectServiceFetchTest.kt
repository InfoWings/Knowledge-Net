package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import io.kotlintest.shouldBe
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ObjectServiceFetchTest {
    @Autowired
    lateinit var objectService: ObjectService

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var aspectService: AspectService

    private val username = "admin"
    private var detailedObjectId: String? = null

    private val knetSubjectName = "ObjectServiceFetchTest - Knowledge Net"
    private val reflexiaSubjectName = "ObjectServiceFetchTest - Reflexia"
    private var tubeObjectGuid: String? = null

    @BeforeEach
    fun initTestData() {
        val knetSubject = subjectService.createSubject(SubjectData(name = knetSubjectName, description = null), username)
        val reflexiaSubject = subjectService.createSubject(SubjectData(name = reflexiaSubjectName, description = null), username)

        val heightAspect =
            aspectService.save(AspectData(name = "ObjectServiceFetchTest - Height", measure = Metre.name, baseType = BaseType.Decimal.name), username)
        val widthAspect =
            aspectService.save(AspectData(name = "ObjectServiceFetchTest - Width", measure = Metre.name, baseType = BaseType.Decimal.name), username)
        val depthAspect =
            aspectService.save(AspectData(name = "ObjectServiceFetchTest - Depth", measure = Metre.name, baseType = BaseType.Decimal.name), username)
        val dimensionsAspect = aspectService.save(
            AspectData(
                name = "ObjectServiceFetchTest - Dimensions",
                baseType = BaseType.Text.name,
                properties = listOf(
                    AspectPropertyData(
                        id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name,
                        aspectId = heightAspect.idStrict(), aspectGuid = heightAspect.guidSoft()
                    ),
                    AspectPropertyData(
                        id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name,
                        aspectId = widthAspect.idStrict(), aspectGuid = heightAspect.guidSoft()
                    ),
                    AspectPropertyData(
                        id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name,
                        aspectId = depthAspect.idStrict(), aspectGuid = heightAspect.guidSoft()
                    )
                )
            ), username
        )

        val boxV1CreateResponse = objectService.create(
            ObjectCreateRequest(name = "ObjectServiceFetchTest - Box V1", description = null, subjectId = knetSubject.id),
            username
        )
        val boxDimensionPropertyCreateResponse =
            objectService.create(PropertyCreateRequest(boxV1CreateResponse.id, "", null, dimensionsAspect.idStrict()), username)
        val boxDimensionValueId = boxDimensionPropertyCreateResponse.rootValue.id
        objectService.create(
            ValueCreateRequest(
                ObjectValueData.DecimalValue.single("42"),
                null,
                boxDimensionPropertyCreateResponse.id,
                Metre.name,
                dimensionsAspect.properties[0].id,
                boxDimensionValueId
            ),
            username
        )
        objectService.create(
            ValueCreateRequest(
                ObjectValueData.DecimalValue.single("42"),
                null,
                boxDimensionPropertyCreateResponse.id,
                Metre.name,
                dimensionsAspect.properties[1].id,
                boxDimensionValueId
            ),
            username
        )
        objectService.create(
            ValueCreateRequest(
                ObjectValueData.DecimalValue.single("42"),
                null,
                boxDimensionPropertyCreateResponse.id,
                Metre.name,
                dimensionsAspect.properties[2].id,
                boxDimensionValueId
            ),
            username
        )
        detailedObjectId = boxV1CreateResponse.id

        objectService.create(
            ObjectCreateRequest(name = "ObjectServiceFetchTest - Box V2", description = null, subjectId = knetSubject.id),
            username
        )

        tubeObjectGuid = objectService.create(
            ObjectCreateRequest(name = "ObjectServiceFetchTest - Tube V1", description = null, subjectId = reflexiaSubject.id),
            username
        ).guid
    }


    private val paginationData = PaginationData(20, 1, 1000)

    @Test
    fun `fetched objects count should be equal to 3`() {
        val request = ObjectsRequestData(emptyList(), "", paginationData, null, emptyList())
        val objects = objectService.fetch(request)
        objects.size shouldBe 3
    }

    @Test
    fun fetchAllObjectsTruncatedWithPattern() {
        val request = ObjectsRequestData(emptyList(), "Tube", paginationData, null, emptyList())
        val objects = objectService.fetch(request)
        assertThat("Fetched objects count should be equal to 1", objects.size, Matchers.greaterThanOrEqualTo(1))
    }

    @Test
    fun `query objects by name and subject should return correct object`() {
        val subject = subjectService.findByName(reflexiaSubjectName)
        val request = ObjectsRequestData(emptyList(), "Tube", paginationData, listOf(subject!!.guid!!), emptyList())
        val objects = objectService.fetch(request)
        objects.size shouldBe 1
    }

    @Test
    fun `query objects by name and wrong subject should return no objects`() {
        val subject = subjectService.findByName(knetSubjectName)
        val request = ObjectsRequestData(emptyList(), "Tube", paginationData, listOf(subject!!.guid!!), emptyList())
        val objects = objectService.fetch(request)
        objects.size shouldBe 0
    }

    @Test
    fun `query objects by name and filter by another subject and exclude guid should return correct object`() {
        val subject = subjectService.findByName(knetSubjectName)
        val request = ObjectsRequestData(emptyList(), "Tube", paginationData, listOf(subject!!.guid!!), listOf(tubeObjectGuid!!))
        val objects = objectService.fetch(request)
        objects.size shouldBe 1
    }

    @Test
    fun `fetch objects filter by another subject should return correct count of objects`() {
        val subject = subjectService.findByName(knetSubjectName)
        val request = ObjectsRequestData(emptyList(), "", paginationData, listOf(subject!!.guid!!), emptyList())
        val objects = objectService.fetch(request)
        objects.size shouldBe 2
    }

    @Test
    fun `fetch objects filter by another subject and exclude guid should return correct count of objects`() {
        val subject = subjectService.findByName(knetSubjectName)
        val request = ObjectsRequestData(emptyList(), "", paginationData, listOf(subject!!.guid!!), listOf(tubeObjectGuid!!))
        val objects = objectService.fetch(request)
        objects.size shouldBe 3
    }

    @Test
    @Suppress("MagicNumber")
    fun fetchDetailedObject() {
        val detailedObject = objectService.getDetailedObject(detailedObjectId!!)
        assertThat("Fetched object has the same id", detailedObject.id, Matchers.`is`(detailedObjectId))
        assertThat("Fetched object has one property", detailedObject.objectPropertyViews.size, Matchers.`is`(1))
        assertThat(
            "Fetched object property has associated aspect with name \"ObjectServiceFetchTest - Dimensions\"",
            detailedObject.objectPropertyViews[0].aspect.name,
            Matchers.`is`("ObjectServiceFetchTest - Dimensions")
        )
        assertThat(
            "Fetched object has three values associated with dimensions",
            detailedObject.objectPropertyViews[0].values[0].children.size,
            Matchers.`is`(3)
        )
    }

}