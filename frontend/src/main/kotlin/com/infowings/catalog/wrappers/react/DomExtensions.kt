package com.infowings.catalog.wrappers.react

import kotlinext.js.assign
import kotlinx.html.*
import react.Component
import react.RBuilder
import react.RState
import react.ReactElement
import react.dom.RDOMBuilder
import react.dom.tag
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Custom SVG tag for inlining all svg icons (sets default attributes like version, xmlns, viewBox)
 */
inline fun RBuilder.svg(
    classes: String? = null,
    viewBox: String = "0 0 20 20",
    block: RDOMBuilder<SVG>.() -> Unit
): ReactElement =
    tag(block) {
        SVG(
            attributesMapOf(
                "class",
                classes,
                "version",
                "1.1",
                "xmlns",
                "http://www.w3.org/2000/svg",
                "viewBox",
                viewBox
            ), it
        )
    }

/**
 * Custom USE tag to use external svg resources (sprites)
 */
class USE(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag(
        "use", consumer, initialAttributes,
        inlineTag = true,
        emptyTag = false
    ), HtmlInlineTag

fun RDOMBuilder<SVG>.use(href: String): ReactElement = tag({}) { USE(attributesMapOf("xlinkHref", href), it) }

/**
 * Custom PATH tag to inline svg icons in SVG tags
 */
class PATH(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
    HTMLTag(
        "path", consumer, initialAttributes,
        inlineTag = true,
        emptyTag = false
    ), HtmlInlineTag

fun RDOMBuilder<SVG>.path(path: String): ReactElement = tag({}) { PATH(attributesMapOf("d", path), it) }

/**
 * Custom label builder that supports 'htmlFor' attribute instead of for in react
 */
inline fun RBuilder.label(
    classes: String? = null,
    htmlFor: String? = null,
    block: RDOMBuilder<LABEL>.() -> Unit
): ReactElement = tag(block) { LABEL(attributesMapOf("class", classes, "htmlFor", htmlFor), it) }


/**
 * Extension for setState with additional callback. Intended to be used to make suspended calls inside coroutines
 */
fun <S : RState> Component<*, S>.setStateWithCallback(callback: () -> Unit, buildState: S.() -> Unit) =
    setState({ assign(it, buildState) }, callback)

suspend fun <S : RState> Component<*, S>.suspendSetState(buildState: S.() -> Unit) =
    suspendCoroutine { cont: Continuation<Unit> ->
        setStateWithCallback({ cont.resume(Unit) }, buildState)
    }
