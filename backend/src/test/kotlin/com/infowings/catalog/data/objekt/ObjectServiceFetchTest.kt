package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
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

    @BeforeEach
    fun initTestData() {
        val knetSubject = subjectService.createSubject(SubjectData(name = "ObjectServiceFetchTest - Knowledge Net", description = null), username)
        val reflexiaSubject = subjectService.createSubject(SubjectData(name = "ObjectServiceFetchTest - Reflexia", description = null), username)

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
                    AspectPropertyData(id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name, aspectId = heightAspect.idStrict()),
                    AspectPropertyData(id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name, aspectId = widthAspect.idStrict()),
                    AspectPropertyData(id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name, aspectId = depthAspect.idStrict())
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

        objectService.create(
            ObjectCreateRequest(name = "ObjectServiceFetchTest - Tube V1", description = null, subjectId = reflexiaSubject.id),
            username
        )
    }


    @Test
    fun fetchAllObjectsTruncated() {
        val objects = objectService.fetch()
        assertThat("Fetched objects count should be equal to 3", objects.size, Matchers.`is`(3))
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