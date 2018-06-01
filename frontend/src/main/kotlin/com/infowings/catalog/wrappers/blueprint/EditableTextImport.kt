@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import react.RClass


external val EditableText: RClass<EditableTextProps>

external interface EditableTextProps : BlueprintComponentProps {
    /**
     * If true and in multiline mode, the enter key will trigger onConfirmSearch and mod+enter will insert a newline.
     * If false, the key bindings are inverted such that enter adds a newline.
     */
    var confirmOnEnterKey: Boolean

    /**
     * Default text value of uncontrolled input.
     */
    var defaultValue: String

    /**
     * Whether the text can be edited.
     */
    var disabled: Boolean

    /**
     * Visual intent color to apply to element.
     */
    var intent: Intent

    /**
     * Whether the component is currently being edited.
     */
    var isEditing: Boolean

    /**
     * Maximum number of characters allowed. Unlimited by default.
     */
    var maxLength: Int

    /**
     * Maximum number of lines before scrolling begins, when multiline.
     */
    var maxLines: Int

    /**
     * Minimum number of lines (essentially minimum height), when multiline.
     */
    var minLines: Int

    /**
     * Minimum width in pixels of the input, when not multiline.
     */
    var minWidth: Int

    /**
     * Whether the component supports multiple lines of text. This prop should not be changed during the component's
     * lifetime.
     */
    var multiline: Boolean

    /**
     * Callback invoked when user cancels input with the esc key. Receives last confirmed value.
     */
    var onCancel: (value: String) -> Unit

    /**
     * Callback invoked when user changes input in any way.
     */
    var onChange: (value: String) -> Unit

    /**
     * Callback invoked when user confirms value with enter key or by blurring input.
     */
    var onConfirm: (value: String) -> Unit

    /**
     * Callback invoked after the user enters edit mode.
     */
    var onEdit: () -> Unit

    /**
     * Placeholder text when there is no value.
     */
    var placeholder: String

    /**
     * Whether the entire text field should be selected on focus. If false, the cursor is placed at the end of the
     * text.
     */
    var selectAllOnFocus: Boolean

    /**
     * Text value of controlled input.
     */
    var value: String
}

