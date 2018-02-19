package core.process

import com.infowings.common.catalog.SubjectInstanceDto
import core.process.tree.tree
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import kotlin.browser.window

class ProcessView : RComponent<RProps, ProcessState>() {

    var timerID: Int? = null

    override fun componentDidMount() {
        timerID = window.setInterval({
            // actually, the operation is performed on a state's copy, so it stays effectively immutable
            getStatus()
        }, 5000)
    }

    override fun componentWillUnmount() {
        window.clearInterval(timerID!!)
    }

    init {
        state.subjectArray = emptyArray()
    }

    private fun getStatus() {
//        launch {
//            try {
//                getAndParseResult("/subject/parts", null) {
//
//                    val newWall = (it as Array<dynamic>)
//                            .map { JSON.stringify(it) }
//                            .foldRight(ArrayList<SubjectInstanceDto>()) { y: String, x: ArrayList<SubjectInstanceDto> ->
//                                x.add(kotlinx.serialization.json.JSON.parse<SubjectInstanceDto>(y))
//                                x
//                            }
//
//                    setState {
//                        subjectArray = newWall.toTypedArray()
//                    }
//                }
//            } catch (e: Exception) {
//                setState {
//                    subjectArray = emptyArray()
//                }
//            }
//        }
    }

    override fun RBuilder.render() {
        tree()
    }
}

external interface ProcessState : RState {
    var subjectArray: Array<SubjectInstanceDto>
}

fun RBuilder.processView() = child(ProcessView::class) {}