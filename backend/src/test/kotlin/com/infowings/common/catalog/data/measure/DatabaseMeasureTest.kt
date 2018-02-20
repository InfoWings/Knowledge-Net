package com.infowings.common.catalog.data.measure

import com.infowings.common.catalog.MasterCatalog
import com.infowings.common.catalog.data.*
import com.infowings.common.catalog.storage.OrientDatabase
import com.orientechnologies.orient.core.record.ODirection
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DatabaseMeasureTest {

    @Autowired
    lateinit var measureService: MeasureService

    @Autowired
    lateinit var database: OrientDatabase

    @Test
    fun everyGroupExist() = database.acquire().use { db ->
        MeasureGroupMap.values.forEach {
            assertTrue("${it.name} group must exist", measureService.findMeasureGroup(it.name, db) != null)
        }
    }

    @Test
    fun everyGroupBaseMeasureExist() = database.acquire().use { db ->
        MeasureGroupMap.values.forEach {
            val baseVertex = measureService.findMeasure(it.base.name, db)
            val groupVertex = measureService.findMeasureGroup(it.name, db)
            assertTrue("${it.name} base measure must exist", baseVertex != null)
            assertTrue("${it.name} base measure must be linked with ${it.name} group",
                baseVertex!!.getVertices(ODirection.BOTH).contains(groupVertex!!)
            )
        }
    }

    @Test
    fun everyGroupContainsAllTheirMeasures() = database.acquire().use { db ->
        MeasureGroupMap.values.forEach { group ->
            val baseVertex = measureService.findMeasure(group.base.name, db)
            group.measureList.forEach { measure ->
                assertTrue("Measure $measure must exist", measureService.findMeasure(measure.name, db) != null)
                assertTrue("Measure ${measure.name} must be linked with ${group.base.name}",
                    measureService.findMeasure(measure.name, db)!!.getVertices(ODirection.OUT).contains(baseVertex!!)
                )
            }
        }
    }


    @Test
    fun measureDirectDependencies() = database.acquire().use {
        val lengthGroupVertex = measureService.findMeasureGroup(LengthGroup.name, it)
        val speedGroupVertex = measureService.findMeasureGroup(SpeedGroup.name, it)
        assertTrue("Length group must be linked with Speed group",
            lengthGroupVertex!!.getVertices(ODirection.BOTH).contains(speedGroupVertex!!)
        )
    }


    @Test
    fun measureTransitiveDependencies() = database.acquire().use {
        val lengthGroupVertex = measureService.findMeasureGroup(LengthGroup.name, it)
        val pressureGroupVertex = measureService.findMeasureGroup(PressureGroup.name, it)
        assertTrue("Length group must not be linked with Pressure group directly",
            !lengthGroupVertex!!.getVertices(ODirection.BOTH).contains(pressureGroupVertex!!)
        )
        assertTrue("Length group must be linked by another vertex with Pressure group",
            lengthGroupVertex.getVertices(ODirection.BOTH).flatMap { it.getVertices(ODirection.BOTH) }.contains(pressureGroupVertex)
        )
    }
}