package com.infowings.catalog.data.measure

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.data.MEASURE_BASE_AND_GROUP_EDGE
import com.infowings.catalog.data.MEASURE_BASE_EDGE
import com.infowings.catalog.data.MEASURE_GROUP_EDGE
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.infowings.common.LengthGroup
import com.infowings.common.MeasureGroupMap
import com.infowings.common.PressureGroup
import com.infowings.common.SpeedGroup
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
    fun everyGroupExist() = session(database) {
        MeasureGroupMap.values.forEach {
            assertTrue("${it.name} group must exist", measureService.findMeasureGroup(it.name) != null)
        }
    }

    @Test
    fun everyGroupBaseMeasureExist() = session(database) {
        MeasureGroupMap.values.forEach {
            val baseVertex = measureService.findMeasure(it.base.name)
            val groupVertex = measureService.findMeasureGroup(it.name)
            assertTrue("${it.name} base measure must exist", baseVertex != null)
            assertTrue("${it.name} base measure must be linked with ${it.name} group",
                    baseVertex!!.getVertices(ODirection.BOTH, MEASURE_BASE_AND_GROUP_EDGE).contains(groupVertex!!)
            )
        }
    }

    @Test
    fun everyGroupContainsAllTheirMeasures() = session(database) {
        MeasureGroupMap.values.forEach { group ->
            val baseVertex = measureService.findMeasure(group.base.name)
            group.measureList.forEach { measure ->
                assertTrue("Measure $measure must exist", measureService.findMeasure(measure.name) != null)
                assertTrue("Measure ${measure.name} must be linked with ${group.base.name}",
                        measureService.findMeasure(measure.name)!!.getVertices(ODirection.OUT, MEASURE_BASE_EDGE).contains(baseVertex!!)
                )
            }
        }
    }


    @Test
    fun measureDirectDependencies() = session(database) {
        val lengthGroupVertex = measureService.findMeasureGroup(LengthGroup.name)
        val speedGroupVertex = measureService.findMeasureGroup(SpeedGroup.name)
        assertTrue("Length group must be linked with Speed group",
                lengthGroupVertex!!.getVertices(ODirection.BOTH, MEASURE_GROUP_EDGE).contains(speedGroupVertex!!)
        )
    }


    @Test
    fun measureTransitiveDependencies() = session(database) {
        val lengthGroupVertex = measureService.findMeasureGroup(LengthGroup.name)
        val pressureGroupVertex = measureService.findMeasureGroup(PressureGroup.name)
        assertTrue("Length group must not be linked with Pressure group directly",
                !lengthGroupVertex!!.getVertices(ODirection.BOTH, MEASURE_GROUP_EDGE).contains(pressureGroupVertex!!)
        )
        assertTrue("Length group must be linked by another vertex with Pressure group",
                lengthGroupVertex.getVertices(ODirection.BOTH, MEASURE_GROUP_EDGE).flatMap { it.getVertices(ODirection.BOTH) }
                        .contains(pressureGroupVertex)
        )
    }
}