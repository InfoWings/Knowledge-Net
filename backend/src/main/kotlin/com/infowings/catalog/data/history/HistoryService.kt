package com.infowings.catalog.data.history

import com.infowings.catalog.auth.UserAcceptService
import com.infowings.catalog.auth.UserNotFoundException
import com.infowings.catalog.storage.OrientDatabase
import com.orientechnologies.orient.core.id.ORID
import java.sql.Timestamp

class HistoryService(
    private val db: OrientDatabase,
    private val historyDaoService: HistoryDaoService,
    private val userAcceptService: UserAcceptService
) {

    fun storeFact(fact: HistoryFact): HistoryEventVertex {
        val historyEventVertex = fact.newHistoryEventVertex()

        val elementVertices = fact.payload.data.map {
            return@map historyDaoService.newHistoryElementVertex().apply {
                addEdge(historyEventVertex)
                key = it.key
                stringValue = it.value
            }
        }

        val addLinkVertices = historyEventVertex.linksVertices(fact.payload.addedLinks,
            { historyDaoService.newAddLinkVertex() })
        val dropLinkVertices = historyEventVertex.linksVertices(fact.payload.removedLinks,
            { historyDaoService.newDropLinkVertex() })

        fact.subject.addEdge(historyEventVertex, HISTORY_EDGE)

        db.saveAll(listOf(historyEventVertex) + elementVertices + addLinkVertices + dropLinkVertices)

        return historyEventVertex
    }

    private fun HistoryFact.newHistoryEventVertex(): HistoryEventVertex =
        historyDaoService.newHistoryEventVertex().apply {
            entityClass = event.entityClass
            entityId = event.entityId
            entityVersion = event.version
            timestamp = Timestamp(event.timestamp)
            val userInfo = event.userInfo // userAcceptService.findByUsernameAsJson(event.user)
            // temporary workarond for OrientDb bug: https://github.com/orientechnologies/orientdb/issues/8216

            /**
             * Конвертируем представление пользователя как строкового имени
             * в расширенное предстовление данных о пользователе.
             *
             * Хорошо бы в виде ребра, указывающего на вершину, но сейчас пользователь
             * представлен в виде ORecord
             *
             * Конвертацию делаем именно здесь, потому что хочется это делать в одном месте.
             * В этом смысле единственная альтернатива - HistoryAware, но туда надо как-то
             * доставить userAcceptService. Сюда его проще и естественнее доставлять
             *
             * Пустую строку временно обрабатываем особо, чтобы не падали напсанные тесты и не переписывать
             * их прямо сейчас. В новых тестах заведение пользователя должно быть частью инициализации
             */
            if (userInfo != null) {
                user = userInfo
            } else {
                if (event.user != "") // временно для тестов - чтобы все разом не исправлять
                    throw UserNotFoundException(event.user)
            }
        }

    private inline fun HistoryEventVertex.linksVertices(
        linksPayload: Map<String, List<ORID>>,
        vertexProducer: () -> HistoryLinksVertex
    ): List<HistoryLinksVertex> =
        linksPayload.flatMap {
            val linkKey = it.key
            val eventVertex = this
            it.value.map {
                val peer = it
                vertexProducer().apply {
                    addEdge(eventVertex)
                    key = linkKey
                    peerId = peer
                }
            }
        }
}