@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import react.RClass
import react.ReactElement

external val Popover: RClass<PopoverProps>

external interface PopoverProps : BlueprintComponentProps {
    var autoFocus: Boolean // inhereted from OverlayableProps
    // var backdropProps: HtmlProp<HtmlDivElement> // Html props for backdrop element
    var canEscapeKeyClose: Boolean // inhereted from OverlayableProps

    /**
     * The content displayed inside the popover. This can instead be provided as the second children element
     * (first is target).
     */
    var content: ReactElement?

    /**
     * Initial opened state when uncontrolled.
     */
    var defaultIsOpen: Boolean

    /**
     * Prevents the popover from appearing when true, even if isOpen={true}.
     */
    var disabled: Boolean

    /**
     * Whether the overlay should prevent focus from leaving itself. That is, if the user attempts to focus an
     * element outside the overlay and this prop is enabled, then the overlay will immediately bring focus back
     * to itself. If you are nesting overlay components, either disable this prop on the "outermost" overlays or
     * mark the nested ones usePortal={false}.
     *
     * Inhereted from OverlayableProps
     */
    var enforceFocus: Boolean

    /**
     * Enables an invisible overlay beneath the popover that captures clicks and prevents interaction with the rest
     * of the document until the popover is closed. This prop is only available when interactionKind is
     * PopoverInteractionKind.CLICK. When popovers with backdrop are opened, they become focused.
     */
    var hasBackdrop: Boolean

    /**
     * The amount of time in milliseconds the popover should remain open after the user hovers off the trigger.
     * The timer is canceled if the user mouses over the target before it expires. This option only applies when
     * interactionKind is HOVER or HOVER_TARGET_ONLY.
     */
    var hoverCloseDelay: Int

    /**
     * The amount of time in milliseconds the popover should wait before opening after the the user hovers over the
     * trigger. The timer is canceled if the user mouses away from the target before it expires. This option only
     * applies when interactionKind is HOVER or HOVER_TARGET_ONLY.
     */
    var hoverOpenDelay: Int

    /**
     * Whether a popover should automatically inherit the dark theme from its parent. Note that this prop is ignored
     * if usePortal={false}, as the Popover will inherit dark theme via CSS.
     */
    var inheritDarkTheme: Boolean

    /**
     * The kind of interaction that triggers the display of the popover.
     */
    var interactionKind: PopoverInteractionKind

    /**
     * Whether the popover is visible. Passing this prop puts the popover in controlled mode, where the only way to
     * change visibility is by updating this property. If disabled={true}, this prop will be ignored, and the popover
     * will remain closed.
     */
    var isOpen: Boolean

    /**
     * If true and usePortal={true}, the Portal containing the children is created and attached to the DOM when the
     * overlay is opened for the first time; otherwise this happens when the component mounts. Lazy mounting
     * provides noticeable performance improvements if you have lots of overlays at once, such as on each row of a
     * table.
     *
     * Inherited from OverlayableProps
     */
    var lazy: Boolean

    /**
     * Whether to apply minimal styles to this popover, which includes removing the arrow and adding the
     * .pt-minimal class to minimize and accelerate the transitions.
     */
    var minimal: Boolean

    /**
     * Popper modifier options, passed directly to internal Popper instance.
     * See https://popper.js.org/popper-documentation.html#modifiers for complete details.
     */
    // var modifiers: PopperModifiers

    /**
     * A callback that is invoked when user interaction causes the overlay to close, such as clicking on the
     * overlay or pressing the esc key (if enabled). Receives the event from the user's interaction, if there was
     * an event (generally either a mouse or key event). Note that, since this component is controlled by the
     * isOpen prop, it will not actually close itself until that prop becomes false.
     *
     * Inherited from OverlayableProps
     */
    var onClose: (Event?/* SyntheticEvent */) -> Unit

    /**
     * Callback invoked in controlled mode when the popover open state would change due to user interaction based on
     * the value of interactionKind.
     */
    var onInteraction: (nextOpenState: Boolean) -> Unit

    /**
     * Whether the popover should open when its target is focused. If true, target will render with tabindex="0"
     * to make it focusable via keyboard navigation. This prop is only available when interactionKind is HOVER or
     * HOVER_TARGET_ONLY.
     */
    var openOnTargetFocus: Boolean

    /**
     * A space-delimited string of class names applied to the popover.
     */
    var popoverClassName: String

    /**
     * Callback invoked when the popover opens after it is added to the DOM.
     */
    var popoverDidOpen: () -> Unit

    /**
     * Ref supplied to the pt-popover element.
     */
    var popoverRef: (ref: HTMLDivElement?) -> Unit

    /**
     * Callback invoked when a popover begins to close.
     */
    var popoverWillClose: () -> Unit

    /**
     * Callback invoked before the popover opens.
     */
    var popoverWillOpen: () -> Unit

    /**
     * Space-delimited string of class names applied to the portal that holds the popover if usePortal={true}.
     */
    var portalClassName: String

    /**
     * The position (relative to the target) at which the popover should appear.
     *
     * The default value of "auto" will choose the best position when opened and will allow the popover to
     * reposition itself to remain onscreen as the user scrolls around.
     */
    var position: Position

    /**
     * The name of the HTML tag to use when rendering the popover target wrapper element (.pt-popover-target).
     */
    var rootElementTag: String

    /**
     * The target element to which the popover content is attached. This can instead be provided as the first
     * children element.
     */
    var target: ReactElement

    /**
     * Space-delimited string of class names applied to the target.
     */
    var targetClassName: String

    /**
     * Indicates how long (in milliseconds) the overlay's enter/leave transition takes. This is used by React
     * CSSTransition to know when a transition completes and must match the duration of the animation in CSS.
     * Only set this prop if you override Blueprint's default transitions with new transitions of a different
     * length.
     *
     * Inherited from OverlayableProps
     */
    var transitionDuration: Int

    /**
     * Whether the popover should be rendered inside a Portal attached to document.body. Rendering content inside
     * a Portal allows the popover content to escape the physical bounds of its parent while still being positioned
     * correctly relative to its target.
     *
     * Using a Portal is necessary if any ancestor of the target hides overflow or uses very complex positioning.
     * Not using a Portal can result in smoother performance when scrolling and allows the popover content to
     * inherit CSS styles from surrounding elements.
     */
    var usePortal: Boolean
}

external enum class PopoverInteractionKind {
    CLICK, CLICK_TARGET_ONLY, HOVER, HOVER_TARGET_ONLY
}