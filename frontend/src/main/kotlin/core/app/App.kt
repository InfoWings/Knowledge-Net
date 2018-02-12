package core.app


import core.process.processView
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import kotlinx.serialization.json.JSON as KJSON


class App : RComponent<RProps, RState>() {

    override fun RBuilder.render() {
        processView()
    }
}

fun RBuilder.app() = child(App::class) {}
