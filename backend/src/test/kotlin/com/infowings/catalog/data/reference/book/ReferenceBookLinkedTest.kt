package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.objekt.ObjectService
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReferenceBookLinkedTest {
    @Autowired
    private lateinit var dao: ReferenceBookDao
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var referenceBookService: ReferenceBookService
    @Autowired
    private lateinit var subjectService: SubjectService
    @Autowired
    private lateinit var objectService: ObjectService

    private lateinit var refBook: ReferenceBook

    private val username = "admin"

    @Before
    fun initTestData() {
        val ad2 = AspectData("", "leaf2", null, null, BaseType.Text.name, emptyList())
        val leafAspect = aspectService.save(ad2, username)
        refBook = referenceBookService.createReferenceBook("Example", leafAspect.id!!, username)
    }

    @get:Rule
    val thrown = ExpectedException.none()

    @Test
    fun updateLinkedReferenceBookItem() {
        val childId = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("layer1_child1"), username)
        addLinkToRefBookItem(childId)
        val forUpdateItem = ReferenceBookItem(childId, "new", null, emptyList(), false, refBook.version)
        thrown.expect(RefBookItemHasLinkedEntitiesException::class.java)
        referenceBookService.updateReferenceBookItem(forUpdateItem, username)
        referenceBookService.updateReferenceBookItem(forUpdateItem, username, true)

        val updatedItem = referenceBookService.getReferenceBookItem(childId)
        Assert.assertEquals("with force values must changed", forUpdateItem.value, updatedItem.value)
    }

    @Test
    fun updateParentOfLinkedReferenceBookItem() {
        val layer1Child = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("layer1_child1"), username)
        val layer2Child = referenceBookService.addReferenceBookItem(layer1Child, createReferenceBookItem("layer2_child1"), username)
        addLinkToRefBookItem(layer2Child)
        val forUpdateItem = referenceBookService.getReferenceBookItem(layer1Child).copy(value = "new")
        referenceBookService.updateReferenceBookItem(forUpdateItem, username)

        val updatedItem = referenceBookService.getReferenceBookItem(layer1Child)
        Assert.assertEquals("value must changed", forUpdateItem.value, updatedItem.value)
    }

    @Test
    fun updateChildOfLinkedReferenceBookItem() {
        val layer1Child = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("layer1_child1"), username)
        val layer2Child = referenceBookService.addReferenceBookItem(layer1Child, createReferenceBookItem("layer2_child1"), username)
        addLinkToRefBookItem(layer1Child)
        val forUpdateItem = referenceBookService.getReferenceBookItem(layer2Child).copy(value = "new")
        referenceBookService.updateReferenceBookItem(forUpdateItem, username)

        val updatedItem = referenceBookService.getReferenceBookItem(layer2Child)
        Assert.assertEquals("value must changed", forUpdateItem.value, updatedItem.value)
    }

    @Test
    fun moveLinkedReferenceBookItem() {
        val layer1Child1 = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("layer1_child1"), username)
        val layer1Child2 = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("layer1_child2"), username)
        addLinkToRefBookItem(layer1Child1)
        referenceBookService.moveReferenceBookItem(
            referenceBookService.getReferenceBookItem(layer1Child1),
            referenceBookService.getReferenceBookItem(layer1Child2),
            username
        )

        val parent = referenceBookService.getReferenceBookItem(layer1Child2)
        val child = referenceBookService.getReferenceBookItem(layer1Child1)
        Assert.assertTrue("move should be correct", parent.children.contains(child))
    }

    @Test
    fun removeLinkedItem() {
        val child = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("child"), username)
        addLinkToRefBookItem(child)
        thrown.expect(RefBookItemHasLinkedEntitiesException::class.java)
        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child), username)

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child), username, true)
        val deleted = referenceBookService.getReferenceBook(child)
        Assert.assertTrue("deleted book item must have deleted flag", deleted.deleted)
    }

    @Test
    fun removeParentLinkedItem() {
        val layer1Child = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("layer1_child1"), username)
        val layer2Child = referenceBookService.addReferenceBookItem(layer1Child, createReferenceBookItem("layer2_child1"), username)

        addLinkToRefBookItem(layer2Child)
        thrown.expect(RefBookItemHasLinkedEntitiesException::class.java)
        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(layer1Child), username)

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(layer1Child), username, true)

        val deletedParent = referenceBookService.getReferenceBook(layer1Child)
        Assert.assertTrue("deleted parent book item must have deleted flag", deletedParent.deleted)

        val deletedChild = referenceBookService.getReferenceBook(layer2Child)
        Assert.assertTrue("deleted child book item must have deleted flag", deletedChild.deleted)
    }

    private fun createReferenceBookItem(value: String): ReferenceBookItem {
        return ReferenceBookItem(
            "",
            value,
            null,
            emptyList(),
            false,
            0
        )
    }

    private fun addLinkToRefBookItem(idForLinking: String) {
        val leafAspect = aspectService.findById(refBook.aspectId)
        val ap2 = AspectPropertyData(name = "ap1", cardinality = PropertyCardinality.ONE.name, aspectId = leafAspect.id!!, id = "", description = "")
        val ad3 = AspectData("", "aspectWithObjectProperty", Kilometre.name, null, BaseType.Decimal.name, listOf(ap2))
        val aspectWithObjectProperty = aspectService.save(ad3, username)

        val subject = subjectService.createSubject(SubjectData(name = "subject", description = null), username)
        val obj = objectService.create(ObjectCreateRequest("obj", null, subject.id, subject.version), username)
        val objProperty = objectService.create(PropertyCreateRequest(obj, "prop", null, PropertyCardinality.ONE.name, aspectWithObjectProperty.id!!), username)
        val objPropertyValueRequest = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.DomainElement(idForLinking)),
            objectPropertyId = objProperty,
            aspectPropertyId = aspectWithObjectProperty.properties[0].id,
            measureId = null,
            parentValueId = null
        )
        objectService.create(objPropertyValueRequest, username)
        refBook = referenceBookService.getReferenceBook(refBook.aspectId)
    }
}