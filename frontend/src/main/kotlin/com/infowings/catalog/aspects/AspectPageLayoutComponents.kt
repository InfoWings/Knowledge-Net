package com.infowings.catalog.aspects

import com.infowings.catalog.aspects.editconsole.aspectConsole
import com.infowings.catalog.aspects.editconsole.popup.unsafeChangesWindow
import com.infowings.catalog.aspects.filter.AspectsFilter
import com.infowings.catalog.aspects.filter.aspectExcludeFilterComponent
import com.infowings.catalog.aspects.filter.aspectSubjectFilterComponent
import com.infowings.catalog.aspects.model.AspectsModel
import com.infowings.catalog.aspects.search.aspectSearchComponent
import com.infowings.catalog.aspects.sort.aspectSort
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.*
import com.infowings.catalog.wrappers.blueprint.*
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder
import react.dom.div

fun RBuilder.aspectPageHeader(
    onOrderByChanged: (List<SortOrder>) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    filter: AspectsFilter,
    setFilterSubjects: (List<SubjectData?>) -> Unit,
    setFilterAspects: (List<AspectHint>) -> Unit,
    refreshAspects: () -> Unit,
    aspectByGuid: Map<String, AspectData>
) {
    div(classes = "aspect-tree-view__header aspect-header") {
        div(classes = "aspect-header__sort-search") {
            aspectSort {
                attrs {
                    this.onOrderByChanged = onOrderByChanged
                }
            }
            aspectSearchComponent {
                attrs {
                    onConfirmSearch = onSearchQueryChanged
                }
            }
        }
        div(classes = "aspect-header__text-filters") {
            aspectSubjectFilterComponent {
                attrs {
                    subjectsFilter = filter.subjects
                    onChange = setFilterSubjects
                }
            }

            aspectExcludeFilterComponent {
                attrs {
                    selectedAspects = filter.excludedAspects.map { it.copy(name = aspectByGuid[it.guid]?.nameWithSubject() ?: it.name ) }
                    onChange = setFilterAspects
                }
            }
        }
        div("aspect-header__refresh") {
            Button {
                attrs {
                    icon = "refresh"
                    onClick = { refreshAspects() }
                }
            }
        }
    }
}

fun RBuilder.aspectPageContent(
    filteredAspects: List<AspectData>,
    aspectContext: Map<String, AspectData>,
    aspectsModel: AspectsModel,
    selectedAspect: AspectData,
    selectedAspectPropertyIndex: Int?
) {
    aspectTreeView {
        attrs {
            aspects = filteredAspects
            this.aspectContext = aspectContext
            selectedAspectId = selectedAspect.id
            selectedPropertyIndex = selectedAspectPropertyIndex
            this.aspectsModel = aspectsModel
        }
    }
    aspectConsole {
        attrs {
            aspect = selectedAspect
            propertyIndex = selectedAspectPropertyIndex
            this.aspectContext = aspectContext
            this.aspectsModel = aspectsModel
        }
    }
}

fun RBuilder.aspectPageOverlay(
    isUnsafeSelection: Boolean,
    onCloseUnsafeSelection: () -> Unit,
    errorMessages: List<String>,
    onDismissErrorMessage: (message: String) -> Unit
) {
    unsafeChangesWindow(isUnsafeSelection, onCloseUnsafeSelection)
    child(Toaster::class) {
        attrs {
            position = Position.TOP_RIGHT
        }
        errorMessages.reversed().forEach { errorMessage ->
            Toast {
                attrs {
                    icon = "warning-sign"
                    intent = Intent.DANGER
                    message = errorMessage.asReactElement()
                    onDismiss = { onDismissErrorMessage(errorMessage) }
                    timeout = 9000
                }
            }
        }
    }
}
