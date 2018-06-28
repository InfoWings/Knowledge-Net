package com.infowings.catalog.objects.edit

import react.*


interface ObjectTreeEditModel {
}

class ObjectTreeEditModelComponent : RComponent<ObjectTreeEditModelComponent.Props, ObjectTreeEditModelComponent.State>(),
    ObjectTreeEditModel {

    override fun State.init() {
    }

    override fun RBuilder.render() {
    }

    interface State : RState {
    }

    interface Props : RProps {
    }
}

fun RBuilder.objectTreeEditModel(block: RHandler<ObjectTreeEditModelComponent.Props>) =
    child(ObjectTreeEditModelComponent::class, block)
