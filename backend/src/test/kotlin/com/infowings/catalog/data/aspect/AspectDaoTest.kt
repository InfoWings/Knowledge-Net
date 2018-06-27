package com.infowings.catalog.data.aspect

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.BaseType.Boolean
import com.infowings.catalog.common.BaseType.Decimal
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.toSubjectData
import com.orientechnologies.orient.core.id.ORecordId
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assert
import kotlin.test.assertEquals


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

class AspectDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectDao: AspectDaoService

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var subjectService: SubjectService

    @Test
    fun testGetDetailsPlain() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val createdAspect: AspectData = aspectService.save(ad, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(createdAspect.id)))

        assertEquals(
            setOf(createdAspect.id), details.keys
        )

        val aspectId = createdAspect.id ?: throw IllegalStateException("aspect id is null")
        val aspectDetails = details.getValue(aspectId)
    }

    @Test
    fun testGetDetailsPlainTwo() {
        val ad1 = AspectData("", "newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val ad2 = AspectData("", "newAspect-2", Kilometre.name, null, Decimal.name, emptyList())
        val created1: AspectData = aspectService.save(ad1, username)
        val created2: AspectData = aspectService.save(ad2, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(created1, created2).map { ORecordId(it.id) })

        assertEquals(
            setOf(created1.id, created2.id), details.keys
        )
    }
}