// Codename One Flutter runtime API stubs — cascadeTypes category (new_gallery, Pass 3).
//
// The unresolved RECEIVER value-types behind the E0132/E0137 "member/method on
// type X" cascade in new_gallery: text-editing value objects (TextEditingValue,
// TextSelection, TextRange), the scrolling model (ScrollController,
// ScrollPosition, ScrollMetrics), vector_math's Vector3, the listenable value
// holders (ValueNotifier / ValueListenable), ImageConfiguration, the slider
// theme (SliderThemeData + ShowValueIndicator), RouteSettings, TimeOfDay and its
// RestorableTimeOfDay wrapper.
//
// Signature-only declarations resolved by the Dart transpiler; each maps to a
// hand-written Java runtime class via @JavaName. The API shapes mirror the real
// Flutter / vector_math signatures (named-parameter names, getters, enum
// constants) so the resolver binds member accesses; faithful behaviour is
// layered in later. Loaded alongside flutter_material.dart by StubRegistry.
//
// Types referenced but owned elsewhere (Size, Color, TextStyle, Duration, Curve,
// BuildContext, RestorableProperty, DateTime) are intentionally NOT redeclared.

// --- text editing value objects -------------------------------------------

// A range of characters within a string. `composing` on TextEditingValue is a
// TextRange; a collapsed/empty range has start == end (or -1 when invalid).
@JavaName('com.codename1.flutter.TextRange')
class TextRange {
  external TextRange({int start, int end});
  external static TextRange collapsed(int offset);
  external static TextRange get empty;
  external int get start;
  external int get end;
  external bool get isValid;
  external bool get isCollapsed;
}

// A selection within editable text; the selection extends TextRange (base/extent
// plus the inherited start/end). new_gallery's phone-number formatter reads
// `selection.end` and builds `TextSelection.collapsed(offset: ...)`.
@JavaName('com.codename1.flutter.TextSelection')
class TextSelection extends TextRange {
  external TextSelection({int baseOffset, int extentOffset});
  external static TextSelection collapsed({int offset});
  external int get baseOffset;
  external int get extentOffset;
}

// The current text/selection/composing snapshot a TextInputFormatter transforms.
@JavaName('com.codename1.flutter.TextEditingValue')
class TextEditingValue {
  external TextEditingValue({String text, TextSelection? selection, TextRange? composing});
  external static TextEditingValue get empty;
  external String get text;
  external TextSelection get selection;
  external TextRange get composing;
  external TextEditingValue copyWith({String? text, TextSelection? selection, TextRange? composing});
}

// --- the scrolling model ---------------------------------------------------

// A read-only description of a scroll view's extents. ScrollPosition and the
// notifications' `.metrics` implement it; new_gallery reads pixels/maxScrollExtent
// to drive its ballistic carousel physics.
@JavaName('com.codename1.flutter.widgets.ScrollMetrics')
abstract class ScrollMetrics {
  // Dragged PAST an extent, as opposed to sitting exactly on it (atEdge).
  external bool get outOfRange;
  external double get pixels;
  external double get minScrollExtent;
  external double get maxScrollExtent;
  external double get viewportDimension;
  external double get extentBefore;
  external double get extentAfter;
  external double get extentInside;
  external bool get atEdge;
  external bool get hasContentDimensions;
  external bool get hasPixels;
  external bool get hasViewportDimension;
}

// The live scroll offset of a single scrollable, driven by a ScrollController.
@JavaName('com.codename1.flutter.widgets.ScrollPosition')
class ScrollPosition extends ScrollMetrics {
  external bool get haveDimensions;
  external Future<void> animateTo(double to, {Duration? duration, Curve? curve});
  external void jumpTo(double value);
}

// Controls one or more scrollables; the gallery carousel reads `offset` /
// `position.maxScrollExtent`, calls `animateTo`, and listens for changes.
@JavaName('com.codename1.flutter.widgets.ScrollController')
class ScrollController {
  external ScrollController({double? initialScrollOffset, bool? keepScrollOffset, String? debugLabel});
  external double get offset;
  external ScrollPosition get position;
  external bool get hasClients;
  external Future<void> animateTo(double offset, {Duration? duration, Curve? curve});
  external void jumpTo(double value);
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void dispose();
}

// --- vector_math -----------------------------------------------------------

// The 3-component double vector from package:vector_math; the transformations
// demo uses it for cube (hex-grid) coordinates and reads x / y / z.
@JavaName('com.codename1.flutter.vectormath.Vector3')
class Vector3 {
  external Vector3(double x, double y, double z);
  external static Vector3 zero();
  external static Vector3 all(double value);
  external double get x;
  external double get y;
  external double get z;
  external set x(double v);
  external set y(double v);
  external set z(double v);
}

// --- listenable value holders ----------------------------------------------

// An object exposing a value that changes over time and can be listened to.
@JavaName('com.codename1.flutter.foundation.ValueListenable')
abstract class ValueListenable<T> extends Listenable {
  external T get value;
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
}

// A ChangeNotifier holding a single value; assigning `value` notifies listeners.
// new_gallery drives ValueListenableBuilder<bool> from these (settings sheet,
// extended nav rail).
@JavaName('com.codename1.flutter.foundation.ValueNotifier')
class ValueNotifier<T> extends ValueListenable<T> {
  external ValueNotifier(T value);
  external T get value;
  external set value(T newValue);
  external void notifyListeners();
  external void dispose();
}

// --- painting --------------------------------------------------------------

// The context (size, bounds, device pixel ratio, ...) passed to a custom
// Decoration/BoxPainter's paint(); the tab-indicator and pie-chart painters read
// `configuration.size`.
@JavaName('com.codename1.flutter.ImageConfiguration')
class ImageConfiguration {
  external ImageConfiguration({Size? size, double? devicePixelRatio, TextDirection? textDirection, Locale? locale});
  external static ImageConfiguration get empty;
  external Size? get size;
  external double? get devicePixelRatio;
}

// --- slider theme ----------------------------------------------------------

// Whether a slider's value-indicator bubble shows.
@JavaName('com.codename1.flutter.material.ShowValueIndicator')
enum ShowValueIndicator { onlyForDiscrete, onlyForContinuous, always, never }

// The visual configuration of a Slider / RangeSlider. The sliders demo builds a
// custom theme via `theme.sliderTheme.copyWith(...)` and reads back thumbColor /
// disabledThumbColor / valueIndicatorColor. Shape parameters are typed Object?
// (their SliderComponentShape / RangeSliderThumbShape base types are owned by the
// widget-extension category).
@JavaName('com.codename1.flutter.material.SliderThemeData')
class SliderThemeData {
  external SliderThemeData({
      double? trackHeight, Color? activeTrackColor, Color? inactiveTrackColor,
      Color? disabledActiveTrackColor, Color? disabledInactiveTrackColor,
      Color? activeTickMarkColor, Color? inactiveTickMarkColor,
      Color? disabledActiveTickMarkColor, Color? disabledInactiveTickMarkColor,
      Color? thumbColor, Color? disabledThumbColor, Color? overlayColor,
      Color? valueIndicatorColor,
      Object? overlayShape, Object? tickMarkShape, Object? thumbShape,
      Object? trackShape, Object? valueIndicatorShape, Object? rangeThumbShape,
      Object? rangeTrackShape, Object? rangeTickMarkShape, Object? rangeValueIndicatorShape,
      ShowValueIndicator? showValueIndicator, TextStyle? valueIndicatorTextStyle});
  external double? get trackHeight;
  external Color? get activeTrackColor;
  external Color? get inactiveTrackColor;
  external Color? get activeTickMarkColor;
  external Color? get inactiveTickMarkColor;
  external Color? get thumbColor;
  external Color? get disabledThumbColor;
  external Color? get overlayColor;
  external Color? get valueIndicatorColor;
  external ShowValueIndicator? get showValueIndicator;
  external TextStyle? get valueIndicatorTextStyle;
  external SliderThemeData copyWith({
      double? trackHeight, Color? activeTrackColor, Color? inactiveTrackColor,
      Color? disabledActiveTrackColor, Color? disabledInactiveTrackColor,
      Color? activeTickMarkColor, Color? inactiveTickMarkColor,
      Color? disabledActiveTickMarkColor, Color? disabledInactiveTickMarkColor,
      Color? thumbColor, Color? disabledThumbColor, Color? overlayColor,
      Color? valueIndicatorColor,
      Object? overlayShape, Object? tickMarkShape, Object? thumbShape,
      Object? trackShape, Object? valueIndicatorShape, Object? rangeThumbShape,
      Object? rangeTrackShape, Object? rangeTickMarkShape, Object? rangeValueIndicatorShape,
      ShowValueIndicator? showValueIndicator, TextStyle? valueIndicatorTextStyle});
}

// --- routing ---------------------------------------------------------------

// The name/arguments a route was pushed with; new_gallery's onGenerateRoute reads
// `settings.name`.
@JavaName('com.codename1.flutter.navigation.RouteSettings')
class RouteSettings {
  external RouteSettings({String? name, Object? arguments});
  external String? get name;
  external Object? get arguments;
  external RouteSettings copyWith({String? name, Object? arguments});
}

// --- time of day -----------------------------------------------------------

// A wall-clock time (hour/minute, no date). The picker demo builds one from a
// DateTime, compares instances and formats via `format(context)`.
@JavaName('com.codename1.flutter.material.TimeOfDay')
class TimeOfDay {
  external TimeOfDay({int hour, int minute});
  external static TimeOfDay fromDateTime(Object time);
  external static TimeOfDay now();
  external int get hour;
  external int get minute;
  external TimeOfDay replacing({int? hour, int? minute});
  external String format(BuildContext context);
}

// A restorable TimeOfDay property (the picker demo's _fromTime); value get/set
// round-trips through the restoration framework (a no-op here).
@JavaName('com.codename1.flutter.RestorableTimeOfDay')
class RestorableTimeOfDay extends RestorableProperty<TimeOfDay> {
  external RestorableTimeOfDay(TimeOfDay defaultValue);
  external TimeOfDay get value;
  external set value(TimeOfDay v);
}

// The direction a scrollable's content grows in — named for where the content END lies.
@JavaName('com.codename1.flutter.AxisDirection')
enum AxisDirection { up, right, down, left }

// An immutable snapshot of a scrollable's extents. Unlike a live ScrollPosition this
// describes a moment rather than tracking one, which is what makes the physics testable:
// applyPhysicsToUserOffset is a pure function of the metrics handed to it.
@JavaName('com.codename1.flutter.widgets.FixedScrollMetrics')
class FixedScrollMetrics extends ScrollMetrics {
  external FixedScrollMetrics({
    double? minScrollExtent,
    double? maxScrollExtent,
    double? pixels,
    double? viewportDimension,
    AxisDirection? axisDirection,
    double? devicePixelRatio,
  });
}
