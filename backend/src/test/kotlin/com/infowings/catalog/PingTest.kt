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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import kotlin.test.assertEquals


@ExtendWith(SpringExtension::class)
@SpringBootTest
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
    @Disabled
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
        val before = pingApi.status()
        aspectService.save(AspectData("", "name", description = null, baseType = BaseType.Text.name), "admin")

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        val metricsDelta = before.dbMetrics.diff(status.dbMetrics)

        assertEquals(OrientClass.values().size, metricsDelta.counts.size)
        assertEquals(1, metricsDelta.counts[OrientClass.ASPECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.REFBOOK_ITEM.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE_GROUP.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(2, metricsDelta.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    @Suppress("MagicNumber")
    fun testStatusWithTwoAspect() {
        val before = pingApi.status()
        aspectService.save(AspectData("", "name-1-${UUID.randomUUID()}", description = null, baseType = BaseType.Text.name), "admin")
        aspectService.save(AspectData("", "name-2-${UUID.randomUUID()}", description = null, baseType = BaseType.Text.name), "admin")

        val response = pingApi.status()
        val metricsDelta = before.dbMetrics.diff(response.dbMetrics)

        assertEquals(OrientClass.values().size, metricsDelta.counts.size)
        assertEquals(2, metricsDelta.counts[OrientClass.ASPECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.REFBOOK_ITEM.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE_GROUP.extName])
        assertEquals(2, metricsDelta.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(4, metricsDelta.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    fun testPingWithSubject() {
        val before = pingApi.status()
        subjectService.createSubject(SubjectData.Initial("name" + UUID.randomUUID()), username)

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        val metricsDelta = before.dbMetrics.diff(status.dbMetrics)

        assertEquals(OrientClass.values().size, metricsDelta.counts.size)
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.REFBOOK_ITEM.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE_GROUP.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    @Suppress("MagicNumber")
    fun testPingWithObject() {
        val before = pingApi.status()
        val subject = subjectService.createSubject(SubjectData.Initial("name"), username)
        objectService.create(ObjectCreateRequest("obj", null, subject.id), username)

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        val metricsDelta = before.dbMetrics.diff(status.dbMetrics)

        assertEquals(OrientClass.values().size, metricsDelta.counts.size)
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.SUBJECT.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.OBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.REFBOOK_ITEM.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE_GROUP.extName])
        assertEquals(3, metricsDelta.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(2, metricsDelta.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(2, metricsDelta.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(1, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    @Suppress("MagicNumber")
    fun testPingWithObjectProperty() {
        val before = pingApi.status()
        val subject = subjectService.createSubject(SubjectData.Initial("name${UUID.randomUUID()}"), username)
        val objectCreateResponse = objectService.create(ObjectCreateRequest("obj${UUID.randomUUID()}", null, subject.id), username)
        val aspect = aspectService.save(AspectData("", "name${UUID.randomUUID()}", description = null, baseType = BaseType.Text.name), "admin")
        objectService.create(
            PropertyCreateRequest(objectCreateResponse.id, "property${UUID.randomUUID()}", PropertyCardinality.ONE.name, aspect.idStrict()),
            username
        )

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        val metricsDelta = before.dbMetrics.diff(status.dbMetrics)

        assertEquals(OrientClass.values().size, metricsDelta.counts.size)
        assertEquals(1, metricsDelta.counts[OrientClass.ASPECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.SUBJECT.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.OBJECT.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.REFBOOK_ITEM.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE_GROUP.extName])
        assertEquals(8, metricsDelta.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(8, metricsDelta.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(7, metricsDelta.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(1, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(1, metricsDelta.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(1, metricsDelta.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    @Suppress("MagicNumber")
    fun testPingWithObjectValue() {
        val before = pingApi.status()
        val subject = subjectService.createSubject(SubjectData.Initial("name${UUID.randomUUID()}"), username)
        val objectCreateResponse = objectService.create(ObjectCreateRequest("obj${UUID.randomUUID()}", null, subject.id), username)
        val aspect = aspectService.save(AspectData("", "name${UUID.randomUUID()}", description = null, baseType = BaseType.Text.name), "admin")
        val propertyCreateResponse =
            objectService.create(
                PropertyCreateRequest(
                    objectCreateResponse.id,
                    "property${UUID.randomUUID()}",
                    PropertyCardinality.ONE.name,
                    aspect.idStrict()
                ), username
            )
        objectService.update(
            ValueUpdateRequest(
                propertyCreateResponse.rootValue.id,
                ObjectValueData.StringValue("hello"),
                null,
                null,
                propertyCreateResponse.rootValue.version
            ), username
        )

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        val metricsDelta = before.dbMetrics.diff(status.dbMetrics)

        assertEquals(OrientClass.values().size, metricsDelta.counts.size)
        assertEquals(1, metricsDelta.counts[OrientClass.ASPECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.SUBJECT.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.OBJECT.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.REFBOOK_ITEM.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE_GROUP.extName])
        assertEquals(10, metricsDelta.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(11, metricsDelta.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(7, metricsDelta.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(1, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(1, metricsDelta.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(1, metricsDelta.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    @Suppress("MagicNumber")
    fun testPingWithAspectProperty() {
        val before = pingApi.status()
        val aspect1 = aspectService.save(AspectData("", "name1${UUID.randomUUID()}", description = null, baseType = BaseType.Text.name), "admin")
        aspectService.save(
            AspectData(
                "", "name2${UUID.randomUUID()}", description = null, baseType = BaseType.Text.name,
                properties = listOf(
                    AspectPropertyData.Initial(
                        name = "prop${UUID.randomUUID()}",
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
        val metricsDelta = before.dbMetrics.diff(status.dbMetrics)

        assertEquals(OrientClass.values().size, metricsDelta.counts.size)
        assertEquals(2, metricsDelta.counts[OrientClass.ASPECT.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.REFBOOK_ITEM.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE_GROUP.extName])
        assertEquals(3, metricsDelta.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(7, metricsDelta.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(2, metricsDelta.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }

    @Test
    @Suppress("MagicNumber")
    fun testPingWithRefBook() {
        val before = pingApi.status()
        val aspect1 = aspectService.save(AspectData("", "name1${UUID.randomUUID()}", description = null, baseType = BaseType.Text.name), "admin")
        refBookService.createReferenceBook("rb${UUID.randomUUID()}", aspect1.idStrict(), "admin")

        val response = pingApi.ping()

        assertEquals("OK", response.pong)
        assertEquals("", response.details)

        val status = pingApi.status()
        val metricsDelta = before.dbMetrics.diff(status.dbMetrics)

        assertEquals(OrientClass.values().size, metricsDelta.counts.size)
        assertEquals(1, metricsDelta.counts[OrientClass.ASPECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.SUBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.OBJECT_VALUE.extName])
        assertEquals(1, metricsDelta.counts[OrientClass.REFBOOK_ITEM.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.MEASURE_GROUP.extName])
        assertEquals(3, metricsDelta.counts[OrientClass.HISTORY_EVENT.extName])
        assertEquals(3, metricsDelta.counts[OrientClass.HISTORY_ELEMENT.extName])
        assertEquals(2, metricsDelta.counts[OrientClass.HISTORY_ADD_LINK.extName])
        assertEquals(0, metricsDelta.counts[OrientClass.HISTORY_REMOVE_LINK.extName])

        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_ASPECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.SUBJECT_OF_OBJECT.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.OBJECT_OF_OBJECT_PROPERTY.extName])
        assertEquals(0, metricsDelta.edgeCounts[OrientEdge.ASPECT_OF_OBJECT_PROPERTY.extName])
    }
}