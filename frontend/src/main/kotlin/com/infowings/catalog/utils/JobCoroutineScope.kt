package com.infowings.catalog.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

interface JobCoroutineScope : CoroutineScope {
    var job: Job
}

/**
 * Encapsulates simple logic from [CoroutineScope]
 *
 * don't forget to create and cancel [job]
 *
 * ```
 * class SomeComponent: JobCoroutineScope by JobSimpleCoroutineScope(){
 *  override fun componentWillUnmount() {
 *      job.cancel()
 *  }
 *
 *  override fun componentDidMount() {
 *      job = Job()
 *  }
 */
class JobSimpleCoroutineScope : JobCoroutineScope {
    override lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}
