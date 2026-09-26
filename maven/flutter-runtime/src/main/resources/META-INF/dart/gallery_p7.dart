// Codename One Flutter runtime API stubs — "flutterApi" category (new_gallery, Pass 7).
//
// The concrete-type member / constructor / identifier gaps that survived Passes
// 1-6 once the dynamic-receiver cascade collapsed (diagnostics E0135 unresolved
// constructor, E0137 unresolved method, E0132 unresolved member, E0129
// unresolved identifier). Every symbol here names a REAL Flutter (framework,
// rendering, gestures, physics, semantics, animations-package, provider or
// flutter_localized_countries) API; the shapes mirror the real signatures.
//
// Members that hang off types ALREADY declared in another stub file
// (Color.value, EdgeInsets.top/bottom, ThemeData.sliderTheme, MediaQueryData.
// viewInsets, BottomNavigationBarItem.icon/label, Size.center, RRect.middleRect,
// Route.settings, NavigatorState.push/popUntil, ScrollPhysics.createBallistic-
// Simulation, AnimationController.fling, AnimationStatus.isDismissed, ...) were
// appended to their owning classes in place. Only the brand-new types live here.
//
// Conventions (see gallery_p4_apitail.dart / gallery_p6.dart headers):
//  - positional constructor params -> Java constructor arguments
//  - named constructor params       -> void setter methods of the same name
//  - named constructors (X.name)    -> `external static X name(...)`
//  - top-level functions            -> `@JavaName('fqcn.method') external ...`
//  - instance getters               -> no-arg method calls
//  - callbacks / types owned elsewhere are declared loosely as `Object?`.

// ======================================================================
// Render tree — RenderObject / RenderBox / PaintingContext
// ======================================================================

// The base of the render tree — Flutter's `RenderObject`. new_gallery reaches
// one via `BuildContext.findRenderObject()` and casts it to RenderBox.
@JavaName('com.codename1.flutter.rendering.RenderObject')
class RenderObject {
  external bool get attached;
  external Rect get paintBounds;
  external Rect get semanticBounds;
  external void markNeedsPaint();
  external void markNeedsLayout();
}

// A render object laid out with the box protocol (a Cartesian size) — Flutter's
// `RenderBox`. The transformations and reply studies read `size` and map points
// through `localToGlobal` / `globalToLocal`.
@JavaName('com.codename1.flutter.rendering.RenderBox')
class RenderBox extends RenderObject {
  external Size get size;
  external bool get hasSize;
  external Offset localToGlobal(Offset point, {RenderObject? ancestor});
  external Offset globalToLocal(Offset point, {RenderObject? ancestor});
  external Object getTransformTo(RenderObject? ancestor);
}

// The canvas + child-painting handle handed to `RenderObject.paint` — Flutter's
// `PaintingContext`. The sliders demo's custom shapes read `context.canvas`.
@JavaName('com.codename1.flutter.rendering.PaintingContext')
class PaintingContext {
  external Canvas get canvas;
  external void paintChild(RenderObject child, Offset offset);
  external Rect get estimatedBounds;
}

// ======================================================================
// Gesture details
// ======================================================================

// The details of a tap-up event — Flutter's `TapUpDetails` (globalPosition is
// used by the transformations demo to hit-test the board).
@JavaName('com.codename1.flutter.gestures.TapUpDetails')
class TapUpDetails {
  external TapUpDetails({Offset? globalPosition, Offset? localPosition, Object? kind});
  external Offset get globalPosition;
  external Offset get localPosition;
}

// The details of a tap-down event — Flutter's `TapDownDetails`.
@JavaName('com.codename1.flutter.gestures.TapDownDetails')
class TapDownDetails {
  external TapDownDetails({Offset? globalPosition, Offset? localPosition, Object? kind});
  external Offset get globalPosition;
  external Offset get localPosition;
}

// The details at the start of a drag — Flutter's `DragStartDetails`.
@JavaName('com.codename1.flutter.gestures.DragStartDetails')
class DragStartDetails {
  external DragStartDetails({Offset? globalPosition, Offset? localPosition});
  external Offset get globalPosition;
  external Offset get localPosition;
}

// The incremental details of a drag — Flutter's `DragUpdateDetails`. The reply
// bottom-drawer reads `primaryDelta` to drive its AnimationController.
@JavaName('com.codename1.flutter.gestures.DragUpdateDetails')
class DragUpdateDetails {
  external DragUpdateDetails({Offset? globalPosition, Offset? localPosition, Offset? delta, double? primaryDelta});
  external Offset get delta;
  external double? get primaryDelta;
  external Offset get globalPosition;
  external Offset get localPosition;
}

// The details at the end of a drag, carrying the fling velocity — Flutter's
// `DragEndDetails`. Home splash + reply drawer read `velocity.pixelsPerSecond`.
@JavaName('com.codename1.flutter.gestures.DragEndDetails')
class DragEndDetails {
  external DragEndDetails({Velocity? velocity, double? primaryVelocity});
  external Velocity get velocity;
  external double? get primaryVelocity;
}

// A 2-D velocity in logical pixels per second — Flutter's `Velocity`.
@JavaName('com.codename1.flutter.gestures.Velocity')
class Velocity {
  external Velocity({Offset pixelsPerSecond});
  external static Velocity get zero;
  external Offset get pixelsPerSecond;
  external Velocity clampMagnitude(double minValue, double maxValue);
}

// The details at the start of a scale/pan gesture — Flutter's `ScaleStartDetails`
// (the transformations demo reads `focalPoint`).
@JavaName('com.codename1.flutter.gestures.ScaleStartDetails')
class ScaleStartDetails {
  external Offset get focalPoint;
  external Offset get localFocalPoint;
  external int get pointerCount;
}

// Signature for a drag-update callback — Flutter's `GestureDragUpdateCallback`
// (`void Function(DragUpdateDetails)`). A SAM the transpiler binds closures to;
// the reply bottom-drawer stores one and hands it to a GestureDetector.
@JavaName('com.codename1.flutter.gestures.GestureDragUpdateCallback')
class GestureDragUpdateCallback {}

// Signature for a drag-end callback — Flutter's `GestureDragEndCallback`
// (`void Function(DragEndDetails)`).
@JavaName('com.codename1.flutter.gestures.GestureDragEndCallback')
class GestureDragEndCallback {}

// ======================================================================
// Physics — Tolerance / Simulation family
// ======================================================================

// The error tolerances a simulation settles within — Flutter's `Tolerance`.
// The home carousel physics compares the fling velocity against
// `tolerance.velocity`.
@JavaName('com.codename1.flutter.physics.Tolerance')
class Tolerance {
  external Tolerance({double? distance, double? time, double? velocity});
  external static Tolerance get defaultTolerance;
  external double get distance;
  external double get time;
  external double get velocity;
}

// The base of a physics simulation over time — Flutter's `Simulation`.
@JavaName('com.codename1.flutter.physics.Simulation')
class Simulation {
  external double x(double time);
  external double dx(double time);
  external bool isDone(double time);
}

// A spring simulation used for scroll snapping — Flutter's
// `ScrollSpringSimulation`.
@JavaName('com.codename1.flutter.physics.ScrollSpringSimulation')
class ScrollSpringSimulation extends Simulation {
  external ScrollSpringSimulation(Object spring, double start, double end, double velocity, {Tolerance? tolerance});
}

// A friction simulation clamped to a scroll range — Flutter's
// `ClampingScrollSimulation`.
@JavaName('com.codename1.flutter.physics.ClampingScrollSimulation')
class ClampingScrollSimulation extends Simulation {
  external ClampingScrollSimulation({double position, double velocity, double? friction, Tolerance? tolerance});
}

// ======================================================================
// Scroll notifications
// ======================================================================

// The base class of notifications that bubble up the widget tree — Flutter's
// `Notification`. new_gallery's ToggleSplashNotification extends it and calls
// `dispatch(context)` to send itself up to an enclosing NotificationListener.
@JavaName('com.codename1.flutter.widgets.Notification')
class Notification {
  external Notification();
  external bool dispatch(BuildContext? target);
}

// A notification bubbled up as a scrollable scrolls — Flutter's
// `ScrollNotification`. The reply adaptive-nav reads `depth` / `direction`.
@JavaName('com.codename1.flutter.widgets.ScrollNotification')
class ScrollNotification {
  external ScrollMetrics get metrics;
  external int get depth;
  external BuildContext? get context;
  external ScrollDirection get direction;
  external bool dispatch(BuildContext? target);
}

// A notification fired when the user starts or stops dragging — Flutter's
// `UserScrollNotification`, carrying the new `direction`.
@JavaName('com.codename1.flutter.widgets.UserScrollNotification')
class UserScrollNotification extends ScrollNotification {
  external UserScrollNotification({BuildContext context, ScrollMetrics metrics, ScrollDirection direction});
}

// A notification fired as the scroll offset changes — Flutter's
// `ScrollUpdateNotification`.
@JavaName('com.codename1.flutter.widgets.ScrollUpdateNotification')
class ScrollUpdateNotification extends ScrollNotification {
  external double? get scrollDelta;
}

// ======================================================================
// InteractiveViewer + its transformation controller
// ======================================================================

// The 4x4-matrix controller shared with an InteractiveViewer — Flutter's
// `TransformationController` (a ValueNotifier<Matrix4>). The transformations
// demo animates `value` and maps viewport points with `toScene`.
@JavaName('com.codename1.flutter.widgets.TransformationController')
class TransformationController extends ValueNotifier<Object> {
  external TransformationController([Matrix4? value]);
  external Matrix4 get value;
  external set value(Matrix4 v);
  external Offset toScene(Offset viewportPoint);
}

// A pan/zoom viewport for its child — Flutter's `InteractiveViewer`.
@JavaName('com.codename1.flutter.widgets.InteractiveViewer')
class InteractiveViewer extends Widget {
  external InteractiveViewer(
      {Key? key, TransformationController? transformationController,
       EdgeInsets? boundaryMargin, double? minScale, double? maxScale,
       bool? constrained, bool? panEnabled, bool? scaleEnabled,
       double? scaleFactor, Object? onInteractionStart, Object? onInteractionUpdate,
       Object? onInteractionEnd, Object? clipBehavior, bool? alignPanAxis,
       Widget? child});
}

// ======================================================================
// PageView + PageController
// ======================================================================

// Controls the visible page of a PageView — Flutter's `PageController`. The home
// carousel reads `page` and `position.haveDimensions`.
@JavaName('com.codename1.flutter.widgets.PageController')
class PageController {
  external PageController({int? initialPage, bool? keepPage, double? viewportFraction});
  external double? get page;
  external int get initialPage;
  external ScrollPosition get position;
  external bool get hasClients;
  external Future<void> animateToPage(int page, {Duration duration, Curve curve});
  external void jumpToPage(int page);
  external Future<void> nextPage({Duration duration, Curve curve});
  external Future<void> previousPage({Duration duration, Curve curve});
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void dispose();
}

// A scrollable list of one-page-at-a-time children — Flutter's `PageView` (and
// its `.builder` / `.custom` named constructors).
@JavaName('com.codename1.flutter.widgets.PageView')
class PageView extends Widget {
  external PageView(
      {Key? key, PageController? controller, Object? scrollDirection, bool? reverse,
       Object? physics, bool? pageSnapping, Object? onPageChanged, List<Widget>? children,
       bool? allowImplicitScrolling, String? restorationId, Object? clipBehavior});
  external static PageView builder(
      {Key? key, PageController? controller, Object? scrollDirection, bool? reverse,
       Object? physics, bool? pageSnapping, Object? onPageChanged,
       Object? itemBuilder, int? itemCount, bool? allowImplicitScrolling,
       String? restorationId, Object? clipBehavior});
}

// ======================================================================
// Forms
// ======================================================================

// The State of a Form, driving validation/save across its fields — Flutter's
// `FormState`.
@JavaName('com.codename1.flutter.widgets.FormState')
class FormState {
  external bool validate();
  external void save();
  external void reset();
}

// The State of a single FormField — Flutter's `FormFieldState<T>`. The text-field
// demo reads/writes `value` and calls didChange/validate/save/reset.
@JavaName('com.codename1.flutter.widgets.FormFieldState')
class FormFieldState<T> {
  external T? get value;
  external bool get hasError;
  external bool get isValid;
  external String? get errorText;
  external void didChange(T? value);
  external bool validate();
  external void save();
  external void reset();
}

// Persists a form field's value on save — Flutter's `FormFieldSetter<T>` typedef
// (`void Function(T? newValue)`). A SAM the transpiler binds closures to.
@JavaName('com.codename1.flutter.widgets.FormFieldSetter')
class FormFieldSetter<T> {}

// Validates a form field's value — Flutter's `FormFieldValidator<T>` typedef
// (`String? Function(T? value)`). A SAM the transpiler binds closures to.
@JavaName('com.codename1.flutter.widgets.FormFieldValidator')
class FormFieldValidator<T> {}

// A grouping of form fields that validate/save together — Flutter's `Form`.
@JavaName('com.codename1.flutter.widgets.Form')
class Form extends Widget {
  external Form({Key? key, Widget? child, Object? onChanged, Object? onWillPop,
      Object? canPop, Object? onPopInvoked, Object? autovalidateMode});
  external static FormState? of(BuildContext context);
  external static FormState? maybeOf(BuildContext context);
}

// A Material text field wired to Form validation — Flutter's `TextFormField`.
@JavaName('com.codename1.flutter.material.TextFormField')
class TextFormField extends Widget {
  external TextFormField(
      {Key? key, TextEditingController? controller, String? initialValue,
       InputDecoration? decoration, TextInputType? keyboardType, TextStyle? style,
       bool? obscureText, bool? enabled, int? maxLines, int? minLines, int? maxLength,
       Object? validator, Object? onSaved, Object? onChanged, Object? onFieldSubmitted,
       Object? onEditingComplete, Object? focusNode, Object? textInputAction,
       Object? textCapitalization, Object? autovalidateMode, Object? inputFormatters,
       List<String>? autofillHints, Object? autofocus, Object? cursorColor});
}

// ======================================================================
// Structural / layout single-child widgets
// ======================================================================

// Scales and positions its child within itself — Flutter's `FittedBox`.
@JavaName('com.codename1.flutter.widgets.FittedBox')
class FittedBox extends Widget {
  external FittedBox({Key? key, BoxFit? fit, Object? alignment, Object? clipBehavior, Widget? child});
}

// Whether (and how) to include a child in the tree — Flutter's `Visibility`.
@JavaName('com.codename1.flutter.widgets.Visibility')
class Visibility extends Widget {
  external Visibility(
      {Key? key, Widget child, Widget? replacement, bool? visible,
       bool? maintainState, bool? maintainAnimation, bool? maintainSize,
       bool? maintainSemantics, bool? maintainInteractivity});
}

// Sizes its child to the child's intrinsic height — Flutter's `IntrinsicHeight`.
@JavaName('com.codename1.flutter.widgets.IntrinsicHeight')
class IntrinsicHeight extends Widget {
  external IntrinsicHeight({Key? key, Widget? child});
}

// Sizes its child to the child's intrinsic width — Flutter's `IntrinsicWidth`.
@JavaName('com.codename1.flutter.widgets.IntrinsicWidth')
class IntrinsicWidth extends Widget {
  external IntrinsicWidth({Key? key, double? stepWidth, double? stepHeight, Widget? child});
}

// Prevents its subtree from receiving pointer events — Flutter's `IgnorePointer`.
@JavaName('com.codename1.flutter.widgets.IgnorePointer')
class IgnorePointer extends Widget {
  external IgnorePointer({Key? key, bool? ignoring, bool? ignoringSemantics, Widget? child});
}

// Isolates its subtree onto its own layer for cheaper repaints — Flutter's
// `RepaintBoundary`.
@JavaName('com.codename1.flutter.widgets.RepaintBoundary')
class RepaintBoundary extends Widget {
  external RepaintBoundary({Key? key, Widget? child});
}

// Forces its child to a specific width/height aspect ratio — Flutter's
// `AspectRatio`.
@JavaName('com.codename1.flutter.widgets.AspectRatio')
class AspectRatio extends Widget {
  external AspectRatio({Key? key, double aspectRatio, Widget? child});
}

// A widget that reports a preferred size — Flutter's `PreferredSizeWidget`
// interface (an AppBar / adaptive app bar implements it).
@JavaName('com.codename1.flutter.widgets.PreferredSizeWidget')
class PreferredSizeWidget extends Widget {
  external Size get preferredSize;
}

// Adapts its child to a PreferredSizeWidget of the given size — Flutter's
// `PreferredSize`.
@JavaName('com.codename1.flutter.widgets.PreferredSize')
class PreferredSize extends Widget {
  external PreferredSize({Key? key, Size preferredSize, Widget child});
  external Size get preferredSize;
}

// Shows a single child of a stack by index — Flutter's `IndexedStack`.
@JavaName('com.codename1.flutter.widgets.IndexedStack')
class IndexedStack extends Widget {
  external IndexedStack({Key? key, Object? alignment, Object? textDirection,
      Object? sizing, int? index, List<Widget>? children});
}

// Lets its child overflow its own constraints — Flutter's `OverflowBox`.
@JavaName('com.codename1.flutter.widgets.OverflowBox')
class OverflowBox extends Widget {
  external OverflowBox({Key? key, Object? alignment, double? minWidth, double? maxWidth,
      double? minHeight, double? maxHeight, Widget? child});
}

// A widget that clips/elevates its child to an arbitrary shape — Flutter's
// `PhysicalShape`.
@JavaName('com.codename1.flutter.widgets.PhysicalShape')
class PhysicalShape extends Widget {
  external PhysicalShape({Key? key, Object clipper, Object? clipBehavior, double? elevation,
      Color? color, Color? shadowColor, Widget? child});
}

// ======================================================================
// Focus / input plumbing widgets
// ======================================================================

// A widget managing a FocusNode for its subtree — Flutter's `Focus`.
@JavaName('com.codename1.flutter.widgets.Focus')
class Focus extends Widget {
  external Focus({Key? key, FocusNode? focusNode, bool? autofocus, Object? onFocusChange,
      Object? onKey, Object? onKeyEvent, bool? canRequestFocus, bool? skipTraversal,
      bool? descendantsAreFocusable, bool? includeSemantics, String? debugLabel, Widget? child});
  external static FocusNode of(BuildContext context, {bool scopeOk});
}

// Excludes its subtree from focus traversal — Flutter's `ExcludeFocus`.
@JavaName('com.codename1.flutter.widgets.ExcludeFocus')
class ExcludeFocus extends Widget {
  external ExcludeFocus({Key? key, bool? excluding, Widget? child});
}

// A raw keyboard listener — Flutter's `KeyboardListener`.
@JavaName('com.codename1.flutter.widgets.KeyboardListener')
class KeyboardListener extends Widget {
  external KeyboardListener({Key? key, FocusNode focusNode, bool? autofocus,
      bool? includeSemantics, Object? onKeyEvent, Widget child});
}

// A low-level pointer-event listener — Flutter's `Listener`.
@JavaName('com.codename1.flutter.widgets.Listener')
class Listener extends Widget {
  external Listener({Key? key, Object? onPointerDown, Object? onPointerMove,
      Object? onPointerUp, Object? onPointerCancel, Object? onPointerHover,
      Object? onPointerSignal, Object? behavior, Widget? child});
}

// Listens for a Notification bubbling up from its subtree — Flutter's
// `NotificationListener<T>`.
@JavaName('com.codename1.flutter.widgets.NotificationListener')
class NotificationListener<T> extends Widget {
  external NotificationListener({Key? key, Object? onNotification, Widget? child});
}

// Intercepts the system back gesture — Flutter's `WillPopScope`.
@JavaName('com.codename1.flutter.widgets.WillPopScope')
class WillPopScope extends Widget {
  external WillPopScope({Key? key, Object onWillPop, Widget child});
}

// ======================================================================
// Focus traversal policies
// ======================================================================

// Traverses focus in widget (tree) order — Flutter's
// `WidgetOrderTraversalPolicy`.
@JavaName('com.codename1.flutter.widgets.WidgetOrderTraversalPolicy')
class WidgetOrderTraversalPolicy {
  external WidgetOrderTraversalPolicy({Object? secondary});
}

// Traverses focus in reading order for the ambient text direction — Flutter's
// `ReadingOrderTraversalPolicy`.
@JavaName('com.codename1.flutter.widgets.ReadingOrderTraversalPolicy')
class ReadingOrderTraversalPolicy {
  external ReadingOrderTraversalPolicy({Object? secondary});
}

// Traverses focus by explicit FocusTraversalOrder — Flutter's
// `OrderedTraversalPolicy`.
@JavaName('com.codename1.flutter.widgets.OrderedTraversalPolicy')
class OrderedTraversalPolicy {
  external OrderedTraversalPolicy({Object? secondary});
}

// ======================================================================
// Overlay
// ======================================================================

// One entry painted into an Overlay — Flutter's `OverlayEntry`. Feature-discovery
// rebuilds it via `markNeedsBuild()` and tears it down with `remove()`.
@JavaName('com.codename1.flutter.widgets.OverlayEntry')
class OverlayEntry {
  external OverlayEntry({WidgetBuilder builder, bool? opaque, bool? maintainState});
  external void markNeedsBuild();
  external void remove();
  external bool get mounted;
}

// The stack of OverlayEntries floating above the navigator — Flutter's
// `Overlay`.
@JavaName('com.codename1.flutter.widgets.Overlay')
class Overlay extends Widget {
  external Overlay({Key? key, List<OverlayEntry>? initialEntries, Object? clipBehavior});
  external static OverlayState of(BuildContext context, {bool rootOverlay, Object? debugRequiredFor});
  external static OverlayState? maybeOf(BuildContext context, {bool rootOverlay});
}

// The mutable State of an Overlay — Flutter's `OverlayState`.
@JavaName('com.codename1.flutter.widgets.OverlayState')
class OverlayState {
  external void insert(OverlayEntry entry, {OverlayEntry? below, OverlayEntry? above});
  external void insertAll(List<OverlayEntry> entries, {OverlayEntry? below, OverlayEntry? above});
}

// ======================================================================
// Material widgets (constructors)
// ======================================================================

// The Material bar docked at the bottom, optionally notched for a FAB —
// Flutter's `BottomAppBar`.
@JavaName('com.codename1.flutter.material.BottomAppBar')
class BottomAppBar extends Widget {
  external BottomAppBar({Key? key, Color? color, double? elevation, NotchedShape? shape,
      Object? clipBehavior, double? notchMargin, EdgeInsets? padding, double? height,
      Color? surfaceTintColor, Color? shadowColor, Widget? child});
}

// A labelled action button inside a SnackBar — Flutter's `SnackBarAction`.
@JavaName('com.codename1.flutter.material.SnackBarAction')
class SnackBarAction {
  external SnackBarAction({Key? key, String label, VoidCallback onPressed,
      Color? textColor, Color? disabledTextColor, Color? backgroundColor});
}

// A Material drawer header showing the signed-in account — Flutter's
// `UserAccountsDrawerHeader`.
@JavaName('com.codename1.flutter.material.UserAccountsDrawerHeader')
class UserAccountsDrawerHeader extends Widget {
  external UserAccountsDrawerHeader({Key? key, Object? decoration, EdgeInsets? margin,
      Widget? currentAccountPicture, List<Widget>? otherAccountsPictures,
      Widget? accountName, Widget? accountEmail, Object? onDetailsPressed,
      Color? arrowColor});
}

// A large flat button with fully custom shape/fill — Flutter's
// `RawMaterialButton`.
@JavaName('com.codename1.flutter.material.RawMaterialButton')
class RawMaterialButton extends Widget {
  external RawMaterialButton({Key? key, VoidCallback? onPressed, Object? onLongPress,
      Object? onHighlightChanged, TextStyle? textStyle, Color? fillColor,
      Color? focusColor, Color? hoverColor, Color? highlightColor, Color? splashColor,
      double? elevation, double? focusElevation, double? hoverElevation,
      double? highlightElevation, double? disabledElevation, EdgeInsets? padding,
      Object? visualDensity, Object? constraints, Object? shape, Object? clipBehavior,
      bool? autofocus, Object? materialTapTargetSize, Widget? child});
}

// A tap/hover ink reaction not necessarily filling its bounds — Flutter's
// `InkResponse`.
@JavaName('com.codename1.flutter.material.InkResponse')
class InkResponse extends Widget {
  external InkResponse({Key? key, VoidCallback? onTap, Object? onTapDown, Object? onTapUp,
      Object? onTapCancel, Object? onDoubleTap, Object? onLongPress, Object? onHighlightChanged,
      Object? onHover, bool? containedInkWell, Object? highlightShape, double? radius,
      Object? borderRadius, Object? customBorder, Color? focusColor, Color? hoverColor,
      Color? highlightColor, Color? splashColor, Object? splashFactory, bool? enableFeedback,
      bool? excludeFromSemantics, Object? mouseCursor, bool? canRequestFocus, Widget? child});
}

// The leading back button of an app bar — Flutter's `BackButton`.
@JavaName('com.codename1.flutter.material.BackButton')
class BackButton extends Widget {
  external BackButton({Key? key, Color? color, Object? onPressed, Object? style});
}

// A builder that rebuilds its own subtree via a local setState — Flutter's
// `StatefulBuilder`.
@JavaName('com.codename1.flutter.widgets.StatefulBuilder')
class StatefulBuilder extends Widget {
  external StatefulBuilder({Key? key, Object builder});
}

// Builds itself from the latest snapshot of a Future — Flutter's
// `FutureBuilder<T>`.
@JavaName('com.codename1.flutter.widgets.FutureBuilder')
class FutureBuilder<T> extends Widget {
  external FutureBuilder({Key? key, Object? future, T? initialData, Object builder});
}

// The connection state of an async computation feeding an AsyncSnapshot —
// Flutter's `ConnectionState`.
@JavaName('com.codename1.flutter.widgets.ConnectionState')
enum ConnectionState { none, waiting, active, done }

// An immutable snapshot of interaction with an async computation, handed to the
// FutureBuilder/StreamBuilder `builder` — Flutter's `AsyncSnapshot<T>`. The
// about page reads `snapshot.hasData` / `snapshot.data`.
@JavaName('com.codename1.flutter.widgets.AsyncSnapshot')
class AsyncSnapshot<T> {
  external ConnectionState get connectionState;
  external T? get data;
  external Object? get error;
  external Object? get stackTrace;
  external bool get hasData;
  external bool get hasError;
  external T get requireData;
}

// ======================================================================
// Date / time picker dialogs
// ======================================================================

// The Material date-picker dialog — Flutter's `DatePickerDialog`.
@JavaName('com.codename1.flutter.material.DatePickerDialog')
class DatePickerDialog extends Widget {
  external DatePickerDialog({Key? key, DateTime? initialDate, DateTime firstDate,
      DateTime lastDate, DateTime? currentDate, Object? initialEntryMode,
      Object? selectableDayPredicate, String? helpText, String? cancelText,
      String? confirmText, Object? initialCalendarMode, String? errorFormatText,
      String? errorInvalidText, String? fieldHintText, String? fieldLabelText,
      Object? keyboardType, Object? restorationId});
}

// The Material time-picker dialog — Flutter's `TimePickerDialog`.
@JavaName('com.codename1.flutter.material.TimePickerDialog')
class TimePickerDialog extends Widget {
  external TimePickerDialog({Key? key, TimeOfDay initialTime, Object? cancelText,
      Object? confirmText, Object? helpText, Object? errorInvalidText, Object? hourLabelText,
      Object? minuteLabelText, Object? initialEntryMode, Object? orientation, Object? onEntryModeChanged,
      Object? restorationId});
}

// The Material date-range-picker dialog — Flutter's `DateRangePickerDialog`.
@JavaName('com.codename1.flutter.material.DateRangePickerDialog')
class DateRangePickerDialog extends Widget {
  external DateRangePickerDialog({Key? key, DateTime firstDate, DateTime lastDate,
      Object? initialDateRange, DateTime? currentDate, Object? initialEntryMode,
      String? helpText, String? cancelText, String? confirmText, String? saveText,
      String? errorFormatText, String? errorInvalidText, String? errorInvalidRangeText,
      String? fieldStartHintText, String? fieldEndHintText, String? fieldStartLabelText,
      String? fieldEndLabelText, Object? keyboardType, Object? restorationId});
}

// A Key backed by an object's identity (===) — Flutter's `ObjectKey`.
@JavaName('com.codename1.flutter.ObjectKey')
class ObjectKey extends Key {
  external ObjectKey(Object? value);
}

// ======================================================================
// Restoration
// ======================================================================

// Establishes a restoration namespace for its subtree — Flutter's
// `RestorationScope`.
@JavaName('com.codename1.flutter.widgets.RestorationScope')
class RestorationScope extends Widget {
  external RestorationScope({Key? key, String? restorationId, Widget child});
  external static Object? of(BuildContext context);
}

// ======================================================================
// animations package
// ======================================================================

// Fades and scales its child in/out for modal reveals — the `animations`
// package's `FadeScaleTransition`.
@JavaName('com.codename1.flutter.animations.FadeScaleTransition')
class FadeScaleTransition extends Widget {
  external FadeScaleTransition({Key? key, Animation<double> animation, Widget? child});
}

// The page-transition builder for the shared-axis (X/Y/Z) motion pattern — the
// `animations` package's `SharedAxisPageTransitionsBuilder`. (SharedAxis-
// TransitionType itself already lives in gallery_p4_apitail.dart.)
@JavaName('com.codename1.flutter.animations.SharedAxisPageTransitionsBuilder')
class SharedAxisPageTransitionsBuilder extends PageTransitionsBuilder {
  external SharedAxisPageTransitionsBuilder({SharedAxisTransitionType transitionType,
      Color? fillColor});
}

// Whether an OpenContainer uses a fade or fade-through transition — the
// `animations` package's `ContainerTransitionType`.
@JavaName('com.codename1.flutter.animations.ContainerTransitionType')
enum ContainerTransitionType { fade, fadeThrough }

// Shows a modal route with an `animations`-package transition — the package's
// top-level `showModal`.
@JavaName('com.codename1.flutter.animations.Animations.showModal')
external Future<T> showModal<T>({BuildContext context, Object? configuration,
    bool? useRootNavigator, WidgetBuilder builder, Object? filter});

// ======================================================================
// Animation combinators
// ======================================================================

// Runs a parent animation in reverse (1 - value) — Flutter's `ReverseAnimation`.
@JavaName('com.codename1.flutter.animation.ReverseAnimation')
class ReverseAnimation extends Animation<double> {
  external ReverseAnimation(Animation<double> parent);
}

// ======================================================================
// Painting / borders / images
// ======================================================================

// A box border resolved against text direction (start/end) — Flutter's
// `BorderDirectional`.
@JavaName('com.codename1.flutter.painting.BorderDirectional')
class BorderDirectional {
  external BorderDirectional({Object? top, Object? bottom, Object? start, Object? end});
}

// An image from an asset at an exact device-pixel scale — Flutter's
// `ExactAssetImage`.
@JavaName('com.codename1.flutter.painting.ExactAssetImage')
class ExactAssetImage extends ImageProvider {
  external ExactAssetImage(String assetName, {double? scale, Object? bundle, String? package});
}

// ======================================================================
// Gestures — TapGestureRecognizer (used by RichText spans)
// ======================================================================

// Recognizes single taps, wired to link spans in the about page — Flutter's
// `TapGestureRecognizer`.
@JavaName('com.codename1.flutter.gestures.TapGestureRecognizer')
class TapGestureRecognizer {
  external TapGestureRecognizer({Object? debugOwner});
  external set onTap(VoidCallback? handler);
  external void dispose();
}

// ======================================================================
// Semantics
// ======================================================================

// A semantic node emitted by a CustomPainter — Flutter's
// `CustomPainterSemantics`.
@JavaName('com.codename1.flutter.semantics.CustomPainterSemantics')
class CustomPainterSemantics {
  external CustomPainterSemantics({Rect rect, Object properties, Object? transform,
      Object? tags, Key? key});
}

// Fires accessibility announcements / haptics — Flutter's `SemanticsService`.
@JavaName('com.codename1.flutter.semantics.SemanticsService')
abstract class SemanticsService {
  external static void announce(String message, TextDirection textDirection, {Object? assertiveness});
  external static void tooltip(String message);
}

// ======================================================================
// Enums / identifier constants
// ======================================================================

// The visual layer flavor of a Material — Flutter's `MaterialType`.
@JavaName('com.codename1.flutter.material.MaterialType')
enum MaterialType { canvas, card, circle, button, transparency }

// How a BottomNavigationBar lays its items out — Flutter's
// `BottomNavigationBarType`.
@JavaName('com.codename1.flutter.material.BottomNavigationBarType')
enum BottomNavigationBarType { fixed, shifting }

// When an InputDecoration floats its label — Flutter's `FloatingLabelBehavior`.
@JavaName('com.codename1.flutter.material.FloatingLabelBehavior')
enum FloatingLabelBehavior { never, auto, always }

// One of the standard, individually-keyed components a scaffold builds (the
// close/back/drawer/... buttons) — Flutter's `StandardComponentType`. Each value
// exposes a stable `key`.
@JavaName('com.codename1.flutter.material.StandardComponentType')
class StandardComponentType {
  external Key get key;
  external static StandardComponentType get backButton;
  external static StandardComponentType get closeButton;
  external static StandardComponentType get drawerButton;
  external static StandardComponentType get moreButton;
}

// The well-known content types offered to platform autofill — Flutter's
// `AutofillHints`.
@JavaName('com.codename1.flutter.services.AutofillHints')
abstract class AutofillHints {
  external static String get username;
  external static String get password;
  external static String get newUsername;
  external static String get newPassword;
  external static String get email;
  external static String get name;
  external static String get givenName;
  external static String get familyName;
  external static String get telephoneNumber;
  external static String get oneTimeCode;
  external static String get creditCardNumber;
  external static String get postalCode;
  external static String get streetAddressLine1;
}

// The glue binding the widget layer to the engine — Flutter's `WidgetsBinding`.
// new_gallery reaches the ambient brightness through
// `WidgetsBinding.instance.platformDispatcher.platformBrightness`; the dispatcher
// is left `dynamic` so that chain resolves without pulling in the engine types.
@JavaName('com.codename1.flutter.widgets.WidgetsBinding')
class WidgetsBinding {
  external static WidgetsBinding get instance;
  external dynamic get platformDispatcher;
  external Object get window;
  external void addPostFrameCallback(Object callback);
  external void addObserver(Object observer);
  external void removeObserver(Object observer);
}

// The default height of a BottomNavigationBar — Flutter's top-level const
// `kBottomNavigationBarHeight` (kToolbarHeight already lives in gallery_p4).
@JavaName('com.codename1.flutter.material.MaterialConstants.kBottomNavigationBarHeight')
double kBottomNavigationBarHeight = 56.0;

// ======================================================================
// Theme value type
// ======================================================================

// The theming applied to descendant bottom sheets — Flutter's
// `BottomSheetThemeData`; the reply bottom-drawer reads `backgroundColor`.
@JavaName('com.codename1.flutter.material.BottomSheetThemeData')
class BottomSheetThemeData {
  external BottomSheetThemeData({Color? backgroundColor, Color? surfaceTintColor,
      double? elevation, Color? modalBackgroundColor, Color? modalBarrierColor,
      double? modalElevation, Object? shape, Object? clipBehavior, Object? constraints});
  external Color? get backgroundColor;
  external Color? get modalBackgroundColor;
  external double? get elevation;
}

// ======================================================================
// provider — Selector
// ======================================================================

// Rebuilds only when a selected slice of a provided value changes — the
// `provider` package's `Selector<A, S>`.
@JavaName('com.codename1.flutter.provider.Selector')
class Selector<A, S> extends StatelessWidget {
  external Selector({Key? key, Object selector, Object builder, Object? shouldRebuild, Widget? child});
}

// ======================================================================
// flutter_localized_countries
// ======================================================================

// The localizations delegate contributing translated locale/country names — the
// `flutter_localized_countries` package's `LocaleNamesLocalizationsDelegate`.
@JavaName('com.codename1.flutter.l10n.LocaleNamesLocalizationsDelegate')
class LocaleNamesLocalizationsDelegate {
  external LocaleNamesLocalizationsDelegate();
  // The map of locale-code -> native display name — the
  // `flutter_localized_countries` package's static `nativeLocaleNames`.
  external static Map<String, String> get nativeLocaleNames;
}

// The translated display names for locales/countries — the
// `flutter_localized_countries` package's `LocaleNames`.
@JavaName('com.codename1.flutter.l10n.LocaleNames')
class LocaleNames {
  external static LocaleNames of(BuildContext context);
  external String? nameOf(String localeCode);
  external Map<String, String> get data;
}

// ======================================================================
// dart:ui / top-level helpers
// ======================================================================

// A raw triangle mesh handed to Canvas.drawVertices — dart:ui's `Vertices`.
@JavaName('com.codename1.flutter.Vertices')
class Vertices {
  external Vertices(VertexMode mode, List<Offset> positions, {List<Color>? colors,
      List<int>? indices, List<Offset>? textureCoordinates});
}

// How a Vertices mesh strings its points into triangles — dart:ui's `VertexMode`.
@JavaName('com.codename1.flutter.VertexMode')
enum VertexMode { triangles, triangleStrip, triangleFan }

// Linearly interpolates two nullable doubles — dart:ui's top-level `lerpDouble`.
@JavaName('com.codename1.flutter.MathUtil.lerpDouble')
external double? lerpDouble(Object? a, Object? b, double t);

// Resolves the best-matching supported locale for the device's preferences —
// Flutter's top-level `basicLocaleListResolution`.
@JavaName('com.codename1.flutter.widgets.WidgetsLocalizations.basicLocaleListResolution')
external Locale basicLocaleListResolution(List<Locale>? preferredLocales, Iterable<Locale> supportedLocales);

// Asserts a MediaQuery ancestor exists (debug builds) — Flutter's
// `debugCheckHasMediaQuery`.
@JavaName('com.codename1.flutter.widgets.Debug.debugCheckHasMediaQuery')
external bool debugCheckHasMediaQuery(BuildContext context);

// Case-insensitive ASCII string comparison — package:collection's
// `compareAsciiUpperCase`.
@JavaName('com.codename1.flutter.util.AsciiUtil.compareAsciiUpperCase')
external int compareAsciiUpperCase(String a, String b);

// A Future that is already complete and calls its listeners synchronously —
// Flutter foundation's `SynchronousFuture<T>`.
@JavaName('com.codename1.flutter.foundation.SynchronousFuture')
class SynchronousFuture<T> {
  external SynchronousFuture(T value);
  external Object then(Object onValue, {Object? onError});
  external Object whenComplete(Object action);
  external Object catchError(Object onError, {Object? test});
}
