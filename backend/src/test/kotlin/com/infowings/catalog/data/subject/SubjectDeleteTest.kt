package com.infowings.catalog.data.subject

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.data.SubjectIsLinked
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.fail

@ExtendWith(SpringExtension::class)
@SpringBootTest
class SubjectServiceDeleteTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    lateinit var objectService: ObjectService

    @Autowired
    lateinit var db: OrientDatabase

    @Test
    fun `Delete subject with linked object`() {
        val subjectName = randomName()
        val subject = subjectService.createSubject(SubjectData.Initial(subjectName), username)

        val aspectName = randomName()
        val aspect = aspectService.save(AspectData.initial(aspectName).copy(baseType = BaseType.Decimal.name), username)

        val objectName = randomName()
        val objectCreateResponse = objectService.create(ObjectCreateRequest.simple(name = randomName(), subjectId = subject.id), username)
        try {
            subjectService.remove(subject.toSubjectData(), username)
            fail("nothing is thrown")
        } catch (e: SubjectIsLinked) {
            assertEquals(emptyList(), e.aspectGuids)
            assertEquals(listOfNotNull(objectCreateResponse.guid), e.objectGuids)
        }
    }

}
