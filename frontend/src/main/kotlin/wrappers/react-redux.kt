@file:JsModule("react-redux")
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "EXTERNAL_DELEGATION", "NESTED_CLASS_IN_EXTERNAL_INTERFACE")

package wrappers;

import react.RClass
import react.RProps

external interface DispatchProp<S> {
    var dispatch: Dispatch<dynamic>? get() = definedExternally; set(value) = definedExternally
}

external interface AdvancedComponentDecorator<TProps, TOwnProps> {
    @nativeInvoke
    operator fun invoke(component: Any): Any
}

external interface InferableComponentEnhancerWithProps<TInjectedProps, TNeedsProps> {
    @nativeInvoke
    operator fun <P : TInjectedProps> invoke(component: dynamic): dynamic /* Any & `T$0` */
}

external interface Connect {
    @nativeInvoke
    operator fun invoke(): InferableComponentEnhancerWithProps<DispatchProp<Any>, Any?>

    @nativeInvoke
    operator fun <TStateProps, no_dispatch, TOwnProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & DispatchProp<Any> */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, no_dispatch, TOwnProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & DispatchProp<Any> */, TOwnProps>

    @nativeInvoke
    operator fun <no_state, TDispatchProps, TOwnProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: TDispatchProps): InferableComponentEnhancerWithProps<TDispatchProps, TOwnProps>

    @nativeInvoke
    operator fun <no_state, TDispatchProps, TOwnProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>): InferableComponentEnhancerWithProps<TDispatchProps, TOwnProps>

    @nativeInvoke
    operator fun <no_state, TDispatchProps, TOwnProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>): InferableComponentEnhancerWithProps<TDispatchProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: TDispatchProps): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun invoke(mapStateToProps: dynamic, mapDispatchToProps: dynamic): dynamic

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: TDispatchProps): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, no_dispatch, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: Nothing?, mergeProps: MergeProps<TStateProps, Nothing?, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, no_dispatch, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: Nothing?, mergeProps: MergeProps<TStateProps, Nothing?, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <no_state, TDispatchProps, TOwnProps, TMergedProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: TDispatchProps, mergeProps: MergeProps<Nothing?, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <no_state, TDispatchProps, TOwnProps, TMergedProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>, mergeProps: MergeProps<Nothing?, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <no_state, TDispatchProps, TOwnProps, TMergedProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>, mergeProps: MergeProps<Nothing?, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <no_state, no_dispatch, TOwnProps, TMergedProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: Nothing?, mergeProps: MergeProps<Nothing?, Nothing?, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: TDispatchProps, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: TDispatchProps, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, no_dispatch, TOwnProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: Nothing?, mergeProps: Nothing?, options: Options<State, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<DispatchProp<Any> /* DispatchProp<Any> & TStateProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, no_dispatch, TOwnProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: Nothing?, mergeProps: Nothing?, options: Options<State, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<DispatchProp<Any> /* DispatchProp<Any> & TStateProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: TDispatchProps, mergeProps: Nothing?, options: Options<Any, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TDispatchProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>, mergeProps: Nothing?, options: Options<Any, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TDispatchProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps> invoke(mapStateToProps: Nothing?, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>, mergeProps: Nothing?, options: Options<Any, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TDispatchProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: TDispatchProps, mergeProps: Nothing?, options: Options<State, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>, mergeProps: Nothing?, options: Options<State, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>, mergeProps: Nothing?, options: Options<State, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: TDispatchProps, mergeProps: Nothing?, options: Options<State, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>, mergeProps: Nothing?, options: Options<State, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>, mergeProps: Nothing?, options: Options<State, TStateProps, TOwnProps, Any?>): InferableComponentEnhancerWithProps<TStateProps /* TStateProps & TDispatchProps */, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: TDispatchProps, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>, options: Options<State, TStateProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>, options: Options<State, TStateProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToPropsFactory<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>, options: Options<State, TStateProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: TDispatchProps, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>, options: Options<State, TStateProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFactory<TDispatchProps, TOwnProps>, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>, options: Options<State, TStateProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>

    @nativeInvoke
    operator fun <TStateProps, TDispatchProps, TOwnProps, TMergedProps, State> invoke(mapStateToProps: MapStateToProps<TStateProps, TOwnProps, State>, mapDispatchToProps: MapDispatchToPropsFunction<TDispatchProps, TOwnProps>, mergeProps: MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps>, options: Options<State, TStateProps, TOwnProps, TMergedProps>): InferableComponentEnhancerWithProps<TMergedProps, TOwnProps>
}

external var connect: Connect = definedExternally

external interface MapStateToProps<TStateProps, TOwnProps, State> {
    @nativeInvoke
    operator fun invoke(state: State, ownProps: TOwnProps): TStateProps
}

external interface MapStateToPropsFactory<TStateProps, TOwnProps, State> {
    @nativeInvoke
    operator fun invoke(initialState: State, ownProps: TOwnProps): MapStateToProps<TStateProps, TOwnProps, State>
}

external interface MapDispatchToPropsFunction<TDispatchProps, TOwnProps> {
    @nativeInvoke
    operator fun invoke(dispatch: Dispatch<dynamic>, ownProps: TOwnProps): TDispatchProps
}

external interface MapDispatchToPropsFactory<TDispatchProps, TOwnProps> {
    @nativeInvoke
    operator fun invoke(dispatch: Dispatch<dynamic>, ownProps: TOwnProps): dynamic /* TDispatchProps | MapDispatchToPropsFunction<TDispatchProps, TOwnProps> */
}

external interface MergeProps<TStateProps, TDispatchProps, TOwnProps, TMergedProps> {
    @nativeInvoke
    operator fun invoke(stateProps: TStateProps, dispatchProps: TDispatchProps, ownProps: TOwnProps): TMergedProps
}

external interface Options<State, TStateProps, TOwnProps, TMergedProps> : ConnectOptions {
    var pure: Boolean? get() = definedExternally; set(value) = definedExternally
    var areStatesEqual: ((nextState: State, prevState: State) -> Boolean)? get() = definedExternally; set(value) = definedExternally
    var areOwnPropsEqual: ((nextOwnProps: TOwnProps, prevOwnProps: TOwnProps) -> Boolean)? get() = definedExternally; set(value) = definedExternally
    var areStatePropsEqual: ((nextStateProps: TStateProps, prevStateProps: TStateProps) -> Boolean)? get() = definedExternally; set(value) = definedExternally
    var areMergedPropsEqual: ((nextMergedProps: TMergedProps, prevMergedProps: TMergedProps) -> Boolean)? get() = definedExternally; set(value) = definedExternally
}

external fun <S, TProps, TOwnProps, TFactoryOptions> connectAdvanced(selectorFactory: SelectorFactory<S, TProps, TOwnProps, TFactoryOptions>, connectOptions: ConnectOptions? /* ConnectOptions & TFactoryOptions */ = definedExternally /* null */): AdvancedComponentDecorator<TProps, TOwnProps> = definedExternally
external interface SelectorFactory<S, TProps, TOwnProps, TFactoryOptions> {
    @nativeInvoke
    operator fun invoke(dispatch: Dispatch<dynamic>, factoryOptions: TFactoryOptions): Selector<S, TProps, TOwnProps>
}

external interface Selector<S, TProps, TOwnProps> {
    @nativeInvoke
    operator fun invoke(state: S, ownProps: TOwnProps): TProps
}

external interface ConnectOptions {
    var getDisplayName: ((componentName: String) -> String)? get() = definedExternally; set(value) = definedExternally
    var methodName: String? get() = definedExternally; set(value) = definedExternally
    var renderCountProp: String? get() = definedExternally; set(value) = definedExternally
    var shouldHandleStateChanges: Boolean? get() = definedExternally; set(value) = definedExternally
    var storeKey: String? get() = definedExternally; set(value) = definedExternally
    var withRef: Boolean? get() = definedExternally; set(value) = definedExternally
}

external interface ProviderProps : RProps {
    var store: Store<dynamic, dynamic>? get() = definedExternally; set(value) = definedExternally
    var children: Any? get() = definedExternally; set(value) = definedExternally
}

external fun createProvider(storeKey: String): Any? = definedExternally
external val Provider: RClass<ProviderProps>
