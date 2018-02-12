@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "EXTERNAL_DELEGATION", "NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@file:JsModule("redux")

package wrappers

external interface Action<T> {
    //var type: T
}

external interface AnyAction : Action<dynamic> {
    @nativeGetter
    operator fun get(extraProps: String): dynamic

    @nativeSetter
    operator fun set(extraProps: String, value: dynamic)
}

external interface Dispatch<in A : Action<dynamic>> {
    @nativeInvoke
    operator fun <T : A> invoke(action: T): T
}

external interface Unsubscribe {
    @nativeInvoke
    operator fun invoke()
}

external interface Store<S, A : Action<dynamic>> {
    var dispatch: Dispatch<A>
    fun getState(): S
    fun subscribe(listener: () -> Unit): Unsubscribe
    fun replaceReducer(nextReducer: (state: S, action: A) -> S)
}

external interface StoreCreator {
    @nativeInvoke
    operator fun <S, A : Action<dynamic>> invoke(reducer: (state: S, action: A) -> S, enhancer: ((next: (reducer: (state: S, action: A) -> S, preloadedState: Any? /*= null*/) -> Store<S /* S & StateExt */, A> /* Store<S /* S & StateExt */, A> & Ext */) -> (reducer: (state: S, action: A) -> S, preloadedState: Any? /*= null*/) -> Store<S /* S & StateExt */, A> /* Store<S /* S & StateExt */, A> & Ext */)? = definedExternally /* null */): Store<S /* S & StateExt */, A> /* Store<S /* S & StateExt */, A> & Ext */

    @nativeInvoke
    operator fun <S, A : Action<dynamic>> invoke(reducer: (state: S, action: A) -> S, preloadedState: dynamic, enhancer: ((next: (reducer: (state: S, action: A) -> S, preloadedState: Any? /*= null*/) -> Store<S /* S & StateExt */, A> /* Store<S /* S & StateExt */, A> & Ext */) -> (reducer: (state: S, action: A) -> S, preloadedState: Any? /*= null*/) -> Store<S /* S & StateExt */, A> /* Store<S /* S & StateExt */, A> & Ext */)? = definedExternally /* null */): Store<S /* S & StateExt */, AnyAction> /* Store<S /* S & StateExt */, AnyAction> & Ext */
}

external interface MiddlewareAPI<D : Dispatch<AnyAction>, S> {
    var dispatch: D
    fun getState(): S
}

external interface Middleware<DispatchExt, S, D : Dispatch<AnyAction>> {
    @nativeInvoke
    operator fun invoke(api: MiddlewareAPI<D, S>): (next: Dispatch<AnyAction>) -> (action: dynamic) -> dynamic
}

external interface `T$0`<Ext1> {
    var dispatch: Ext1
}

external interface `T$1`<Ext1> {
    var dispatch: Ext1 /* Ext1 & Ext2 */
}

external interface `T$2`<Ext1> {
    var dispatch: Ext1 /* Ext1 & Ext2 & Ext3 */
}

external interface `T$3`<Ext1> {
    var dispatch: Ext1 /* Ext1 & Ext2 & Ext3 & Ext4 */
}

external interface `T$4`<Ext1> {
    var dispatch: Ext1 /* Ext1 & Ext2 & Ext3 & Ext4 & Ext5 */
}

external interface `T$5`<Ext> {
    var dispatch: Ext
}

external interface ActionCreator<A> {
    @nativeInvoke
    operator fun invoke(vararg args: dynamic): A
}

external interface ActionCreatorsMapObject<A> {
    @nativeGetter
    operator fun get(key: String): ActionCreator<A>?

    @nativeSetter
    operator fun set(key: String, value: ActionCreator<A>)
}

external val createStore: dynamic = definedExternally
external val combineReducers: dynamic = definedExternally
external val applyMiddleware: dynamic = definedExternally
