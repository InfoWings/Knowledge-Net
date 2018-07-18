package com.infowings.catalog.wrappers.select

import org.w3c.dom.ErrorEvent
import org.w3c.dom.events.Event
import react.*

external interface CommonSelectProps<T : SelectOption> : RProps {
    //var aria-describedby: String
    //var aria-label: String
    //var aria-labelledby: String

    /**
     * Renders custom drop-down arrow to be shown in a right-hand side of select: arrowRenderer({onMouseDown, isOpen})
     * Won't render when set to null
     */
    var arrowRenderer: (ArrowRendererArgs) -> ReactElement

    /**
     * Blurs the input element after a selection has been made. Handy for lowering the keyboard on mobile devices
     */
    var autoBlur: Boolean

    /**
     * Autofocus the component on mount
     */
    var autoFocus: Boolean

    /**
     * whether to auto-load the default async options set
     */
    var autoload: Boolean

    /**
     * If enabled, the input will expand as the length of its value increases
     */
    var autosize: Boolean

    /**
     * whether pressing backspace removes the last item when there is no input value
     */
    var backspaceRemoves: Boolean

    /**
     * prompt shown in input when at least one option in a multiselect is shown, set to '' to clear
     */
    var backspaceToRemoveMessage: String

    /**
     * className for the outer element
     */
    var className: String

    /**
     * should it be possible to reset value
     */
    var clearable: Boolean

    /**
     * title for the "clear" control when `multi` is true
     */
    var clearAllText: String

    /**
     * Renders a custom clear to be shown in the right-hand side of the select when clearable true: `#clearRenderer()`
     */
    var clearRenderer: () -> ReactElement

    /**
     * title for the "clear" control
     */
    var clearValueText: String

    /**
     * whether to close the menu when a value is selected
     */
    var closeOnSelect: Boolean

    /**
     * whether pressing delete key removes the last item when there is no input value
     */
    var deleteRemoves: Boolean

    /**
     * delimiter to use to join multiple values
     */
    var delimiter: String

    /**
     * whether the Select is disabled or not
     */
    var disabled: Boolean

    /**
     * whether escape clears the value when the menu is closed
     */
    var escapeClearsValue: Boolean

    /**
     * method to filter a single option `(option, filterString) => boolean`
     */
    var filterOption: (T, String) -> Boolean

    /**
     * boolean to enable default filtering or function to filter the options array
     * (Array<option>, filterString, Array<values>) => Array<options>
     */
    var filterOptions: (Array<T>, input: String, currentValues: Array<String>) -> Array<T>

    /**
     * html id to set on the input element for accessibility or tests
     */
    var id: String

    /**
     * whether to strip accents when filtering
     */
    var ignoreAccents: Boolean

    /**
     * whether to perform case-insensitive filtering
     */
    var ignoreCase: Boolean

    /**
     * custom attributes for the Input (in the Select-control) e.g: `{'data-foo': 'bar'}`
     */
    var inputProps: Any

    /**
     * renders a custom input component
     */
    var inputRenderer: () -> ReactElement // No arguments ???

    /**
     * instance ID used internally to set html ids on elements for accessibility, specify for universal rendering
     */
    var instanceId: String

    /**
     * whether the Select is loading externally or not (such as options being loaded)
     */
    var isLoading: Boolean

    /**
     * join multiple values into a single hidden input using the `delimiter`
     */
    var joinValues: Boolean

    /**
     * the option property to use for the label
     */
    var labelKey: String

    /**
     * (any, start) match the start or entire string when filtering
     */
    var matchPos: String

    /**
     * (any, label, value) which option property to filter on
     */
    var matchProp: String

    /**
     * buffer of px between the base of the dropdown and the viewport to shift if menu doesnt fit in viewport
     */
    var menuBuffer: Int

    /**
     * optional style to apply to the menu container
     */
    var menuContainerStyle: Any

    /**
     * Renders a custom menu with options;
     * accepts the following named parameters:
     * `menuRenderer({ focusedOption, focusOption, options, selectValue, valueArray })`
     */
    // var menuRenderer: (MenuRendererArgs) -> ReactElement

    /**
     * optional style to apply to the menu
     */
    var menuStyle: Any

    /**
     * multi-value input
     */
    var multi: Boolean

    /**
     * fieldName name, for hidden `<input />` tag
     */
    var name: String

    /**
     * placeholder displayed when there are no matching search results or a
     * falsy value to hide it (can also be a react component)
     */
    var noResultsText: String

    /**
     * onBlur handler: `function(event) {}`
     */
    var onBlur: (Event) -> Unit

    /**
     * Whether to clear input on blur or not. If set to false, it only works if onCloseResetsInput is false as well.
     */
    var onBlurResetsValue: Boolean

    /**
     * onChange handler: `function(newOption) {}`
     */
    var onChange: (T) -> Unit

    /**
     * handler for when the menu closes: `function () {}`
     */
    var onClose: () -> Unit

    /**
     * whether to clear input when closing the menu through the arrow
     */
    var onCloseResetsInput: Boolean

    /**
     * onFocus handler: function(event) {}
     */
    var onFocus: (Event) -> Unit

    /**
     * onInputChange handler/interceptor: function(inputValue: string): string
     */
    var onInputChange: (String) -> String

    /**
     * input keyDown handler;
     * call `event.preventDefault()` to override default `Select` behaviour:
     * `function(event) {}`
     */
    var onInputKeyDown: (Event) -> Unit

    /**
     * called when the menu is scrolled to the bottom
     */
    var onMenuScrollToBottom: () -> Unit

    /**
     * handler for when the menu opens: `function () {}`
     */
    var onOpen: () -> Unit

    /**
     * whether the input value should be reset when options are selected.
     * Also input value will be set to empty if 'onSelectResetsInput=true'
     * and Select will get new value that not equal previous value.
     */
    var onSelectResetsInput: Boolean

    /**
     * onClick handler for value labels: `function (value, event) {}`
     */
    var onValueClick: (Event) -> Unit

    /**
     * open the options menu when the control is clicked (requires searchable = true)
     */
    var openOnClick: Boolean

    /**
     * open the options menu when the control gets focus
     */
    var openOnFocus: Boolean

    /**
     * additional class(es) to apply to the elements
     */
    var optionClassName: String

    /**
     * option component to render in dropdown
     */
    var optionComponent: RClass<OptionComponentProps<T>>

    /**
     * custom function to render the options in the menu
     */
    //var optionRenderer: ((RendererArgs) -> ReactElement/RClass<RendererProps>)

    /**
     * array of options
     */
    var options: Array<T>

    /**
     * whether the selected option is removed from the dropdown on multi selects
     */
    var removeSelected: Boolean

    /**
     * number of options to jump when using page up/down keys
     */
    var pageSize: Int

    /**
     * fieldName placeholder, displayed when there's no value
     */
    var placeholder: String //Or ReactElement (node)

    /**
     * applies HTML5 required attribute when needed
     */
    var required: Boolean

    /**
     * value to set when the control is cleared
     */
    var resetValue: Any?

    /**
     * use react-select in right-to-left direction
     */
    var rtl: Boolean

    /**
     * whether the viewport will shift to display the entire menu when engaged
     */
    var scrollMenuIntoView: Boolean

    /**
     * whether to enable searching feature or not
     */
    var searchable: Boolean

    /**
     * label to prompt for search input
     */
    var searchPromptText: String // or ReactElement (node)

    /**
     * pass the value to onChange as a string
     */
    var simpleValue: Boolean

    /**
     * optional styles to apply to the control
     */
    var style: Any

    /**
     * tabIndex of the control
     */
    var tabIndex: String // or Number (Int)

    /**
     * whether to select the currently focused value when the [tab] key is pressed
     */
    var tabSelectsValue: Boolean

    /**
     * whether to trim whitespace from the filter value
     */
    var trimFilter: Boolean

    /**
     * initial fieldName value
     */
    var value: Any?

    /**
     * function which returns a custom way to render/manage the value selected `<CustomValue />`
     */
    // var valueComponent: (???) -> ???

    /**
     * the option property to use for the value
     */
    var valueKey: String

    /**
     * function which returns a custom way to render the value selected
     * `function (option) {}`
     */
    // var <T: (SelectOption + RProps)???> valueRenderer: (RClass<ValueRendererProps<T>/(T) -> ReactElement)

    /**
     * optional styles to apply to the component wrap
     */
    var wrapperStyle: Any
}

external interface ArrowRendererArgs {
    var onMouseDown: (Event) -> Unit //Type may be not correct
    var isOpen: Boolean
}

external interface AsyncSpecificProps<T : SelectOption> : RProps {

    /**
     * automatically call the `loadOptions` prop on-mount
     */
    var autoload: Boolean

    /**
     * Sets the cache object used for options. Set to `false` if you would like to disable caching.
     */
    var cache: Any

    /**
     * label to prompt for loading search result
     */
    var loadingPlaceholder: String // or ReactElement (node)

    /**
     * function that returns a promise or calls a callback with the options:
     * `function(input, [callback])`
     *
     * NOTE: Boolean return type (should always be false) is a hack in order to fool javascript truthy check on a result of
     * a callback. `loadOptions` may return a promise and library code checks the fact using `if (promise)`
     */
    var loadOptions: (String, AsyncLoadCallback<T>) -> Boolean

}

typealias AsyncLoadCallback<T> = (ErrorEvent?, AsyncLoadData<T>?) -> Unit

external interface AsyncLoadData<T : SelectOption> {
    var options: Array<T>
    var complete: Boolean // Set only if all data is fetched
}

external interface OptionComponentProps<T : SelectOption> : RProps {
    var className: String
    var instancePrefix: String
    var isDisabled: Boolean
    var isFocused: Boolean
    var isSelected: Boolean
    var onFocus: (T, Event) -> Unit
    var onSelect: (T, Event) -> Unit
    var onUnfocus: (T, Event) -> Unit
    var option: T
    var optionIndex: Int
}

external interface CreatableSpecificProps<T : SelectOption> : RProps {

    /**
     * Child function responsible for creating the inner Select component.
     * This component can be used to compose HOCs (eg Creatable and Async).
     * Expected signature: (props: Object): PropTypes.element
     */
    var children: (Any) -> ReactElement

    /**
     * Searches for any matching option within the set of options.
     * This function prevents duplicate options from being created.
     * By default this is a basic, case-sensitive comparison of label and value.
     * Expected signature: `({ option: Object, options: Array, labelKey: string, valueKey: string }): boolean`
     */
    var isOptionUnique: (OptionUniqueArgs<T>) -> Boolean

    /**
     * Determines if the current input text represents a valid option.
     * By default any non-empty string will be considered valid.
     * Expected signature: `({ label: string }): boolean`
     */
    var isValidNewOption: (ValidNewOptionArgs) -> Boolean

    /**
     * Factory to create new option.
     * Expected signature: `({ label: string, labelKey: string, valueKey: string }): Object`
     */
    var newOptionCreator: (OptionFactoryArgs) -> SelectOption?

    /**
     * new option click handler, it calls when new option has been selected.
     * `function(option) {}`
     */
    var onNewOptionClick: (T) -> Unit

    /**
     * Decides if a keyDown event (eg its keyCode) should result in the creation of a new option.
     * ENTER, TAB and comma keys create new options by default.
     * Expected signature: `({ keyCode: number }): boolean`
     */
    var shouldKeyDownEventCreateNewOption: (ShouldKeyDownEventCreateNewOptionArgs) -> Boolean

    /**
     * Factory for overriding default option creator prompt label.
     * By default it will read 'Create option "{label}"'.
     * Expected signature: (label: String): String
     */
    var promptTextCreator: (String) -> String
}

external interface OptionUniqueArgs<T : SelectOption> {
    var option: T
    var options: Array<T>
    var labelKey: String
    var valueKey: String
}

external interface ValidNewOptionArgs {
    var label: String
}

external interface OptionFactoryArgs {
    var label: String
    var labelKey: String
    var valueKey: String
}

external interface ShouldKeyDownEventCreateNewOptionArgs {
    var keyCode: Int
}

external interface AsyncSelectProps<T : SelectOption> : CommonSelectProps<T>, AsyncSpecificProps<T>
external interface CreatableSelectProps<T : SelectOption> : CommonSelectProps<T>, CreatableSpecificProps<T>
external interface AsyncCreatableSelectProps<T : SelectOption> : CommonSelectProps<T>,
    AsyncSpecificProps<T>, CreatableSpecificProps<T>

external interface SelectOption

external interface SelectComponent<T : SelectOption> : RClass<CommonSelectProps<T>>
external interface AsyncComponent<T : SelectOption> : RClass<AsyncSelectProps<T>>
external interface AsyncCreatableComponent<T : SelectOption> : RClass<AsyncCreatableSelectProps<T>>
external interface CreatableComponent<T : SelectOption> : RClass<CreatableSelectProps<T>>

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")
fun <T : SelectOption> RBuilder.commonSelect(block: RHandler<CommonSelectProps<T>>) {
    (Select as SelectComponent<T>)(block)
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")
fun <T : SelectOption> RBuilder.asyncSelect(block: RHandler<AsyncSelectProps<T>>) {
    (Async as AsyncComponent<T>)(block)
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")
fun <T : SelectOption> RBuilder.creatableSelect(block: RHandler<CreatableSelectProps<T>>) {
    (Creatable as CreatableComponent<T>)(block)
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")
fun <T : SelectOption> RBuilder.asyncCreatableSelect(block: RHandler<AsyncCreatableSelectProps<T>>) {
    (AsyncCreatable as AsyncCreatableComponent<T>)(block)
}
