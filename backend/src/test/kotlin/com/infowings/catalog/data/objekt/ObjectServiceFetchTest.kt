package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.data.SubjectService
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

    private val username = "admin"

    @Before
    fun initTestData() {
        val knetSubject = subjectService.createSubject(SubjectData(name = "Knowledge Net", description = null), username)
        val reflexiaSubject = subjectService.createSubject(SubjectData(name = "Reflexia", description = null), username)

        objectService.create(
            ObjectCreateRequest(name = "Box V1", description = null, subjectId = knetSubject.id, subjectVersion = knetSubject.version),
            username
        )
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

    //TEST :: What if there is no subject for existing object???
}