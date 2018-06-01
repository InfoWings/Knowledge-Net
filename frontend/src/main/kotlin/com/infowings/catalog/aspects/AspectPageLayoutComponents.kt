package com.infowings.catalog.aspects

import com.infowings.catalog.aspects.editconsole.aspectConsole
import com.infowings.catalog.aspects.editconsole.popup.unsafeChangesWindow
import com.infowings.catalog.aspects.filter.AspectsFilter
import com.infowings.catalog.aspects.filter.aspectExcludeFilterComponent
import com.infowings.catalog.aspects.filter.aspectSubjectFilterComponent
import com.infowings.catalog.aspects.model.AspectsModel
import com.infowings.catalog.aspects.sort.aspectSort
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectOrderBy
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.blueprint.Position
import com.infowings.catalog.wrappers.blueprint.Toast
import com.infowings.catalog.wrappers.blueprint.Toaster
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder
import react.dom.div

fun RBuilder.aspectPageHeader(
    onFetchAspects: (List<AspectOrderBy>) -> Unit,
    filter: AspectsFilter,
    setFilterSubjects: (List<SubjectData?>) -> Unit,
    setFilterAspects: (List<AspectData>) -> Unit
) = div(classes = "aspect-tree-view__header") {
    aspectSort {
        attrs {
            onFetchAspect = onFetchAspects
        }
    }
    aspectSubjectFilterComponent {
        attrs {
            subjectsFilter = filter.subjects
            onChange = setFilterSubjects
        }
    }
    aspectExcludeFilterComponent {
        attrs {
            selectedAspects = filter.excludedAspects
            onChange = setFilterAspects
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
    Toaster {
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
