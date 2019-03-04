package com.infowings.catalog.data.guid

import com.infowings.catalog.common.*
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
@SpringBootTest
class GuidDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var refBookService: ReferenceBookService

    @Autowired
    lateinit var guidDao: GuidDaoService

    @Autowired
    lateinit var db: OrientDatabase

    lateinit var baseAspect: AspectData
    lateinit var complexAspect: AspectData
    lateinit var subject: Subject

    @BeforeEach
    fun initData() {
        val ad = AspectData("", randomName("base"), Kilometre.name, null, BaseType.Decimal.name, emptyList())
        baseAspect = aspectService.save(ad, username)

        val property = AspectPropertyData("", randomName("p"), baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData("", randomName("complex"), Kilometre.name, null, BaseType.Decimal.name, listOf(property))
        complexAspect = aspectService.save(ad2, username)

        subject = subjectService.createSubject(SubjectData.Initial(name = randomName("subject")), username)
    }

    @Test
    fun testGuidDaoUnique() {
        assertTrue(baseAspect.guid != null)
        assertTrue(complexAspect.guid != null)
        assertTrue(complexAspect.properties[0].guid != null)
        assertTrue(subject.guid != null)
    }

}
