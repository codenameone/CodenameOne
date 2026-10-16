// Codename One Flutter runtime API stubs — widgetsMore category (new_gallery, Pass 2).
//
// Signature-only declarations resolved by the Dart transpiler; each maps to a
// hand-written Java runtime class via @JavaName. Following the established
// convention, parameter types that belong to categories this file does not own
// (ScrollController, ScrollPhysics, EdgeInsetsGeometry, MouseCursor,
// DragStartBehavior, ...) are declared loosely as `Object?` — the transpiler
// resolves those argument expressions against whichever category owns them.
//
// Callback parameters use the transpiler-internal typedef SAM names
// (VoidCallback / IntCallback / BoolCallback / WidgetBuilder /
// IndexedWidgetBuilder / DynamicCallback). Callbacks whose argument is a
// value type (RangeValues, DismissDirection) or which take multiple arguments
// are typed `Object?`; the concrete lambdas in the gallery are explicitly
// typed, so the Java runtime setter binds them to the precise Funcs SAM.

// ======================================================================
// Tabs
// ======================================================================

@JavaName('com.codename1.flutter.material.TabController')
class TabController {
  external TabController({int? initialIndex, required int length, Duration? animationDuration, TickerProvider? vsync});
  external int get index;
  external set index(int value);
  external int get length;
  external int get previousIndex;
  external bool get indexIsChanging;
  external double get offset;
  external Animation<double>? get animation;
  external void animateTo(int value, {Duration? duration, Curve? curve});
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void dispose();
}

@JavaName('com.codename1.flutter.material.Tab')
class Tab extends Widget {
  external Tab({Key? key, String? text, Widget? icon, Object? iconMargin, double? height, Widget? child});
}

@JavaName('com.codename1.flutter.material.TabBar')
class TabBar extends Widget {
  external TabBar({Key? key, required List<Widget> tabs, TabController? controller, bool? isScrollable, Object? padding, Color? indicatorColor, double? indicatorWeight, Object? indicatorPadding, Object? indicator, Object? indicatorSize, Color? labelColor, TextStyle? labelStyle, Object? labelPadding, Color? unselectedLabelColor, TextStyle? unselectedLabelStyle, Object? dragStartBehavior, Object? mouseCursor, bool? enableFeedback, IntCallback? onTap, Object? physics});
}

@JavaName('com.codename1.flutter.material.TabBarView')
class TabBarView extends Widget {
  external TabBarView({Key? key, required List<Widget> children, TabController? controller, Object? physics, Object? dragStartBehavior, double? viewportFraction, Clip? clipBehavior});
}

@JavaName('com.codename1.flutter.material.DefaultTabController')
class DefaultTabController extends Widget {
  external DefaultTabController({Key? key, required int length, int? initialIndex, Duration? animationDuration, required Widget child});
  external static TabController of(BuildContext context);
}

// ======================================================================
// Range slider
// ======================================================================

@JavaName('com.codename1.flutter.material.RangeValues')
class RangeValues {
  external RangeValues(double start, double end);
  external double get start;
  external double get end;
}

@JavaName('com.codename1.flutter.material.RangeLabels')
class RangeLabels {
  external RangeLabels(String start, String end);
  external String get start;
  external String get end;
}

@JavaName('com.codename1.flutter.material.RangeSlider')
class RangeSlider extends Widget {
  external RangeSlider({Key? key, required RangeValues values, Object? onChanged, Object? onChangeStart, Object? onChangeEnd, double? min, double? max, int? divisions, RangeLabels? labels, Color? activeColor, Color? inactiveColor, Object? semanticFormatterCallback});
}

// ======================================================================
// Spacer
// ======================================================================

@JavaName('com.codename1.flutter.widgets.Spacer')
class Spacer extends Widget {
  external Spacer({Key? key, int? flex});
}

// ======================================================================
// Popup menu divider
// ======================================================================

@JavaName('com.codename1.flutter.material.PopupMenuDivider')
class PopupMenuDivider extends PopupMenuEntry<dynamic> {
  external PopupMenuDivider({Key? key, double? height});
}

// ======================================================================
// Scrollbars
// ======================================================================

@JavaName('com.codename1.flutter.widgets.RawScrollbar')
class RawScrollbar extends Widget {
  external RawScrollbar({Key? key, required Widget child, Object? controller, bool? thumbVisibility, Object? thumbColor, Object? radius, double? thickness, bool? interactive, Object? notificationPredicate, Object? scrollbarOrientation});
}

// ======================================================================
// Slivers (minimal: modeled as a scrollable list)
// ======================================================================

@JavaName('com.codename1.flutter.widgets.SliverChildDelegate')
abstract class SliverChildDelegate {}

@JavaName('com.codename1.flutter.widgets.SliverChildBuilderDelegate')
class SliverChildBuilderDelegate extends SliverChildDelegate {
  external SliverChildBuilderDelegate(IndexedWidgetBuilder builder, {int? childCount, bool? addAutomaticKeepAlives, bool? addRepaintBoundaries, bool? addSemanticIndexes});
}

@JavaName('com.codename1.flutter.widgets.SliverChildListDelegate')
class SliverChildListDelegate extends SliverChildDelegate {
  external SliverChildListDelegate(List<Widget> children, {bool? addAutomaticKeepAlives, bool? addRepaintBoundaries, bool? addSemanticIndexes});
}

@JavaName('com.codename1.flutter.widgets.CustomScrollView')
class CustomScrollView extends Widget {
  external CustomScrollView({Key? key, List<Widget> slivers, Object? controller, Object? scrollDirection, bool? reverse, bool? shrinkWrap, Object? physics, double? cacheExtent, Object? primary, Clip? clipBehavior});
}

@JavaName('com.codename1.flutter.widgets.SliverList')
class SliverList extends Widget {
  external SliverList({Key? key, required SliverChildDelegate delegate});
}

@JavaName('com.codename1.flutter.widgets.SliverGrid')
class SliverGrid extends Widget {
  external SliverGrid({Key? key, required SliverChildDelegate delegate, required Object gridDelegate});
}

@JavaName('com.codename1.flutter.widgets.SliverToBoxAdapter')
class SliverToBoxAdapter extends Widget {
  external SliverToBoxAdapter({Key? key, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.SliverPadding')
class SliverPadding extends Widget {
  external SliverPadding({Key? key, required Object padding, Widget? sliver});
}

@JavaName('com.codename1.flutter.widgets.SliverFillRemaining')
class SliverFillRemaining extends Widget {
  external SliverFillRemaining({Key? key, Widget? child, bool? hasScrollBody, bool? fillOverscroll});
}

@JavaName('com.codename1.flutter.widgets.SliverAppBar')
class SliverAppBar extends Widget {
  external SliverAppBar({Key? key, Widget? title, Widget? leading, List<Widget>? actions, Widget? flexibleSpace, Color? backgroundColor, bool? pinned, bool? floating, bool? snap, double? expandedHeight, bool? automaticallyImplyLeading, Widget? bottom, bool? centerTitle, double? elevation});
}

// ======================================================================
// Nested scroll view
// ======================================================================

@JavaName('com.codename1.flutter.widgets.NestedScrollView')
class NestedScrollView extends Widget {
  external NestedScrollView({Key? key, required Object headerSliverBuilder, required Widget body, Object? controller, Object? scrollDirection, bool? reverse, Object? physics, bool? floatHeaderSlivers});
}

// ======================================================================
// Refresh indicator
// ======================================================================

@JavaName('com.codename1.flutter.material.RefreshIndicator')
class RefreshIndicator extends Widget {
  external RefreshIndicator({Key? key, required Widget child, double? displacement, Object? onRefresh, Color? color, Color? backgroundColor, double? strokeWidth, Object? notificationPredicate, String? semanticsLabel, String? semanticsValue});
}

// ======================================================================
// Dismissible
// ======================================================================

@JavaName('com.codename1.flutter.widgets.DismissDirection')
enum DismissDirection { vertical, horizontal, endToStart, startToEnd, up, down, none }

@JavaName('com.codename1.flutter.widgets.Dismissible')
class Dismissible extends Widget {
  external Dismissible({required Key key, required Widget child, Widget? background, Widget? secondaryBackground, Object? confirmDismiss, VoidCallback? onResize, Object? onUpdate, Object? onDismissed, Object? direction, Object? resizeDuration, Object? dismissThresholds, Object? movementDuration, double? crossAxisEndOffset, Object? dragStartBehavior, Object? behavior});
}

// ======================================================================
// Reorderable list view
// ======================================================================

@JavaName('com.codename1.flutter.widgets.ReorderableListView')
class ReorderableListView extends Widget {
  external ReorderableListView({Key? key, List<Widget> children, required Object onReorder, Object? padding, Widget? header, Object? scrollDirection, bool? shrinkWrap, Object? physics, bool? buildDefaultDragHandles});
  external static ReorderableListView builder({Key? key, required IndexedWidgetBuilder itemBuilder, required int itemCount, required Object onReorder, Object? padding, Object? scrollDirection, bool? shrinkWrap});
}

// ======================================================================
// Animated list
// ======================================================================

@JavaName('com.codename1.flutter.widgets.AnimatedListState')
class AnimatedListState {
  external void insertItem(int index, {Duration? duration});
  external void removeItem(int index, Object builder, {Duration? duration});
}

@JavaName('com.codename1.flutter.widgets.AnimatedList')
class AnimatedList extends Widget {
  external AnimatedList({Key? key, required Object itemBuilder, int? initialItemCount, Object? scrollDirection, bool? reverse, Object? controller, Object? primary, Object? physics, bool? shrinkWrap, Object? padding, Clip? clipBehavior});
  external static AnimatedListState of(BuildContext context);
}

// ======================================================================
// Stepper
// ======================================================================

@JavaName('com.codename1.flutter.material.StepState')
enum StepState { indexed, editing, complete, disabled, error }

@JavaName('com.codename1.flutter.material.StepperType')
enum StepperType { vertical, horizontal }

@JavaName('com.codename1.flutter.material.Step')
class Step {
  external Step({required Widget title, Widget? subtitle, required Widget content, Object? state, bool? isActive, Object? stepStyle});
}

@JavaName('com.codename1.flutter.material.Stepper')
class Stepper extends Widget {
  external Stepper({Key? key, required List<Step> steps, Object? physics, StepperType? type, int? currentStep, IntCallback? onStepTapped, VoidCallback? onStepContinue, VoidCallback? onStepCancel, Object? controlsBuilder, double? elevation, Object? margin});
}

// ======================================================================
// Expansion panels / tile
// ======================================================================

@JavaName('com.codename1.flutter.material.ExpansionPanel')
class ExpansionPanel {
  external ExpansionPanel({required Object headerBuilder, required Widget body, bool? isExpanded, bool? canTapOnHeader, Color? backgroundColor});
}

@JavaName('com.codename1.flutter.material.ExpansionPanelList')
class ExpansionPanelList extends Widget {
  external ExpansionPanelList({Key? key, List<ExpansionPanel> children, Object? expansionCallback, Object? animationDuration, Object? expandedHeaderPadding, double? elevation});
}

@JavaName('com.codename1.flutter.material.ExpansionTile')
class ExpansionTile extends Widget {
  external ExpansionTile({Key? key, required Widget title, Widget? subtitle, List<Widget> children, Widget? leading, Widget? trailing, bool? initiallyExpanded, BoolCallback? onExpansionChanged, Object? childrenPadding, Color? backgroundColor, Color? collapsedBackgroundColor, Color? textColor, Color? iconColor, Object? tilePadding, Object? expandedAlignment, Object? expandedCrossAxisAlignment});
}

// ======================================================================
// Data table
// ======================================================================

@JavaName('com.codename1.flutter.material.DataColumn')
class DataColumn {
  external DataColumn({required Widget label, String? tooltip, bool? numeric, Object? onSort});
}

@JavaName('com.codename1.flutter.material.DataCell')
class DataCell {
  external DataCell(Widget child, {bool? placeholder, bool? showEditIcon, VoidCallback? onTap, VoidCallback? onLongPress, Object? onTapDown});
}

@JavaName('com.codename1.flutter.material.DataRow')
class DataRow {
  external DataRow({Key? key, bool? selected, BoolCallback? onSelectChanged, Object? onLongPress, Object? color, required List<DataCell> cells});
  external static DataRow byIndex({required int index, bool? selected, BoolCallback? onSelectChanged, Object? onLongPress, Object? color, required List<DataCell> cells});
}

@JavaName('com.codename1.flutter.material.DataTable')
class DataTable extends Widget {
  external DataTable({Key? key, required List<DataColumn> columns, required List<DataRow> rows, int? sortColumnIndex, bool? sortAscending, BoolCallback? onSelectAll, double? dataRowHeight, double? headingRowHeight, double? horizontalMargin, double? columnSpacing, bool? showCheckboxColumn, Object? decoration});
}

@JavaName('com.codename1.flutter.material.DataTableSource')
class DataTableSource extends ChangeNotifier {
  external DataRow? getRow(int index);
  external int get rowCount;
  external bool get isRowCountApproximate;
  external int get selectedRowCount;
}

@JavaName('com.codename1.flutter.material.PaginatedDataTable')
class PaginatedDataTable extends Widget {
  external PaginatedDataTable({Key? key, Widget? header, List<Widget>? actions, required List<DataColumn> columns, int? sortColumnIndex, bool? sortAscending, BoolCallback? onSelectAll, double? dataRowHeight, double? headingRowHeight, double? horizontalMargin, double? columnSpacing, bool? showCheckboxColumn, bool? showFirstLastButtons, int? initialFirstRowIndex, IntCallback? onPageChanged, int? rowsPerPage, List<int>? availableRowsPerPage, IntCallback? onRowsPerPageChanged, required DataTableSource source, Object? checkboxHorizontalMargin, Object? controller, bool? primary});
  // The default value for `rowsPerPage` — Flutter's
  // `PaginatedDataTable.defaultRowsPerPage` (== 10).
  external static int get defaultRowsPerPage;
}

// ======================================================================
// Chips
// ======================================================================

@JavaName('com.codename1.flutter.material.Chip')
class Chip extends Widget {
  external Chip({Key? key, Widget? avatar, required Widget label, TextStyle? labelStyle, Object? labelPadding, Widget? deleteIcon, VoidCallback? onDeleted, Color? deleteIconColor, String? deleteButtonTooltipMessage, Object? side, Object? shape, Clip? clipBehavior, Color? backgroundColor, Object? padding, Object? visualDensity, Object? materialTapTargetSize, double? elevation, Color? shadowColor});
}

@JavaName('com.codename1.flutter.material.InputChip')
class InputChip extends Widget {
  external InputChip({Key? key, Widget? avatar, required Widget label, TextStyle? labelStyle, Object? labelPadding, bool? selected, bool? isEnabled, BoolCallback? onSelected, Widget? deleteIcon, VoidCallback? onDeleted, Color? deleteIconColor, VoidCallback? onPressed, Object? pressElevation, Color? disabledColor, Color? selectedColor, Object? tooltip, Object? side, Object? shape, Color? backgroundColor, Object? padding, double? elevation});
}

@JavaName('com.codename1.flutter.material.ChoiceChip')
class ChoiceChip extends Widget {
  external ChoiceChip({Key? key, Widget? avatar, required Widget label, TextStyle? labelStyle, Object? labelPadding, required bool selected, BoolCallback? onSelected, Object? pressElevation, Color? disabledColor, Color? selectedColor, Object? tooltip, Object? side, Object? shape, Color? backgroundColor, Object? padding, double? elevation});
}

@JavaName('com.codename1.flutter.material.FilterChip')
class FilterChip extends Widget {
  external FilterChip({Key? key, Widget? avatar, required Widget label, TextStyle? labelStyle, Object? labelPadding, required bool selected, required BoolCallback? onSelected, Object? pressElevation, Color? disabledColor, Color? selectedColor, Object? tooltip, Object? side, Object? shape, Color? backgroundColor, Object? padding, double? elevation});
}

@JavaName('com.codename1.flutter.material.ActionChip')
class ActionChip extends Widget {
  external ActionChip({Key? key, Widget? avatar, required Widget label, TextStyle? labelStyle, Object? labelPadding, required VoidCallback? onPressed, Object? pressElevation, Object? tooltip, Object? side, Object? shape, Color? backgroundColor, Object? padding, double? elevation});
}

// ======================================================================
// Circle avatar
// ======================================================================

@JavaName('com.codename1.flutter.material.CircleAvatar')
class CircleAvatar extends Widget {
  external CircleAvatar({Key? key, Widget? child, Color? backgroundColor, Color? foregroundColor, ImageProvider? backgroundImage, ImageProvider? foregroundImage, Object? onBackgroundImageError, double? radius, double? minRadius, double? maxRadius});
}

// ======================================================================
// Progress indicator
// ======================================================================

@JavaName('com.codename1.flutter.material.LinearProgressIndicator')
class LinearProgressIndicator extends Widget {
  external LinearProgressIndicator({Key? key, double? value, Color? backgroundColor, Color? color, Object? valueColor, double? minHeight, String? semanticsLabel, String? semanticsValue, Object? borderRadius});
}

// ======================================================================
// Banners
// ======================================================================

@JavaName('com.codename1.flutter.material.BannerLocation')
enum BannerLocation { topStart, topEnd, bottomStart, bottomEnd }

@JavaName('com.codename1.flutter.material.Banner')
class Banner extends Widget {
  external Banner({Key? key, Widget? child, required String message, Object? textDirection, required Object location, Object? layoutDirection, Color? color, TextStyle? textStyle});
}

@JavaName('com.codename1.flutter.material.MaterialBanner')
class MaterialBanner extends Widget {
  external MaterialBanner({Key? key, required Widget content, TextStyle? contentTextStyle, required List<Widget> actions, double? elevation, Widget? leading, Color? backgroundColor, Color? surfaceTintColor, Color? shadowColor, Color? dividerColor, Object? padding, Object? leadingPadding, bool? forceActionsBelow, Object? overflowAlignment, Object? animation, Object? onVisible});
}

// ======================================================================
// Bottom sheet
// ======================================================================

@JavaName('com.codename1.flutter.material.BottomSheet')
class BottomSheet extends Widget {
  external BottomSheet({Key? key, Object? animationController, bool? enableDrag, required VoidCallback onClosing, required WidgetBuilder builder, Color? backgroundColor, double? elevation, Object? shape, Clip? clipBehavior, Object? constraints});
}

@JavaName('com.codename1.flutter.material.BottomSheets.showModalBottomSheet')
external Future<dynamic> showModalBottomSheet({required BuildContext context, required WidgetBuilder builder, Color? backgroundColor, double? elevation, Object? shape, Clip? clipBehavior, Object? constraints, Color? barrierColor, bool? isScrollControlled, bool? useRootNavigator, bool? isDismissible, bool? enableDrag, bool? showDragHandle, Object? routeSettings, Object? transitionAnimationController});

// ======================================================================
// Hero (minimal: renders child, no flight animation)
// ======================================================================

@JavaName('com.codename1.flutter.widgets.Hero')
class Hero extends Widget {
  external Hero({Key? key, required Object tag, Object? createRectTween, Object? flightShuttleBuilder, Object? placeholderBuilder, bool? transitionOnUserGestures, required Widget child});
}
