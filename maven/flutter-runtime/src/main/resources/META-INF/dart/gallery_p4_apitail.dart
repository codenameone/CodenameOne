// Codename One Flutter runtime API stubs — "apiTail" category (new_gallery, Pass 4).
//
// The remaining long tail of Flutter widget / value-type / route / top-level
// symbols new_gallery references that were still unresolved after Passes 1-3
// (diagnostics E0135 unresolved-constructor, E0132 unresolved-member,
// E0129/E0136 unresolved-identifier). Each declaration is signature-only and
// maps, via @JavaName, to a hand-written Java runtime class or static member.
//
// Conventions (see flutter_material.dart / gallery_p3_widgetCtors.dart headers):
//  - positional constructor params    -> Java constructor arguments
//  - named constructor params         -> void setter methods of the same name
//  - named constructors (X.name)      -> `external static X name(...)`
//  - top-level functions              -> `@JavaName('fqcn.method') external ...`
//  - top-level consts / vars          -> `@JavaName('fqcn.field') <type> name = <literal>;`
//    (the literal is only a parse anchor; the emitted reference is the Java static)
//  - callbacks / types owned by other categories are declared loosely as `Object?`.

// ======================================================================
// Keys
// ======================================================================

// A key that is unique across the entire application — Flutter's `UniqueKey`.
// Never equal to any other key (identity equality), forcing a fresh element.
@JavaName('com.codename1.flutter.UniqueKey')
class UniqueKey extends Key {
  external UniqueKey();
}

// A ValueKey that additionally scrolls its subtree's PageStorage bucket.
// new_gallery tags each home carousel card with one so scroll offsets persist.
@JavaName('com.codename1.flutter.PageStorageKey')
class PageStorageKey<T> extends ValueKey {
  external PageStorageKey(T value);
}

// ======================================================================
// Image providers
// ======================================================================

// Decodes an image from an in-memory byte buffer — Flutter's `MemoryImage`.
@JavaName('com.codename1.flutter.MemoryImage')
class MemoryImage extends ImageProvider {
  external MemoryImage(Uint8List bytes, {double? scale});
}

// Wraps another ImageProvider and resizes the decoded image to the given
// dimensions — Flutter's `ResizeImage`.
@JavaName('com.codename1.flutter.ResizeImage')
class ResizeImage extends ImageProvider {
  external ResizeImage(ImageProvider imageProvider,
      {int? width, int? height, Object? policy, bool? allowUpscaling});
}

// ======================================================================
// Single-child layout widgets
// ======================================================================

// Applies a translation expressed as a fraction of its own size before
// painting its child — Flutter's `FractionalTranslation`.
@JavaName('com.codename1.flutter.widgets.FractionalTranslation')
class FractionalTranslation extends Widget {
  external FractionalTranslation(
      {Key? key, Offset translation, bool? transformHitTests, Widget? child});
}

// Rotates its child by an integral number of quarter turns — Flutter's
// `RotatedBox`. Unlike Transform.rotate this affects layout.
@JavaName('com.codename1.flutter.widgets.RotatedBox')
class RotatedBox extends Widget {
  external RotatedBox({Key? key, int quarterTurns, Widget? child});
}

// A directional Positioned for a Stack: `start`/`end` resolve against the
// ambient text direction — Flutter's `PositionedDirectional`.
@JavaName('com.codename1.flutter.widgets.PositionedDirectional')
class PositionedDirectional extends Widget {
  external PositionedDirectional(
      {Key? key, double? start, double? top, double? end, double? bottom,
       double? width, double? height, Widget? child});
}

// Lays out its children horizontally, overflowing to a vertical column when
// they do not fit — Flutter's `OverflowBar` (button-bar style).
@JavaName('com.codename1.flutter.widgets.OverflowBar')
class OverflowBar extends Widget {
  external OverflowBar(
      {Key? key, double? spacing, Object? alignment, double? overflowSpacing,
       Object? overflowAlignment, Object? overflowDirection, Object? textDirection,
       List<Widget> children});
}

// A single tile in a Material grid, with an optional header/footer band —
// Flutter's `GridTile`.
@JavaName('com.codename1.flutter.widgets.GridTile')
class GridTile extends Widget {
  external GridTile({Key? key, Widget? header, Widget? footer, Widget child});
}

// The header/footer band placed inside a GridTile — Flutter's `GridTileBar`.
@JavaName('com.codename1.flutter.widgets.GridTileBar')
class GridTileBar extends Widget {
  external GridTileBar(
      {Key? key, Color? backgroundColor, Widget? leading, Widget? title,
       Widget? subtitle, Widget? trailing});
}

// ======================================================================
// Focus traversal order
// ======================================================================

// Base type for an explicit focus-traversal ordering value.
@JavaName('com.codename1.flutter.widgets.FocusOrder')
abstract class FocusOrder {}

// Orders a focusable subtree by an ascending numeric value — Flutter's
// `NumericFocusOrder`.
@JavaName('com.codename1.flutter.widgets.NumericFocusOrder')
class NumericFocusOrder extends FocusOrder {
  external NumericFocusOrder(double order);
}

// Assigns an explicit traversal `order` to its child — Flutter's
// `FocusTraversalOrder`.
@JavaName('com.codename1.flutter.widgets.FocusTraversalOrder')
class FocusTraversalOrder extends Widget {
  external FocusTraversalOrder({Key? key, FocusOrder order, Widget child});
}

// ======================================================================
// Icon theming
// ======================================================================

// Establishes an ambient IconThemeData for its subtree — Flutter's `IconTheme`.
// (IconThemeData itself is contributed by the stateMgmt category.)
@JavaName('com.codename1.flutter.material.IconTheme')
class IconTheme extends Widget {
  external IconTheme({Key? key, IconThemeData data, Widget child});
  external static IconThemeData of(BuildContext context);
  external static IconTheme merge({Key? key, IconThemeData data, Widget child});
}

// ======================================================================
// Slider theming
// ======================================================================

// Establishes an ambient SliderThemeData for its subtree — Flutter's
// `SliderTheme`. (SliderThemeData itself is contributed by the cascadeTypes
// category.)
@JavaName('com.codename1.flutter.material.SliderTheme')
class SliderTheme extends Widget {
  external SliderTheme({Key? key, SliderThemeData data, Widget child});
  external static SliderThemeData of(BuildContext context);
}

// The active thumb of a RangeSlider — Flutter's `Thumb` enum.
@JavaName('com.codename1.flutter.material.Thumb')
enum Thumb { start, end }

// ======================================================================
// Toggle buttons
// ======================================================================

// A horizontal set of toggle buttons — Flutter's `ToggleButtons`. Styling
// params owned by other categories are declared loosely.
@JavaName('com.codename1.flutter.material.ToggleButtons')
class ToggleButtons extends Widget {
  external ToggleButtons(
      {Key? key, List<Widget> children, List<bool> isSelected, Object? onPressed,
       TextStyle? textStyle, Object? constraints, Color? color, Color? selectedColor,
       Color? disabledColor, Color? fillColor, Color? focusColor, Color? highlightColor,
       Color? hoverColor, Color? splashColor, bool? renderBorder, Color? borderColor,
       Color? selectedBorderColor, Color? disabledBorderColor, Object? borderRadius,
       double? borderWidth, Object? direction});
}

// ======================================================================
// Simple dialog
// ======================================================================

// A Material dialog presenting a title and a list of options — Flutter's
// `SimpleDialog`.
@JavaName('com.codename1.flutter.material.SimpleDialog')
class SimpleDialog extends Widget {
  external SimpleDialog(
      {Key? key, Widget? title, EdgeInsets? titlePadding, TextStyle? titleTextStyle,
       List<Widget>? children, EdgeInsets? contentPadding, Color? backgroundColor,
       double? elevation, Color? shadowColor, Color? surfaceTintColor,
       String? semanticLabel, EdgeInsets? insetPadding, Clip? clipBehavior,
       Object? shape, Object? alignment});
}

// A single tappable option inside a SimpleDialog — Flutter's `SimpleDialogOption`.
@JavaName('com.codename1.flutter.material.SimpleDialogOption')
class SimpleDialogOption extends Widget {
  external SimpleDialogOption(
      {Key? key, Object? onPressed, EdgeInsets? padding, Widget? child});
}

// ======================================================================
// Typography
// ======================================================================

// The set of text themes for a Material design language — Flutter's
// `Typography`. Constructed via the `material2018` / `material2014` factories.
@JavaName('com.codename1.flutter.material.Typography')
class Typography {
  external static Typography material2018(
      {Object? platform, TextTheme? black, TextTheme? white, TextTheme? englishLike,
       TextTheme? dense, TextTheme? tall});
  external static Typography material2014(
      {Object? platform, TextTheme? black, TextTheme? white, TextTheme? englishLike,
       TextTheme? dense, TextTheme? tall});
  external TextTheme? get black;
  external TextTheme? get white;
  external TextTheme? get englishLike;
  external TextTheme? get dense;
  external TextTheme? get tall;
}

// ======================================================================
// Routing
// ======================================================================

// A route whose transition is described by builder callbacks — Flutter's
// `PageRouteBuilder`. `pageBuilder` / `transitionsBuilder` receive the
// (context, animation, secondaryAnimation) triple.
@JavaName('com.codename1.flutter.navigation.PageRouteBuilder')
class PageRouteBuilder<T> extends Route<T> {
  external PageRouteBuilder(
      {RouteSettings? settings, Object pageBuilder, Object? transitionsBuilder,
       Duration? transitionDuration, Duration? reverseTransitionDuration,
       bool? opaque, bool? barrierDismissible, Color? barrierColor,
       String? barrierLabel, bool? maintainState, bool? fullscreenDialog});
}

// ======================================================================
// Diagnostics / errors
// ======================================================================

// Flutter's `FlutterError` — the error type the framework (and app assertions)
// throw. `FlutterError.reportError` routes a caught error to the current handler.
@JavaName('com.codename1.flutter.foundation.FlutterError')
class FlutterError {
  external FlutterError(String message);
  external static void reportError(Object details);
}

// ======================================================================
// animations package — shared-axis transition
// ======================================================================

// The direction of a shared-axis transition — the `animations` package's
// `SharedAxisTransitionType`.
@JavaName('com.codename1.flutter.animations.SharedAxisTransitionType')
enum SharedAxisTransitionType { horizontal, vertical, scaled }

// Cross-fades + slides two pages along a shared axis — the `animations`
// package's `SharedAxisTransition`.
@JavaName('com.codename1.flutter.animations.SharedAxisTransition')
class SharedAxisTransition extends Widget {
  external SharedAxisTransition(
      {Key? key, Animation<double> animation, Animation<double> secondaryAnimation,
       SharedAxisTransitionType transitionType, Color? fillColor, Widget? child});
}

// ======================================================================
// vector_math_64 — Matrix4
// ======================================================================

// A 4x4 column-major transform matrix — `package:vector_math_64`'s `Matrix4`.
// Used by Transform to build its `transform` argument.
@JavaName('com.codename1.flutter.vectormath.Matrix4')
class Matrix4 {
  external static Matrix4 identity();
  external static Matrix4 rotationX(double radians);
  external static Matrix4 rotationY(double radians);
  external static Matrix4 rotationZ(double radians);
  external static Matrix4 translationValues(double x, double y, double z);
  external static Matrix4 diagonal3Values(double x, double y, double z);
  external List<double> get storage;
  external Matrix4 clone();
  external void translate(double x, [double y, double z]);
  external void scale(double x, [double? y, double? z]);
  external void setEntry(int row, int col, double value);
  external void setRotationZ(double radians);
}

// ======================================================================
// scheduler binding
// ======================================================================

// The singleton driving frame scheduling — Flutter's `SchedulerBinding`.
// new_gallery registers post-frame callbacks through `SchedulerBinding.instance`.
@JavaName('com.codename1.flutter.scheduler.SchedulerBinding')
class SchedulerBinding {
  external static SchedulerBinding get instance;
  external void addPostFrameCallback(Object callback);
  external int scheduleFrameCallback(Object callback, {bool rescheduling});
  external void scheduleFrame();
}

// ======================================================================
// url_launcher package
// ======================================================================

@JavaName('com.codename1.flutter.services.UrlLauncher.launchUrl')
external Future<bool> launchUrl(Uri url, {Object? mode, Object? webOnlyWindowName});

@JavaName('com.codename1.flutter.services.UrlLauncher.canLaunchUrl')
external Future<bool> canLaunchUrl(Uri url);

@JavaName('com.codename1.flutter.services.UrlLauncher.launchUrlString')
external Future<bool> launchUrlString(String urlString, {Object? mode, Object? webOnlyWindowName});

@JavaName('com.codename1.flutter.services.UrlLauncher.canLaunchUrlString')
external Future<bool> canLaunchUrlString(String urlString);

// ======================================================================
// Top-level constants
// ======================================================================

// package:flutter/foundation — true only in a web build; always false here.
@JavaName('com.codename1.flutter.foundation.FoundationConstants.kIsWeb')
bool kIsWeb = false;

// package:flutter/material — default AppBar toolbar height (logical pixels).
@JavaName('com.codename1.flutter.material.MaterialConstants.kToolbarHeight')
double kToolbarHeight = 56.0;

// package:flutter/material — default margin around a FloatingActionButton.
@JavaName('com.codename1.flutter.material.MaterialConstants.kFloatingActionButtonMargin')
double kFloatingActionButtonMargin = 16.0;

// package:flutter/material — the duration Material widgets animate theme changes.
@JavaName('com.codename1.flutter.material.MaterialConstants.kThemeAnimationDuration')
Duration kThemeAnimationDuration = const Duration(milliseconds: 200);
