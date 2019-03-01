package com.infowings.catalog

import com.infowings.catalog.data.MEASURE_GROUP_VERTEX
import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import kotlin.test.assertEquals


@ExtendWith(SpringExtension::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD, methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
class DbTest {
    @Autowired
    lateinit var database: OrientDatabase

    @Test
    fun indexesTest() {
        listOf(ASPECT_CLASS, SUBJECT_CLASS, MEASURE_VERTEX, MEASURE_GROUP_VERTEX).forEach { className ->
            val indexes = database.luceneIndexesOf(className)
            assertEquals(2, indexes.size, "class $className")
        }

        listOf(SUBJECT_CLASS, MEASURE_VERTEX, MEASURE_GROUP_VERTEX).forEach { className ->
            val indexes = database.sbTreeIndexesOf(className)
            assertEquals(2, indexes.size, "class $className")
        }

        listOf(OBJECT_CLASS).forEach { className ->
            val indexes = database.sbTreeIndexesOf(className)
            assertEquals(1, indexes.size, "class $className")
        }

        listOf(ASPECT_CLASS).forEach { className ->
            val indexes = database.sbTreeIndexesOf(className)
            assertEquals(2, indexes.size, "class $className")
        }
    }

    @Test
    fun indexRemoveTest() {
        listOf(ASPECT_CLASS, SUBJECT_CLASS, MEASURE_VERTEX, MEASURE_GROUP_VERTEX).forEach { className ->
            val indexes = database.luceneIndexesOf(className)
            indexes.forEachIndexed { i, index ->
                val indexName = index.name
                database.removeIndex(className, indexName)
                val remained = indexes.drop(i + 1).map { it.name }
                val indexesAfter = database.luceneIndexesOf(className)
                assertEquals(remained, indexesAfter.map { it.name }, "class $className, index $indexName")
            }
        }

        listOf(ASPECT_CLASS, SUBJECT_CLASS, OBJECT_CLASS, MEASURE_VERTEX, MEASURE_GROUP_VERTEX).forEach { className ->
            val indexes = database.sbTreeIndexesOf(className)
            indexes.forEachIndexed { i, index ->
                database.removeIndex(className, index.name)
                val remainedNames = indexes.drop(i + 1).map { it.name }
                val indexesAfter = database.sbTreeIndexesOf(className)
                assertEquals(remainedNames, indexesAfter.map { it.name }, "class $className, index ${index.name}")
            }
        }
    }

    @Test
    fun indexResetTest() {
        listOf(ASPECT_CLASS, SUBJECT_CLASS, MEASURE_VERTEX, MEASURE_GROUP_VERTEX).forEach { className ->
            val indexes = database.luceneIndexesOf(className)
            indexes.forEachIndexed { _, index ->
                val indexName = index.name
                database.resetLuceneIndex(className, indexName)
                val indexesAfter = database.luceneIndexesOf(className)
                assertEquals(indexes.map { it.name }, indexesAfter.map { it.name }, "class $className, index $indexName")
            }
        }

        val testKey = "TEST_KEY"

        listOf(ASPECT_CLASS, SUBJECT_CLASS, OBJECT_CLASS, MEASURE_VERTEX, MEASURE_GROUP_VERTEX).forEach { className ->
            val indexes = database.sbTreeIndexesOf(className)
            indexes.forEachIndexed { _, index ->
                val indexName = index.name

                val indexNameComponents = indexName.split(".")

                val fieldName = if (indexNameComponents.size == 4) indexNameComponents[2] else indexNameComponents[1]

                val vertex = database.createNewVertex(className)

                // todo: #395
                if (className == ASPECT_CLASS) {
                    vertex.assignGuid()
                    vertex.setProperty(ATTR_NAME, UUID.randomUUID().toString())
                }

                transaction(database) {
                    vertex.setProperty(fieldName, testKey)
                    vertex.save<OVertex>()
                }

                val sizeBeforeRemove = database.indexSize(index)

                transaction(database) { index.remove(testKey) }

                val sizeBeforeReset = database.indexSize(index)

                val newIndex = database.resetSbTreeIndex(className, indexName)

                val sizeAfterReset = database.indexSize(newIndex)

                val indexesAfter = database.sbTreeIndexesOf(className)
                assertEquals(indexes.map { it.name }, indexesAfter.map { it.name }, "class $className, index $indexName")
                assertEquals(sizeBeforeRemove, sizeBeforeReset + 1, "class $className, index $indexName")
                assertEquals(sizeBeforeRemove, sizeAfterReset, "class $className, index $indexName")
            }
        }
    }
}