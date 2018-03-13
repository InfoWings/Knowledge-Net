@file:JsModule("react-select")

package com.infowings.catalog.wrappers.select


// Core components
@JsName("default")
external val Select: SelectComponent<SelectOption> = definedExternally
external val Async: AsyncComponent<SelectOption> = definedExternally
external val AsyncCreatable: AsyncCreatableComponent<SelectOption> = definedExternally
external val Creatable: CreatableComponent<SelectOption> = definedExternally

// Additional default functions and (sub)components
//external val Value: RClass<SelectValueProps> = definedExternally
//external val Option: RClass<SelectOptionProps> = definedExternally
//external (fun/val) defaultMenuRenderer: ???
//external (fun/val) defaultArrowRenderer: ???
//external (fun/val) defaultClearRenderer: ???
//external (fun/val) defaultFilterOptions: ???
