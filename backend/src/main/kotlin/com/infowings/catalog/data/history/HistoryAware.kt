package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.UserVertex
import com.infowings.catalog.common.EventType
import com.infowings.catalog.storage.sessionStore
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex
import java.util.*


const val HISTORY_EDGE = "HistoryEdge"

interface HistoryAware : OVertex {
    val entityClass: String

    fun currentSnapshot(): Snapshot

    /**
     * по-хорошему здесь lazy property просится - мы один раз делаем слепок со структуры снепшота
     * мы не может этого делать в момент инициализации интерфейса,
     * но в рантайме каждый вызов вернет одно и то же
     * Kotlin не дает их заводить в интерфейсе,
     * хотя не очент понятно, что мешает
     */
    fun emptySnapshot(): Snapshot {
        val base = currentSnapshot()

        val data = base.data.mapValues { "" }
        val links = base.links.mapValues { emptyList<ORID>() }

        return Snapshot(data, links)
    }

    private fun historyEvent(userVertex: UserVertex, event: EventType, sessionId: UUID): HistoryEventWrite {
        return HistoryEventWrite(
            userVertex = userVertex, timestamp = System.currentTimeMillis(), version = version + 1,
            type = event, entityVertex = this, entityClass = entityClass, sessionId = sessionId
        )
    }

    /*
      Берет текущий снепшот и сравнивает его с базовым, фиксируя замеченные отличия.

      Схема сравнения сознательно упрощена.
      В принципе, есть три отдельных случая:
      1. значением строкового поля является пустая строка. На уровне бизнес-логики это обязательное поле,
        в которое сознательно ввели значение, равно строке длиной 0
      2. значением nullable поля является null. На уровне бизнес-логики это опциональное поле,
        в которое сознательно не ввели значения
      3. отсутствует поле. Когда-то поля не было совсем. В какой-то момент оно появилось

       В принципе, возможно аккуратно разобрать все эти варианты со всеми граничными случаями.
       Но это кажется излишним усложнением. Как-минимум надо придумать тег, отличающий 1 от 2.

       Поэтому различиются 2 случая - есть поле и нет поля (куда попадают все три варианта выше).
       Это кажется соответствующим опыту юзера и избавляет нас от лишних разборов.
     */
    private fun toFact(
        context: HistoryContext,
        eventType: EventType,
        sessionId: UUID,
        base: Snapshot
    ): HistoryFactWrite {
        val userVertex = context.userVertex
        return when (eventType) {
            EventType.CREATE,
            EventType.UPDATE ->
                toHistoryFact(historyEvent(userVertex, eventType, sessionId), base, currentSnapshot())

            EventType.DELETE,
            EventType.SOFT_DELETE ->
                toHistoryFact(historyEvent(userVertex, eventType, sessionId), currentSnapshot(), base)
        }
    }

    private fun sessionId() = sessionStore.get().uuid

    /**
     *  Факт создания сущности.
     *  В качестве базового снепшота берется пустой снепшот. Все непустые поля фиксируются в истории
     *
     *  Надо вызывать в тот момент, когда сущность создана, все поля и связи определены.
     */
    fun toCreateFact(context: HistoryContext) = toFact(context, EventType.CREATE, sessionId(), emptySnapshot())


    /**
     *  Факт удаления сущности.
     *  В качестве базового снепшота берется пустой снепшот. Все непустые поля фиксируются в истории
     *
     *  В этом есть некоторая нерегулярность, потому что, в отличие от update/create, в качестве базового
     *  снепшота берется состояние посде действия.
     *
     *  Это делается для того, чтобы быстро получить состояние сущности на момент удаления.
     *  Без этого для того, чтобы узнать все связи сщуности в момент удаления, пришлось бы пройти всю
     *  историю в поисках тех связей, что бы ли добавлены и не удалены (т.к. у нас нет ограниченного набора
     *  возможных связей)
     *
     *  Надо вызывать до удаления сущности.
     */
    fun toDeleteFact(context: HistoryContext) = toFact(context, EventType.DELETE, sessionId(), emptySnapshot())


    /**
     * Аналогично delete
     */
    fun toSoftDeleteFact(context: HistoryContext) = toFact(context, EventType.SOFT_DELETE, sessionId(), emptySnapshot())


    /**
     * Факт обновления сущности.
     * В качестве базового берется снепшот сущности до изменения.
     *
     * Надо до изменения вызвать currentSnapshot(), запомнить значение,
     * а по окончании изменения - вызвать toUpdateFact, передав сохраненное значение
     * в previous
     */
    fun toUpdateFact(context: HistoryContext, previous: Snapshot) =
        toFact(context, EventType.UPDATE, sessionId(), previous)
}