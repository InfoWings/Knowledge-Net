@file:Suppress("UNUSED_VARIABLE")

package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.guid.GuidService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

import kotlin.test.assertEquals


@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("StringLiteralDuplication")
class ObjectMeasureTest {
    @Autowired
    private lateinit var db: OrientDatabase
    @Autowired
    private lateinit var subjectService: SubjectService
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var objectService: ObjectService
    @Autowired
    private lateinit var guidService: GuidService

    private lateinit var subject1: Subject
    private lateinit var subject2: Subject

    private lateinit var decimalAspect: AspectData
    private lateinit var referenceAspect: AspectData
    private lateinit var complexAspect: AspectData

    private val username = "admin"
    private val propDescription = "prop description"

    @BeforeEach
    fun initTestData() {
        subject1 = subjectService.createSubject(SubjectData(name = randomName("Subject1"), description = "descr"), username)
        subject2 = subjectService.createSubject(SubjectData(name = randomName("Subject2"), description = "descr"), username)

        decimalAspect = aspectService.save(
            AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Decimal.name, measure = Millimetre.name),
            username
        )
        referenceAspect = aspectService.save(
            AspectData(name = randomName(), description = "aspect with reference base type", baseType = BaseType.Reference.name), username
        )
    }

    @Test
    fun correctMeasureById() {
        val request = ObjectCreateRequest(randomName(), "object descr", subject1.id)
        val objectCreateResponse = objectService.create(request, username)
        val stringRepr = "14"
        val measure = Millimetre

        val decimalPropResponse = objectService.create(
            PropertyCreateRequest(objectCreateResponse.id, randomName("Decimal"), propDescription, decimalAspect.idStrict()), username
        )
        val referencePropResponse = objectService.create(
            PropertyCreateRequest(objectCreateResponse.id, randomName("Reference"), propDescription, referenceAspect.idStrict()), username
        )

        val decimalValueResponse = objectService.create(
            ValueCreateRequest(ObjectValueData.DecimalValue(stringRepr), null, decimalPropResponse.id, measure.name), username
        )

        val guid = decimalValueResponse.guid ?: throw IllegalStateException()

        val foundByGuid = guidService.findObjectValue(guid)

        val foundById = guidService.findObjectValueById(decimalValueResponse.id)

        val foundViaObject = objectService.getDetailedObject(objectCreateResponse.id).objectPropertyViews.first().values[1]

        assertEquals(stringRepr, foundByGuid.value.decimalStrict())
        assertEquals(stringRepr, foundById.value.decimalStrict())
        assertEquals(stringRepr, foundViaObject.value.decimalStrict())

        assertEquals(measure.symbol, foundByGuid.measure)
        assertEquals(measure.symbol, foundById.measure)
        assertEquals(measure.symbol, foundViaObject.measureSymbol)
    }
}