package com.infowings.catalog.data.history

import com.infowings.catalog.auth.UserAcceptService
import com.infowings.catalog.auth.UserEntity
import com.infowings.catalog.auth.UserNotFoundException
import com.infowings.catalog.common.EventType
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.ODirection
import kotlinx.serialization.json.JSON
import java.time.Instant

class HistoryService(
    private val db: OrientDatabase,
    private val historyDao: HistoryDao,
    private val userAcceptService: UserAcceptService
) {

    fun getAll(): Set<HistoryFactDto> = transaction(db) {
        return@transaction historyDao.getAllHistoryEvents()
            .map {
                val event = HistoryEvent(
                    JSON.nonstrict.parse<UserEntity>(it.user).username,
                    it.timestamp.toEpochMilli(),
                    it.entityVersion,
                    EventType.valueOf(it.eventType),
                    it.entityRID,
                    it.entityClass
                )
                val data = it.getVertices(ODirection.OUT, HISTORY_ELEMENT_EDGE)
                    .map { it.toHistoryElementVertex() }
                    .map { it.key to it.stringValue }
                    .toMap()

                val addedLinks = it.getVertices(ODirection.OUT, HISTORY_ADD_LINK_EDGE)
                    .map { it.toHistoryLinksVertex() }
                    .groupBy { it.key }
                    .map { it.key to it.value.map { it.peerId } }
                    .toMap()

                val removedLinks = it.getVertices(ODirection.OUT, HISTORY_DROP_LINK_EDGE)
                    .map { it.toHistoryLinksVertex() }
                    .groupBy { it.key }
                    .map { it.key to it.value.map { it.peerId } }
                    .toMap()

                val payload = DiffPayload(data, addedLinks, removedLinks)

                return@map HistoryFactDto(event, payload)
            }
            .toSet()
    }

    fun storeFact(fact: HistoryFact): HistoryEventVertex = transaction(db) {
        val historyEventVertex = fact.newHistoryEventVertex()

        val elementVertices = fact.payload.data.map {
            return@map historyDao.newHistoryElementVertex().apply {
                key = it.key
                stringValue = it.value
            }
        }
        elementVertices.forEach { historyEventVertex.addEdge(it, HISTORY_ELEMENT_EDGE) }

        val addLinkVertices = linksVertices(fact.payload.addedLinks, historyDao.newAddLinkVertex())
        addLinkVertices.forEach { historyEventVertex.addEdge(it, HISTORY_ADD_LINK_EDGE) }

        val dropLinkVertices = linksVertices(fact.payload.removedLinks, historyDao.newDropLinkVertex())
        dropLinkVertices.forEach { historyEventVertex.addEdge(it, HISTORY_DROP_LINK_EDGE) }

        fact.subject.addEdge(historyEventVertex, HISTORY_EDGE)

        db.saveAll(listOf(historyEventVertex) + elementVertices + addLinkVertices + dropLinkVertices)

        return@transaction historyEventVertex
    }

    private fun HistoryFact.newHistoryEventVertex(): HistoryEventVertex =
        historyDao.newHistoryEventVertex().apply {
            entityClass = event.entityClass
            entityRID = event.entityId
            entityVersion = event.version
            timestamp = Instant.ofEpochMilli(event.timestamp)
            eventType = event.type.name
            val userInfo = userAcceptService.findByUsernameAsJson(event.user)

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

    private fun linksVertices(
        linksPayload: Map<String, List<ORID>>,
        linksVertex: HistoryLinksVertex
    ): List<HistoryLinksVertex> =
        linksPayload.flatMap { (linkKey, peerIds) ->
            peerIds.map { id ->
                linksVertex.apply {
                    key = linkKey
                    peerId = id
                }
            }
        }
}

// We can't use HistoryFact because vertex HistoryAware possible not exist
// todo: Make history independent from aware entity
class HistoryFactDto(val event: HistoryEvent, var payload: DiffPayload)