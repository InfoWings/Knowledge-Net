@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import org.w3c.dom.events.Event
import react.RClass
import react.ReactElement

external val Dialog: RClass<DialogProps>

external interface DialogProps : BlueprintComponentProps {

    /**
     * Whether the overlay should acquire application focus when it first opens.
     */
    var autoFocus: Boolean

    /**
     * CSS class names to apply to backdrop element.
     */
    var backdropClassName: String

    /**
     * HTML props for the backdrop element.
     */
    //var backdropProps: HtmlDivProps

    /**
     * Whether pressing the esc key should invoke onClose.
     */
    var canEscapeKeyClose: Boolean

    /**
     * Whether clicking outside the overlay element (either on backdrop when present or on document) should invoke onClose.
     */
    var canOutsideClickClose: Boolean

    /**
     * Whether the overlay should prevent focus from leaving itself. That is, if the user attempts to focus an element outside
     * the overlay and this prop is enabled, then the overlay will immediately bring focus back to itself. If you are nesting
     * overlay components, either disable this prop on the "outermost" overlays or mark the nested ones usePortal={false}.
     */
    var enforceFocus: Boolean

    /**
     * Dialog always has a backdrop so this prop is excluded from the public API.
     */
    var hasBackdrop: Boolean

    /**
     * Name of a Blueprint UI icon (or an icon element) to render in the dialog's header. Note that the header will only be
     * rendered if title is provided.
     */
    var icon: String // IconName | Element

    /**
     * Whether to show the close button in the dialog's header. Note that the header will only be rendered if title is provided.
     */
    var isCloseButtonShown: Boolean

    /**
     * Toggles the visibility of the overlay and its children. This prop is required because the component is controlled.
     */
    var isOpen: Boolean

    /**
     * If true and usePortal={true}, the Portal containing the children is created and attached to the DOM when the overlay
     * is opened for the first time; otherwise this happens when the component mounts. Lazy mounting provides noticeable
     * performance improvements if you have lots of overlays at once, such as on each row of a table.
     */
    var lazy: Boolean

    /**
     * A callback that is invoked when user interaction causes the overlay to close, such as clicking on the overlay or
     * pressing the esc key (if enabled). Receives the event from the user's interaction, if there was an event (generally
     * either a mouse or key event). Note that, since this component is controlled by the isOpen prop, it will not actually
     * close itself until that prop becomes false.
     */
    var onClose: (Event?) -> Unit

    /**
     * CSS styles to apply to the dialog.
     */
    //var style: CSSProperties

    /**
     * Title of the dialog. If provided, an element with Classes.DIALOG_HEADER will be rendered inside the dialog before
     * any children elements
     */
    var title: ReactElement

    /**
     * Indicates how long (in milliseconds) the overlay's enter/leave transition takes. This is used by React CSSTransition
     * to know when a transition completes and must match the duration of the animation in CSS. Only set this prop if you
     * override Blueprint's default transitions with new transitions of a different length.
     */
    var transitionDuration: Number

    /**
     * Name of the transition for internal CSSTransition. Providing your own name here will require defining new CSS transition
     * properties.
     */
    var transitionName: String

    var usePortal: Boolean
}