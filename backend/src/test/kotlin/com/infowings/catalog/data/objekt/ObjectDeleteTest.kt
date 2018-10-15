@file:Suppress("UNUSED_VARIABLE")

package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueUpdateRequest
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail


@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("StringLiteralDuplication")
class ObjectDeleteTest {
    @Autowired
    private lateinit var db: OrientDatabase
    @Autowired
    private lateinit var subjectService: SubjectService
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var objectService: ObjectService
    @Autowired
    private lateinit var objectDaoService: ObjectDaoService

    private lateinit var subject: Subject

    private lateinit var aspect: AspectData
    private lateinit var referenceAspect: AspectData
    private lateinit var complexAspect: AspectData
    private lateinit var aspectInt: AspectData

    private val username = "admin"
    private val sampleDescription = "object description"

    @BeforeEach
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = randomName(), description = "descr"), username)
        aspect = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Text.name), username)
        aspectInt = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Integer.name), username)
        referenceAspect = aspectService.save(
            AspectData(name = randomName(), description = "aspect with reference base type", baseType = BaseType.Reference.name), username
        )
        val property = AspectPropertyData("", "p", aspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val referenceProperty = AspectPropertyData("", "p", referenceAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, listOf(property, referenceProperty))
        complexAspect = aspectService.save(complexAspectData, username)
    }

    private fun createObject(request: ObjectCreateRequest) = objectService.create(request, username)
    private fun newObject(name: String, subjectId: String) = createObject(ObjectCreateRequest(name, sampleDescription, subjectId))
    private fun newObject(name: String) = newObject(name, subject.id)
    private fun newObject() = newObject(randomName(), subject.id)


    private fun checkValueAbsence(id: String) = try {
        val found = objectService.findPropertyValueById(id)
        fail("object property value is found after deletion: $found")
    } catch (e: ObjectPropertyValueNotFoundException) {
    }

    private fun checkValuesAbsence(ids: List<String>) = ids.forEach { checkValueAbsence(it) }

    private fun checkValueSoftAbsence(id: String) {
        val found = objectService.findPropertyValueById(id)
        assertTrue(found.deleted)
    }

    private fun checkValuesSoftAbsence(ids: List<String>) {
        ids.forEach { checkValueSoftAbsence(it) }
    }

    private fun checkPropertyAbsence(id: String) = try {
        val found = objectService.findPropertyById(id)
        fail("object property is found after deletion: $found")
    } catch (e: ObjectPropertyNotFoundException) {
    }

    private fun checkPropertiesAbsence(ids: List<String>) = ids.forEach { checkPropertyAbsence(it) }

    private fun checkPropertySoftAbsence(id: String) {
        val found = objectService.findPropertyById(id)
        assertEquals(true, found.deleted)
    }

    private fun checkObjectAbsense(id: String) = try {
        val found = objectService.findById(id)
        fail("object is found after deletion: $found")
    } catch (e: ObjectNotFoundException) {
    }

    private fun checkObjectSoftAbsense(id: String) {
        val found = objectService.findById(id)
        assertEquals(true, found.deleted)
    }

    @Test
    fun `Soft delete object that is linked by other object`() {
        val builder = ObjectBuilder(objectService)

        val extObject = builder.name(randomName("ext-object")).subject(subject).build()

        val objectName = randomName("object")
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest =
            PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.Link(LinkValueData.Object(extObject.id)),
            null,
            null,
            propertyCreateResponse.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest, username)

        objectService.softDeleteObject(extObject.id, username)

        val v = objectDaoService.getObjectVertex(extObject.id)

        checkObjectSoftAbsense(extObject.id)
    }
}
