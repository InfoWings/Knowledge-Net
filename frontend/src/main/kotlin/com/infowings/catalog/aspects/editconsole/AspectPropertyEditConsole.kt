package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyAspect
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyCardinality
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyNameInput
import com.infowings.catalog.aspects.editconsole.view.aspectConsoleBlock
import com.infowings.catalog.aspects.editconsole.view.consoleButtonsGroup
import com.infowings.catalog.common.AspectData
import kotlinx.coroutines.experimental.launch
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.div

class AspectPropertyEditConsole(props: Props) :
    RComponent<AspectPropertyEditConsole.Props, RState>(props) {

    private var inputRef: HTMLInputElement? = null

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.parentAspect.id != nextProps.parentAspect.id
            || props.aspectPropertyIndex != nextProps.aspectPropertyIndex
        ) {
            inputRef?.focus()
            inputRef?.select()
        }
    }

    override fun componentDidMount() {
        inputRef?.focus()
        inputRef?.select()
    }

    private fun assignInputRef(inputRef: HTMLInputElement?) {
        this.inputRef = inputRef
    }

    /**
     * Business logic function (handler for add-to-list icon click and ctrl-enter keystoke)
     */
    private fun switchToNextProperty() {
        launch {
            props.propertyEditConsoleModel.switchToNextProperty()
        }
    }

    /**
     * Business logic function (handler for check icon click and enter keystoke)
     */
    private fun trySubmitParentAspect() {
        launch {
            props.propertyEditConsoleModel.submitParentAspect()
        }
    }


    private fun handlePropertyNameChanged(name: String) {
        props.propertyEditConsoleModel.updateAspectProperty(
            props.parentAspect.properties[props.aspectPropertyIndex].copy(
                name = name
            )
        )
    }

    private fun handlePropertyCardinalityChanged(cardinality: String) {
        props.propertyEditConsoleModel.updateAspectProperty(
            props.parentAspect.properties[props.aspectPropertyIndex].copy(
                cardinality = cardinality
            )
        )
    }

    private fun handlePropertyAspectIdChanged(aspect: AspectData) {
        props.propertyEditConsoleModel.updateAspectProperty(
            props.parentAspect.properties[props.aspectPropertyIndex].copy(
                aspectId = aspect.id ?: error("Selected aspect has no ID")
            )
        )
    }

    override fun RBuilder.render() {
        aspectConsoleBlock {
            attrs {
                onEscape = props.propertyEditConsoleModel::discardChanges
                onEnter = ::trySubmitParentAspect
                onCtrlEnter = ::switchToNextProperty
            }
            div(classes = "aspect-edit-console--input-group-aspect-property") {
                aspectPropertyNameInput {
                    attrs {
                        value = props.parentAspect.properties[props.aspectPropertyIndex].name
                        onChange = ::handlePropertyNameChanged
                        inputRef = ::assignInputRef
                    }
                }
                aspectPropertyCardinality {
                    attrs {
                        value = props.parentAspect.properties[props.aspectPropertyIndex].cardinality
                        onChange = ::handlePropertyCardinalityChanged
                    }
                }
                div(classes = "aspect-edit-console--property-aspect-wrapper") {
                    aspectPropertyAspect {
                        val aspectPropertyId = props.parentAspect.properties[props.aspectPropertyIndex].id
                        attrs {
                            parentAspectId = props.parentAspect.id
                            this.aspectPropertyId = if (aspectPropertyId.isEmpty()) null else aspectPropertyId
                            aspect = props.childAspect
                            onAspectSelected = ::handlePropertyAspectIdChanged
                        }
                    }
                }
                consoleButtonsGroup(
                    onSubmitClick = ::trySubmitParentAspect,
                    onCancelClick = props.propertyEditConsoleModel::discardChanges,
                    onAddToListClick = ::switchToNextProperty,
                    onDeleteClick = props.propertyEditConsoleModel::deleteProperty
                )
            }
        }
    }

    interface Props : RProps {
        var parentAspect: AspectData
        var aspectPropertyIndex: Int
        var childAspect: AspectData?
        var propertyEditConsoleModel: AspectPropertyEditConsoleModel
    }
}

fun RBuilder.aspectPropertyEditConsole(block: RHandler<AspectPropertyEditConsole.Props>) =
    child(AspectPropertyEditConsole::class, block)