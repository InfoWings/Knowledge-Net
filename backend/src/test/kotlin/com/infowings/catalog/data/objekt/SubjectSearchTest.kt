package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import com.infowings.catalog.search.CommonSuggestionParam
import com.infowings.catalog.search.ObjectSuggestionParam
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.OrientDatabase
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("StringLiteralDuplication")
class ObjectSearchTest {
    @Autowired
    private lateinit var db: OrientDatabase
    @Autowired
    private lateinit var subjectService: SubjectService
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var objectService: ObjectService
    @Autowired
    private lateinit var suggestionService: SuggestionService

    private lateinit var subject: Subject

    private lateinit var aspect: AspectData
    private lateinit var referenceAspect: AspectData
    private lateinit var complexAspect: AspectData

    private val username = "admin"

    @BeforeEach
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = randomName(), description = "descr"), username)
        aspect = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Text.name), username)
        referenceAspect = aspectService.save(
            AspectData(name = randomName(), description = "aspect with reference base type", baseType = BaseType.Reference.name), username
        )
        val property = AspectPropertyData("", "p", aspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val referenceProperty = AspectPropertyData("", "p", referenceAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, listOf(property, referenceProperty))
        complexAspect = aspectService.save(complexAspectData, username)
    }


    @Test
    fun `object should be found by name`() {
        val builder = ObjectBuilder(objectService)

        val objectResponse = builder.name(randomName("ObjectShouldBeFoundByName")).subject(subject).build()

        val result = suggestionService.findObject(CommonSuggestionParam("ObjectShouldBeFoundByName"), ObjectSuggestionParam())

        result.size shouldBe 1

        //res.first() shouldBe subject.toSubjectData()
    }
}
