package com.infowings.catalog.data.history

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.record.impl.ODocument
import org.hamcrest.core.Is
import org.junit.Assert
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
class SnapshotTest {
    private fun initVertex(session: ODatabaseDocument, name: String) =
        session.getClass(name) ?: session.createVertexClass(name)

    val EmptyDiffPayload = DiffPayload(emptyMap(), emptyMap(), emptyMap())

    @Autowired
    lateinit var database: OrientDatabase

    var ids: List<ORID> = emptyList()

    @Before
    fun addAspectWithProperty() {
        val data = session(database) {
            initVertex(it, "SOME_CLASS")
        }
        println(data)

        val vertices = transaction(database) {
            val verts = listOf(
                it.newVertex("SOME_CLASS"), it.newVertex("SOME_CLASS")
            )
            for (v in verts) {
                v.save<OVertex>()
            }
            return@transaction verts
        }
        ids = vertices.map {it?.identity ?: throw IllegalStateException("null identity")}
    }

    @Test
    fun diffSnapshotsSame() {
        println(ids)

        val snapshots = listOf(
            Snapshot(emptyMap(), emptyMap()),
            Snapshot(mapOf("name" to "value"), emptyMap()),
            Snapshot(mapOf("name" to "value"), mapOf("property" to listOf(ODocument().identity)))
        )

        for (s in snapshots) {
            val diffCopy = diffShapshots(s, s.copy())
            Assert.assertThat("(1) diff should be empty", diffCopy, Is.`is`(EmptyDiffPayload))

            val diff = diffShapshots(s, s.copy())
            Assert.assertThat("(2) diff should be empty", diff, Is.`is`(EmptyDiffPayload))
        }
    }


    private fun diffDataPayload(key: String, value: String) = DiffPayload(mapOf(key to value), emptyMap(), emptyMap())

    @Test
    fun diffSnapshotsUpdatedDataElement() {
        val snapshots = listOf(
            Pair(Snapshot(mapOf("name" to "value"), emptyMap()), Snapshot(mapOf("name" to "value2"), emptyMap())),
            Pair(Snapshot(mapOf("name" to "value", "other" to "another"), emptyMap()),
                Snapshot(mapOf("name" to "value2", "other" to "another"), emptyMap()))
        )

        for (s in snapshots) {
            val expected = diffDataPayload("name", s.second.data.getValue("name"))
            val diffCopy = diffShapshots(s.first, s.second)
            Assert.assertThat("diff should contain one changed value and nothing more", diffCopy, Is.`is`(expected))

            val expectedRev = diffDataPayload("name", s.first.data.getValue("name"))
            val diffCopyRev = diffShapshots(s.second, s.first)
            Assert.assertThat("diff should contain one changed value and nothing more", diffCopyRev, Is.`is`(expectedRev))
        }
    }

    @Test
    fun diffSnapshotsNewDataElement() {
        val snapshots = listOf(
            Pair(Snapshot(emptyMap(), emptyMap()), Snapshot(mapOf("name" to "value2"), emptyMap())),
            Pair(Snapshot(mapOf("other" to "another"), emptyMap()),
                Snapshot(mapOf("name" to "value2", "other" to "another"), emptyMap()))
        )

        for (s in snapshots) {
            val expected = diffDataPayload("name", s.second.data.getValue("name"))
            val diffCopy = diffShapshots(s.first, s.second)
            Assert.assertThat("diff should contain one changed value and nothing more", diffCopy, Is.`is`(expected))
        }
    }
}