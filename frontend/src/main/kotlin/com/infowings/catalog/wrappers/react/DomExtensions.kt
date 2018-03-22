package com.infowings.catalog.wrappers.react

import kotlinx.html.*
import react.RBuilder
import react.ReactElement
import react.dom.RDOMBuilder
import react.dom.tag

/**
 * Custom SVG tag for inlining all svg icons (sets default attributes like version, xmlns, viewBox)
 */
inline fun RBuilder.svg(classes: String? = null, block: RDOMBuilder<SVG>.() -> Unit): ReactElement =
        tag(block) { SVG(attributesMapOf("class", classes, "version", "1.1", "xmlns", "http://www.w3.org/2000/svg", "viewBox", "0 0 20 20"), it) }

/**
 * Custom USE tag to use external svg resources (sprites)
 */
class USE(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
        HTMLTag("use", consumer, initialAttributes,
                inlineTag = true,
                emptyTag = false), HtmlInlineTag

fun RDOMBuilder<SVG>.use(href: String): ReactElement = tag({}) { USE(attributesMapOf("xlinkHref", href), it) }

/**
 * Custom PATH tag to inline svg icons in SVG tags
 */
class PATH(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
        HTMLTag("path", consumer, initialAttributes,
                inlineTag = true,
                emptyTag = false), HtmlInlineTag

fun RDOMBuilder<SVG>.path(path: String): ReactElement = tag({}) { PATH(attributesMapOf("d", path), it) }

/**
 * Custom label builder that supports 'htmlFor' attribute instead of for in react
 */
inline fun RBuilder.label(classes: String? = null, htmlFor: String? = null, block: RDOMBuilder<LABEL>.() -> Unit): ReactElement = tag(block) { LABEL(attributesMapOf("class", classes, "htmlFor", htmlFor), it) }