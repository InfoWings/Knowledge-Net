package catalog.aspects

import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import wrappers.table.*

private fun headerComponent(columnName: String) = rFunction<RTableRendererProps>("AspectHeader") {
    span {
        +columnName
    }
}

private fun cellComponent(onChangeHandler: (Aspect, String) -> Unit) = rFunction<RTableRendererProps>("LoggingCell") { rTableRendererProps ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = rTableRendererProps.value.toString()
            onChangeFunction = { e ->
                onChangeHandler(rTableRendererProps.original.aspect, e.asDynamic().target.value)
            }
        }
    }
}

private fun aspectColumn(accessor: String, header: RClass<RTableRendererProps>, cell: RClass<RTableRendererProps>? = null) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = header
            cell?.let {
                this.Cell = cell
            }
        }

private data class AspectRow(val aspect: Aspect, val pending: Boolean)

private fun addNewAspectHeaderEnabled(onCreateNewAspect: () -> Unit): RClass<RTableRendererProps> = rFunction("CheckboxHeaderEnabled") {
    div(classes = "create-new-aspect-container") {
        i(classes = "fas fa-plus") {}
        attrs {
            onClickFunction = { onCreateNewAspect() }
        }
    }
}

private val addNewAspectHeaderDisabled: RClass<RTableRendererProps> = rFunction("CheckboxHeaderDisabled") {
    div(classes = "create-new-aspect-container create-new-aspect-container--disabled") {
        i(classes = "fas fa-plus") {}
    }
}

private val emptySubComponent: RClass<SubComponentProps> = rFunction("EmptySubComponent") { props ->
    val aspectRow = props.original as AspectRow
    div {
        span {
            +"Name:"
        }
        span {
            +aspectRow.aspect.name
        }
    }
}

/**
 * Use as: child(AspectsTable::class) {}
 */
class AspectsTable(props: Props) : RComponent<AspectsTable.Props, AspectsTable.State>(props) {

    override fun State.init(props: Props) {
        pending = emptyMap()
        newAspect = null
    }

    private fun onAspectNameChanged(changed: Aspect, name: String) {
        setState {
            if (changed.id == "") {
                newAspect = Aspect("", name, newAspect!!.measureUnit, newAspect!!.domain, newAspect!!.baseType)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            Aspect(aspect.id, name, aspect.measureUnit, aspect.domain, aspect.baseType)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            Aspect(changed.id, name, changed.measureUnit, changed.domain, changed.baseType)
                    ))
                }
            }
        }
    }

    private fun onAspectUnitChanged(changed: Aspect, unit: String) {
        setState {
            if (changed.id == "") {
                newAspect = Aspect("", newAspect!!.name, unit, newAspect!!.domain, newAspect!!.baseType)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            Aspect(aspect.id, aspect.name, unit, aspect.domain, aspect.baseType)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            Aspect(changed.id, changed.name, unit, changed.domain, changed.baseType)
                    ))
                }
            }
        }
    }

    private fun onAspectDomainChanged(changed: Aspect, domain: String) {
        setState {
            if (changed.id == "") {
                newAspect = Aspect("", newAspect!!.name, newAspect!!.measureUnit, domain, newAspect!!.baseType)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            Aspect(aspect.id, aspect.name, aspect.measureUnit, domain, aspect.baseType)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            Aspect(changed.id, changed.name, changed.measureUnit, domain, changed.baseType)
                    ))
                }
            }
        }
    }

    private fun onAspectBaseTypeChanged(changed: Aspect, baseType: String) {
        setState {
            if (changed.id == "") {
                newAspect = Aspect("", newAspect!!.name, newAspect!!.measureUnit, newAspect!!.domain, baseType)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            Aspect(aspect.id, aspect.name, aspect.measureUnit, aspect.domain, baseType)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            Aspect(changed.id, changed.name, changed.measureUnit, changed.domain, baseType)
                    ))
                }
            }
        }
    }

    private fun reduceRows(): Array<AspectRow> =
            if (state.newAspect == null)
                props.data.map {
                    val pending = state.pending[it.id]
                    if (pending == null) {
                        AspectRow(it, false)
                    } else {
                        AspectRow(pending, true)
                    }
                }.toTypedArray()
            else
                props.data.map {
                    val pending = state.pending[it.id]
                    if (pending == null) {
                        AspectRow(it, false)
                    } else {
                        AspectRow(pending, true)
                    }
                }.toTypedArray() + AspectRow(state.newAspect!!, true)

    private fun resetAspect(aspectId: String) {
        setState {
            if (aspectId == "") {
                newAspect = null
            } else {
                pending = pending.minus(aspectId)
            }
        }
    }

    private fun saveAspect(aspectId: String) {

    }

    private fun createNewAspect() {
        setState {
            newAspect = Aspect("", "", "", "", "")
        }
    }

    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                        aspectColumn("aspect.name", headerComponent("Name"), cellComponent(::onAspectNameChanged)),
                        aspectColumn("aspect.measureUnit", headerComponent("Measure Unit"), cellComponent(::onAspectUnitChanged)),
                        aspectColumn("aspect.domain", headerComponent("Domain"), cellComponent(::onAspectDomainChanged)),
                        aspectColumn("aspect.baseType", headerComponent("Base Type"), cellComponent(::onAspectBaseTypeChanged)),
                        checkboxColumn(
                                if (state.newAspect == null)
                                    addNewAspectHeaderEnabled(::createNewAspect)
                                else addNewAspectHeaderDisabled,
                                ::saveAspect,
                                ::resetAspect
                        )
                )
                data = reduceRows()
                SubComponent = emptySubComponent
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
                resizable = false
            }
        }
    }

    interface Props : RProps {
        var data: Array<Aspect>
        var onAspectUpdate: suspend (changed: Aspect) -> Aspect
    }

    interface State : RState {
        var pending: Map<String, Aspect?>
        var newAspect: Aspect?
    }
}