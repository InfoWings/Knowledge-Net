package com.infowings.catalog.wrappers.react

import kotlinx.html.*
import react.RBuilder
import react.ReactElement
import react.dom.RDOMBuilder
import react.dom.tag

class USE(initialAttributes: Map<String, String>, consumer: TagConsumer<*>) :
        HTMLTag("use", consumer, initialAttributes,
                inlineTag = true,
                emptyTag = false), HtmlInlineTag

fun RDOMBuilder<SVG>.use(href: String): ReactElement = tag({}) { USE(attributesMapOf("xlinkHref", href), it) }

inline fun RBuilder.label(classes: String? = null, htmlFor: String? = null, block: RDOMBuilder<LABEL>.() -> Unit): ReactElement = tag(block) { LABEL(attributesMapOf("class", classes, "htmlFor", htmlFor), it) }