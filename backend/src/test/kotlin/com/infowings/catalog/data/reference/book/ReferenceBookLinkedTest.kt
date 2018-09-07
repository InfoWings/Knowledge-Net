package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.common.objekt.ValueUpdateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.randomName
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [MasterCatalog::class])
@Suppress("UnsafeCallOnNullableType")
class ReferenceBookLinkedTest {
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

    @BeforeEach
    fun initTestData() {
        val ad2 = AspectData("", randomName(), null, null, BaseType.Text.name, emptyList())
        val leafAspect = aspectService.save(ad2, username)
        refBook = referenceBookService.createReferenceBook("Example", leafAspect.id!!, username)
    }

    @Test
    fun updateLinkedReferenceBookItem() {
        val childId = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("layer1_child1"), username)
        addLinkToRefBookItem(childId)
        val forUpdateItem = ReferenceBookItem(childId, "new", null, emptyList(), false, refBook.version, null)
        assertThrows<RefBookItemHasLinkedEntitiesException> {
            referenceBookService.updateReferenceBookItem(forUpdateItem, username)
        }
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
        val child = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("removeLinkedItem_child"), username)
        addLinkToRefBookItem(child)
        assertThrows<RefBookItemHasLinkedEntitiesException> {
            referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child), username)
        }

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child), username, true)
        val deleted = referenceBookService.getReferenceBookItem(child)
        Assert.assertTrue("deleted book item must have deleted flag", deleted.deleted)
    }

    @Test
    fun removeParentLinkedItem() {
        val layer1Child = referenceBookService.addReferenceBookItem(refBook.id, createReferenceBookItem("layer1_child1"), username)
        val layer2Child = referenceBookService.addReferenceBookItem(layer1Child, createReferenceBookItem("layer2_child1"), username)

        addLinkToRefBookItem(layer2Child)
        assertThrows<RefBookItemHasLinkedEntitiesException> {
            referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(layer1Child), username)
        }
        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(layer1Child), username, true)

        val deletedParent = referenceBookService.getReferenceBookItem(layer1Child)
        Assert.assertTrue("deleted parent book item must have deleted flag", deletedParent.deleted)

        val deletedChild = referenceBookService.getReferenceBookItem(layer2Child)
        Assert.assertTrue("deleted child book item must have deleted flag", deletedChild.deleted)
    }

    private fun createReferenceBookItem(value: String): ReferenceBookItem {
        return ReferenceBookItem(
            "",
            value,
            null,
            emptyList(),
            false,
            0,
            null
        )
    }

    private fun addLinkToRefBookItem(idForLinking: String) {
        val leafAspect = aspectService.findById(refBook.aspectId)
        val ap2 = AspectPropertyData(name = "ap1", cardinality = PropertyCardinality.ONE.name, aspectId = leafAspect.id!!, id = "", description = "")
        val ad3 = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, listOf(ap2))
        val aspectWithObjectProperty = aspectService.save(ad3, username)

        val subject = subjectService.createSubject(SubjectData(name = randomName(), description = null), username)
        val objectCreateResponse = objectService.create(ObjectCreateRequest("obj", null, subject.id), username)
        val propertyCreateResponse = objectService.create(PropertyCreateRequest(objectCreateResponse.id, "prop", null, aspectWithObjectProperty.id!!), username)
        val objPropertyRootValueRequest = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.DecimalValue("123.1"),
            Kilometre.name,
            null,
            propertyCreateResponse.rootValue.version
        )
        val rootValueUpdateResponse = objectService.update(objPropertyRootValueRequest, username)
        val objPropertyValueRequest = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.DomainElement(idForLinking)),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = aspectWithObjectProperty.properties[0].id,
            parentValueId = rootValueUpdateResponse.id
        )
        objectService.create(objPropertyValueRequest, username)
        refBook = referenceBookService.getReferenceBook(refBook.aspectId)
    }
}