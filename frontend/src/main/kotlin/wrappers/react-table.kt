@file:JsModule("react-table")

package wrappers

import react.RClass
import react.RProps
import react.ReactElement

@JsName("default")
external val ReactTable: RClass<RTableProps>
external val ReactTableDefaults: RTableProps

external interface RTableRenderer : RClass<RTableRendererProps>

external interface RTableProps : RProps {
    var data: Array<dynamic>
    var loading: Boolean?
    var showPagination: Boolean?
    var showPaginationTop: Boolean?
    var showPaginationBottom: Boolean?
    var showPageSizeOptions: Array<Int>?
    var defaultPageSize: Int?
    var minRows: Int?
    var showPageJump: Boolean?
    var collapseOnSortingChange: Boolean?
    var collapseOnPageChange: Boolean?
    var collapseOnDataChange: Boolean?
    var freezeWhenExpanded: Boolean?
    var sortable: Boolean?
    var multiSort: Boolean?
    var resizable: Boolean?
    var filterable: Boolean?
    var defaultSortDesc: Boolean?
    //    var defaultSorted:   // Type???
//    var defaultFiltered: // Type???
//    var defaultResized:  // Type???
//    var defaultExpanded: // Type???
    var defaultFilterMethod: ((filter: dynamic, row: dynamic, column: dynamic) -> Boolean)?
    var defaultSortMethod: ((firstRow: dynamic, secondRow: dynamic, desc: Boolean) -> Int)?
    var PadRowComponent: (() -> ReactElement)? //() => <span>&nbsp;</span>, // the content rendered inside of a padding row

    // Controlled State Overrides (see Fully Controlled Component section)
    var page: Int?
    var pageSize: Int?
    var sorted: Array<SortingModel>?
    var filtered: Array<FilteringModel>?
    var resized: Array<ResizingModel>?
    var expanded: dynamic // the nested row indexes on the current page that should appear expanded. See docs

    // Controlled State Callbacks
    var onPageChange: ((pageIndex: Int) -> Unit)?
    var onPageSizeChange: ((pageSize: Double, pageIndex: Int) -> Unit)?
    var onSortedChange: ((newSorted: Array<SortingModel>, column: dynamic, shiftKey: Boolean) -> Unit)?
    var onFilteredChange: ((newExpanded: dynamic, index: dynamic, event: dynamic) -> Unit)?
    var onResizedChange: ((column: dynamic, value: dynamic) -> Unit)?
    var onExpandedChange: ((newResized: dynamic, event: dynamic) -> Unit)?

    // Pivoting
    var pivotBy: Array<String>?

    // Key Constants
    var pivotValKey: String?       // Default: '_pivotVal',
    var pivotIDKey: String?        // Default: '_pivotID',
    var subRowsKey: String?        // Default: '_subRows',
    var aggregatedKey: String?     // Default: '_aggregated',
    var nestingLevelKey: String?   // Default: '_nestingLevel',
    var originalKey: String?       // Default: '_original',
    var indexKey: String?          // Default: '_index',
    var groupedByPivotKey: String? // Default: '_groupedByPivot',

    // Server-side callbacks
    var onFetchData: (() -> dynamic)?

    // Classes
    var className: String?
    var style: dynamic

    // Component decorators
    var getProps: (() -> dynamic)?
    var getTableProps: (() -> dynamic)?
    var getTheadGroupProps: (() -> dynamic)?
    var getTheadGroupTrProps: (() -> dynamic)?
    var getTheadGroupThProps: (() -> dynamic)?
    var getTheadProps: (() -> dynamic)?
    var getTheadTrProps: (() -> dynamic)?
    var getTheadThProps: (() -> dynamic)?
    var getTheadFilterProps: (() -> dynamic)?
    var getTheadFilterTrProps: (() -> dynamic)?
    var getTheadFilterThProps: (() -> dynamic)?
    var getTbodyProps: (() -> dynamic)?
    var getTrGroupProps: (() -> dynamic)?
    var getTrProps: (() -> dynamic)?
    var getThProps: (() -> dynamic)?
    var getTdProps: (() -> dynamic)?
    var getTfootProps: (() -> dynamic)?
    var getTfootTrProps: (() -> dynamic)?
    var getTfootThProps: (() -> dynamic)?
    var getPaginationProps: (() -> dynamic)?
    var getLoadingProps: (() -> dynamic)?
    var getNoDataProps: (() -> dynamic)?
    var getResizerProps: (() -> dynamic)?

    // Global Column Defaults
    var column: Array<RTableColumnProps>

    // Global Expander Column Defaults
    var expanderDefaults: ExpanderDescriptor?

    // Global Pivot Column Defaults
    var pivotDefaults: dynamic

    // Text
    var previousText: String? // Default: 'Previous',
    var nextText: String?     // Default: 'Next',
    var loadingText: String?  // Default: 'Loading...',
    var noDataText: String?   // Default: 'No rows found',
    var pageText: String?     // Default: 'Page',
    var ofText: String?       // Default: 'of',
    var rowsText: String?     // Default: 'rows',
}

external interface RTableColumnProps {
    // Renderers
    var Cell: RTableRenderer?
    var Header: RTableRenderer?
    var Footer: RTableRenderer?
    var Aggregated: RTableRenderer?
    var Pivot: RTableRenderer?
    var PivotValue: RTableRenderer?
    var Expander: RTableRenderer?
    var Filter: RTableRenderer?

    // Standard options
    var accessor: dynamic
    var id: String
    var sortable: Boolean?
    var resizable: Boolean?
    var filterable: Boolean?
    var show: Boolean?
    var width: Double?
    var minWidth: Double?
    var maxWidth: Double?

    //Special
    var pivot: Boolean?
    var expander: Boolean?

    // Cells only
    var className: String?
    var style: dynamic
    var getProps: (() -> dynamic)?

    // Headers only
    var headerClassName: String?
    var headerStyle: dynamic
    var getHeaderProps: (() -> dynamic)?

    // For Header groups only*
    var columns: Array<RTableColumnProps>?

    // Footers only
    var footerClassName: String?
    var footerStyle: dynamic
    var getFooterProps: (() -> dynamic)?

    var filterAll: Boolean
    var filterMethod: dynamic    // Function signature ???
    var sortMethod: dynamic      // Function signature ???
    var defaultSortDesc: dynamic // Function signature ???
}

external interface RTableRendererProps : RProps {
    var row: dynamic // the materialized row of data
    var original: dynamic // the original row of data
    var index: Int // the index of the row in the original array
    var viewIndex: Int // the index of the row relative to the current view
    var level: Int // the nesting level of this row
    var nestingPath: String // the nesting path of this row
    var aggregated: Boolean // true if this row's values were aggregated
    var groupedByPivot: Boolean // true if this row was produced by a pivot
    var subRows: dynamic // any sub rows defined by the `subRowKey` prop

    // Cells-level props
    var isExpanded: Boolean // true if this row is expanded
    var value: dynamic // the materialized value of this cell
    var resized: dynamic // the resize information for this cell's column
    var show: Boolean // true if the column is visible
    var width: Double // the resolved width of this cell
    var maxWidth: Double // the resolved maxWidth of this cell
    var tdProps: dynamic // the resolved tdProps from `getTdProps` for this cell
    var columnProps: dynamic // the resolved column props from 'getProps' for this cell's column
    var classes: Array<String> // the resolved array of classes for this cell
    var styles: dynamic // the resolved styles for this cell
}

external interface SortingModel {
    var id: String
    var desc: Boolean
}

external interface FilteringModel {
    var id: String
    var value: String
}

external interface ResizingModel {
    var id: String
    var value: Double
}

external interface ExpanderDescriptor {
    var sortable: Boolean?
    var resizable: Boolean?
    var filterable: Boolean?
    var width: Double?
}




