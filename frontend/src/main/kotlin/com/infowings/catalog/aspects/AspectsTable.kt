package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import com.infowings.catalog.wrappers.table.*

private fun headerComponent(columnName: String) = rFunction<RTableRendererProps>("AspectHeader") {
    span {
        +columnName
    }
}

private fun cellComponent(onChangeHandler: (AspectData, String) -> Unit) = rFunction<RTableRendererProps>("AspectInputCell") { rTableRendererProps ->
    input(type = InputType.text, classes = "rtable-input") {
        attrs {
            value = rTableRendererProps.value?.toString() ?: ""
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

private data class AspectRow(val aspect: AspectData, val pending: Boolean)

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

private fun propertySubComponent(
        aspectsMap: Map<String, AspectData>,
        onAspectPropertyChanged: (changedAspect: AspectData, propertyChanger: (aspect: AspectData) -> AspectData) -> Unit
): RClass<SubComponentProps> = rFunction("PropertySubComponent") { props ->
    val original = props.original as AspectRow
    child(AspectPropertySubtable::class) {
        attrs {
            data = original.aspect.properties.map {
                AspectPropertyRow(it, aspectsMap[it.aspectId]
                        ?: AspectData("", "", "", "", ""))
            }.toTypedArray()
            onPropertyChanged = { propertyChanger -> onAspectPropertyChanged(original.aspect, propertyChanger) }
        }
    }
}

/**
 * Use as: child(AspectsTable::class) {}
 */
class AspectsTable(props: AspectApiReceiverProps) : RComponent<AspectApiReceiverProps, AspectsTable.State>(props) {

    private lateinit var subComponent: RClass<SubComponentProps>

    override fun componentWillMount() {
        subComponent = propertySubComponent(props.aspectsMap, ::onAspectPropertyChanged)
    }

    override fun State.init(props: AspectApiReceiverProps) {
        pending = emptyMap()
        newAspect = null
    }

    private fun onAspectNameChanged(changed: AspectData, name: String) {
        setState {
            if (changed.id == "") {
                newAspect = AspectData("", name, changed.measure, changed.domain, changed.baseType, changed.properties)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            AspectData(aspect.id, name, aspect.measure, aspect.domain, aspect.baseType, aspect.properties)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            AspectData(changed.id, name, changed.measure, changed.domain, changed.baseType, changed.properties)
                    ))
                }
            }
        }
    }

    private fun onAspectUnitChanged(changed: AspectData, unit: String) {
        setState {
            if (changed.id == "") {
                newAspect = AspectData("", changed.name, unit, changed.domain, changed.baseType, changed.properties)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            AspectData(aspect.id, aspect.name, unit, aspect.domain, aspect.baseType, aspect.properties)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            AspectData(changed.id, changed.name, unit, changed.domain, changed.baseType, changed.properties)
                    ))
                }
            }
        }
    }

    private fun onAspectDomainChanged(changed: AspectData, domain: String) {
        setState {
            if (changed.id == "") {
                newAspect = AspectData("", changed.name, changed.measure, domain, changed.baseType, changed.properties)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            AspectData(aspect.id, aspect.name, aspect.measure, domain, aspect.baseType, aspect.properties)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            AspectData(changed.id, changed.name, changed.measure, domain, changed.baseType, changed.properties)
                    ))
                }
            }
        }
    }

    private fun onAspectBaseTypeChanged(changed: AspectData, baseType: String) {
        setState {
            if (changed.id == "") {
                newAspect = AspectData("", changed.name, changed.measure, changed.domain, baseType, changed.properties)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            AspectData(aspect.id, aspect.name, aspect.measure, aspect.domain, baseType, aspect.properties)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            AspectData(changed.id, changed.name, changed.measure, changed.domain, baseType, changed.properties)
                    ))
                }
            }
        }
    }

    private fun onAspectPropertyChanged(changed: AspectData, propertyChanger: (aspect: AspectData) -> AspectData) {
        setState {
            if (changed.id == "") {
                newAspect = propertyChanger(changed)
            } else {
                val aspect = pending[changed.id]
                pending = if (aspect != null) {
                    pending.plus(Pair(
                            changed.id,
                            propertyChanger(aspect)
                    ))
                } else {
                    pending.plus(Pair(
                            changed.id,
                            propertyChanger(changed)
                    ))
                }
            }
        }
    }

    private fun aspectsToRows(): Array<AspectRow> =
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
        if (aspectId == "") {
            val savedAspect = state.newAspect!!
            setState {
                newAspect = null
            }
            props.onAspectCreate(savedAspect)
        }
    }

    private fun startCreatingNewAspect() {
        setState {
            newAspect = AspectData("", "", null, null, null)
        }
    }

    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                        aspectColumn("aspect.name", headerComponent("Name"), cellComponent(::onAspectNameChanged)),
                        aspectColumn("aspect.measure", headerComponent("Measure Unit"), cellComponent(::onAspectUnitChanged)),
                        aspectColumn("aspect.domain", headerComponent("Domain"), cellComponent(::onAspectDomainChanged)),
                        aspectColumn("aspect.baseType", headerComponent("Base Type"), cellComponent(::onAspectBaseTypeChanged)),
                        controlsColumn(
                                if (state.newAspect == null)
                                    addNewAspectHeaderEnabled(::startCreatingNewAspect)
                                else addNewAspectHeaderDisabled,
                                ::saveAspect,
                                ::resetAspect
                        )
                )
                data = aspectsToRows()
                loading = props.loading
                SubComponent = propertySubComponent(props.aspectsMap, ::onAspectPropertyChanged)
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
                resizable = false
                collapseOnDataChange = false
            }
        }
    }

    interface State : RState {
        var pending: Map<String, AspectData?>
        var newAspect: AspectData?
    }
}