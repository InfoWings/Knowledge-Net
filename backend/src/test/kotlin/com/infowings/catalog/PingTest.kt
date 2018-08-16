package com.infowings.catalog

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueUpdateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.external.PingApi
import com.infowings.catalog.storage.OrientClass
import com.infowings.catalog.storage.OrientEdge
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD, methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
class PingTest {
    private val username = "admin"

    @Autowired
    lateinit var pingApi: PingApi

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var objectService: ObjectService

    @Autowired
    lateinit var refBookService: ReferenceBookService

    @Test
    fun testPingEmpty() {
        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        assertEquals(OrientClass.values().size, status.dbMetrics.counts.size)
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testPingWithAspect() {
        aspectService.save(AspectData("", "name", description = null, baseType = BaseType.Text.name), "admin")

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        assertEquals(OrientClass.values().size, status.dbMetrics.counts.size)
        assertEquals(1, status.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(1, status.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(2, status.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testStatusWithTwoAspect() {
        aspectService.save(AspectData("", "name-1", description = null, baseType = BaseType.Text.name), "admin")
        aspectService.save(AspectData("", "name-2", description = null, baseType = BaseType.Text.name), "admin")

        val response = pingApi.status()

        assertEquals(OrientClass.values().size, response.dbMetrics.counts.size)
        assertEquals(2, response.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(0, response.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, response.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, response.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(0, response.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, response.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, response.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(response.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(response.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(2, response.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(4, response.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(0, response.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, response.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, response.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, response.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, response.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, response.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, response.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testPingWithSubject() {
        subjectService.createSubject(SubjectData.Initial("name"), username)

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        assertEquals(OrientClass.values().size, status.dbMetrics.counts.size)
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(1, status.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testPingWithObject() {
        val subject = subjectService.createSubject(SubjectData.Initial("name"), username)
        objectService.create(ObjectCreateRequest("obj", null, subject.id), username)

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        assertEquals(OrientClass.values().size, status.dbMetrics.counts.size)
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(3, status.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(2, status.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(2, status.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(1, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testPingWithObjectProperty() {
        val subject = subjectService.createSubject(SubjectData.Initial("name"), username)
        val objectCreateResponse = objectService.create(ObjectCreateRequest("obj", null, subject.id), username)
        val aspect = aspectService.save(AspectData("", "name", description = null, baseType = BaseType.Text.name), "admin")
        objectService.create(PropertyCreateRequest(objectCreateResponse.id, "property", PropertyCardinality.ONE.name, aspect.idStrict()), username)

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        assertEquals(OrientClass.values().size, status.dbMetrics.counts.size)
        assertEquals(1, status.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(6, status.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(7, status.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(5, status.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(1, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(1, status.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(1, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testPingWithObjectValue() {
        val subject = subjectService.createSubject(SubjectData.Initial("name"), username)
        val objectCreateResponse = objectService.create(ObjectCreateRequest("obj", null, subject.id), username)
        val aspect = aspectService.save(AspectData("", "name", description = null, baseType = BaseType.Text.name), "admin")
        val propertyCreateResponse =
            objectService.create(PropertyCreateRequest(objectCreateResponse.id, "property", PropertyCardinality.ONE.name, aspect.idStrict()), username)
        objectService.update(
            ValueUpdateRequest(
                propertyCreateResponse.rootValue.id,
                ObjectValueData.StringValue("hello"),
                null,
                propertyCreateResponse.rootValue.version
            ), username
        )

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        assertEquals(OrientClass.values().size, status.dbMetrics.counts.size)
        assertEquals(1, status.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(8, status.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(10, status.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(7, status.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(1, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(1, status.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(1, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testPingWithAspectProperty() {
        val aspect1 = aspectService.save(AspectData("", "name1", description = null, baseType = BaseType.Text.name), "admin")
        aspectService.save(
            AspectData(
                "", "name2", description = null, baseType = BaseType.Text.name,
                properties = listOf(
                    AspectPropertyData.Initial(
                        name = "prop",
                        cardinality = PropertyCardinality.ONE.name,
                        aspectId = aspect1.idStrict(),
                        description = null
                    )
                )
            ), "admin"
        )

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        assertEquals(OrientClass.values().size, status.dbMetrics.counts.size)
        assertEquals(2, status.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(3, status.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(7, status.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(2, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testPingWithRefBook() {
        val aspect1 = aspectService.save(AspectData("", "name1", description = null, baseType = BaseType.Text.name), "admin")
        refBookService.createReferenceBook("rb", aspect1.idStrict(), "admin")

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        assertEquals(OrientClass.values().size, status.dbMetrics.counts.size)
        assertEquals(1, status.dbMetrics.counts[OrientClass.ASPECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(1, status.dbMetrics.counts[OrientClass.REFBOOK_ITEM.extName])
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE.extName]!!, 0)
        assertGreater(status.dbMetrics.counts[OrientClass.MEASURE_GROUP.extName]!!, 0)
        assertEquals(3, status.dbMetrics.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(3, status.dbMetrics.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(2, status.dbMetrics.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, status.dbMetrics.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, status.dbMetrics.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }
}