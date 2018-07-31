package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
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

    @Before
    fun initTestData() {
        val knetSubject = subjectService.createSubject(SubjectData(name = "Knowledge Net", description = null), username)
        val reflexiaSubject = subjectService.createSubject(SubjectData(name = "Reflexia", description = null), username)

        val heightAspect = aspectService.save(AspectData(name = "Height", measure = Metre.name, baseType = BaseType.Decimal.name), username)
        val widthAspect = aspectService.save(AspectData(name = "Width", measure = Metre.name, baseType = BaseType.Decimal.name), username)
        val depthAspect = aspectService.save(AspectData(name = "Depth", measure = Metre.name, baseType = BaseType.Decimal.name), username)
        val dimensionsAspect = aspectService.save(
            AspectData(
                name = "Dimensions",
                baseType = BaseType.Text.name,
                properties = listOf(
                    AspectPropertyData(id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name, aspectId = heightAspect.idStrict()),
                    AspectPropertyData(id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name, aspectId = widthAspect.idStrict()),
                    AspectPropertyData(id = "", name = "", description = "", cardinality = PropertyCardinality.ONE.name, aspectId = depthAspect.idStrict())
                )
            ), username
        )

        val boxV1Id = objectService.create(
            ObjectCreateRequest(name = "Box V1", description = null, subjectId = knetSubject.id, subjectVersion = knetSubject.version),
            username
        )
        val boxDimensionPropertyId = objectService.create(PropertyCreateRequest(boxV1Id, "", null, dimensionsAspect.idStrict()), username)
        val boxDimensionValue = objectService.create(ValueCreateRequest(ObjectValueData.NullValue, null, boxDimensionPropertyId), username)
        objectService.create(
            ValueCreateRequest(
                ObjectValueData.DecimalValue("42"),
                null,
                boxDimensionPropertyId,
                null,
                dimensionsAspect.properties[0].id,
                boxDimensionValue.id.toString()
            ),
            username
        )
        objectService.create(
            ValueCreateRequest(
                ObjectValueData.DecimalValue("42"),
                null,
                boxDimensionPropertyId,
                null,
                dimensionsAspect.properties[1].id,
                boxDimensionValue.id.toString()
            ),
            username
        )
        objectService.create(
            ValueCreateRequest(
                ObjectValueData.DecimalValue("42"),
                null,
                boxDimensionPropertyId,
                null,
                dimensionsAspect.properties[2].id,
                boxDimensionValue.id.toString()
            ),
            username
        )
        detailedObjectId = boxV1Id

        objectService.create(
            ObjectCreateRequest(name = "Box V2", description = null, subjectId = knetSubject.id, subjectVersion = knetSubject.version + 1),
            username
        )

        objectService.create(
            ObjectCreateRequest(name = "Tube V1", description = null, subjectId = reflexiaSubject.id, subjectVersion = reflexiaSubject.version),
            username
        )
    }


    @Test
    fun fetchAllObjectsTruncated() {
        val objects = objectService.fetch()
        assertThat("Fetched objects count should be equal to 3", objects.size, Matchers.`is`(3))
    }

    @Test
    fun fetchDetailedObject() {
        val detailedObject = objectService.getDetailedObject(detailedObjectId!!)
        assertThat("Fetched object has the same id", detailedObject.id, Matchers.`is`(detailedObjectId))
        assertThat("Fetched object has one property", detailedObject.objectProperties.size, Matchers.`is`(1))
        assertThat(
            "Fetched object property has associated aspect with name \"Dimensions\"",
            detailedObject.objectProperties[0].aspect.name,
            Matchers.`is`("Dimensions")
        )
        assertThat("Fetched object has three values associated with dimensions", detailedObject.objectProperties[0].values[0].children.size, Matchers.`is`(3))
    }

}