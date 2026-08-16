// Codename One Flutter runtime API stubs — widgetCtors category (new_gallery, Pass 3).
//
// Signature-only declarations resolved by the Dart transpiler; each maps to a
// hand-written Java runtime class via @JavaName. These are the ~20 distinct
// widget / value-type / route CONSTRUCTORS new_gallery instantiates that were
// still unresolved after Passes 1-2 (diagnostic E0135).
//
// Conventions (see flutter_material.dart header):
//  - positional constructor params -> Java constructor arguments
//  - named constructor params      -> void setter methods of the same name
//  - named constructors (X.name)   -> `external static X name(...)`; the named
//                                     params map to positional Java args in the
//                                     declared order (key, ...)
//  - callback SAM typedefs (VoidCallback / BoolCallback / DynamicCallback /
//    WidgetBuilder ...) map to dart.runtime.Funcs.* ; multi-arg or value-typed
//    callbacks are declared loosely as `Object?`.
//  - parameter types owned by other categories are declared `Object?`.

// ======================================================================
// Semantics sort key
// ======================================================================

@JavaName('com.codename1.flutter.semantics.OrdinalSortKey')
class OrdinalSortKey {
  external OrdinalSortKey(double order, {String? name});
}

// ======================================================================
// Inherited / focus / structural single-child widgets
// ======================================================================

@JavaName('com.codename1.flutter.widgets.DefaultTextStyle')
class DefaultTextStyle extends Widget {
  external DefaultTextStyle({Key? key, TextStyle? style, TextAlign? textAlign, bool? softWrap, Object? overflow, int? maxLines, Widget? child});
  external static DefaultTextStyle of(BuildContext context);
}

@JavaName('com.codename1.flutter.widgets.FocusTraversalGroup')
class FocusTraversalGroup extends Widget {
  external FocusTraversalGroup({Key? key, Object? policy, bool? descendantsAreFocusable, bool? descendantsAreTraversable, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Opacity')
class Opacity extends Widget {
  external Opacity({Key? key, double opacity, bool? alwaysIncludeSemantics, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Transform')
class Transform extends Widget {
  external Transform({Key? key, Object transform, Object? origin, Object? alignment, bool? transformHitTests, Object? filterQuality, Widget? child});
  external static Transform rotate({Key? key, double angle, Object? origin, Object? alignment, bool? transformHitTests, Object? filterQuality, Widget? child});
  external static Transform scale({Key? key, double? scale, double? scaleX, double? scaleY, Object? origin, Object? alignment, bool? transformHitTests, Object? filterQuality, Widget? child});
  external static Transform translate({Key? key, Object offset, bool? transformHitTests, Object? filterQuality, Widget? child});
}

// ======================================================================
// Image widgets
// ======================================================================

@JavaName('com.codename1.flutter.widgets.ImageIcon')
class ImageIcon extends Widget {
  external ImageIcon(ImageProvider? image, {Key? key, double? size, Color? color, String? semanticLabel});
}

@JavaName('com.codename1.flutter.widgets.FadeInImage')
class FadeInImage extends Widget {
  external FadeInImage({Key? key, ImageProvider placeholder, ImageProvider image, Duration? fadeOutDuration, Duration? fadeInDuration, double? width, double? height, Object? fit, Object? alignment, Object? repeat, Object? placeholderFit});
}

// ======================================================================
// Progress indicators
// ======================================================================

@JavaName('com.codename1.flutter.material.CircularProgressIndicator')
class CircularProgressIndicator extends Widget {
  external CircularProgressIndicator({Key? key, double? value, Color? backgroundColor, Color? color, Object? valueColor, double? strokeWidth, String? semanticsLabel, String? semanticsValue});
}

// ======================================================================
// Dividers
// ======================================================================

@JavaName('com.codename1.flutter.material.VerticalDivider')
class VerticalDivider extends Widget {
  external VerticalDivider({Key? key, double? width, double? thickness, double? indent, double? endIndent, Color? color});
}

// ======================================================================
// List tiles with an embedded control
// ======================================================================

@JavaName('com.codename1.flutter.material.RadioListTile')
class RadioListTile<T> extends Widget {
  external RadioListTile({Key? key, Object? value, Object? groupValue, DynamicCallback? onChanged, Widget? title, Widget? subtitle, Widget? secondary, bool? isThreeLine, bool? selected, bool? dense, Object? controlAffinity, Object? activeColor, Object? contentPadding});
}

@JavaName('com.codename1.flutter.material.SwitchListTile')
class SwitchListTile extends Widget {
  external SwitchListTile({Key? key, bool value, BoolCallback? onChanged, Widget? title, Widget? subtitle, Widget? secondary, bool? isThreeLine, bool? selected, bool? dense, Object? controlAffinity, Object? activeColor, Object? contentPadding});
}

// ======================================================================
// Checked popup menu item (extends the existing PopupMenuItem)
// ======================================================================

@JavaName('com.codename1.flutter.material.CheckedPopupMenuItem')
class CheckedPopupMenuItem<T> extends PopupMenuItem<T> {
  external CheckedPopupMenuItem({Key? key, T? value, bool? checked, bool? enabled, Widget? child, VoidCallback? onTap, Object? padding});
}

// ======================================================================
// Value-driven builder
// ======================================================================

@JavaName('com.codename1.flutter.widgets.ValueListenableBuilder')
class ValueListenableBuilder<T> extends Widget {
  external ValueListenableBuilder({Key? key, Object valueListenable, Object builder, Widget? child});
}

// ======================================================================
// Custom paint
// ======================================================================

@JavaName('com.codename1.flutter.rendering.CustomPainter')
abstract class CustomPainter {
  external CustomPainter({Object? repaint});
  void paint(Canvas canvas, Size size);
  bool shouldRepaint(CustomPainter oldDelegate);
}

@JavaName('com.codename1.flutter.widgets.CustomPaint')
class CustomPaint extends Widget {
  external CustomPaint({Key? key, CustomPainter? painter, CustomPainter? foregroundPainter, Size? size, bool? isComplex, bool? willChange, Widget? child});
}

@JavaName('com.codename1.flutter.rendering.TextPainter')
class TextPainter {
  external TextPainter({Object? text, TextDirection? textDirection, TextAlign? textAlign, double? textScaleFactor, int? maxLines, String? ellipsis, Object? textWidthBasis, Object? strutStyle, Object? locale});
  external void layout({double? minWidth, double? maxWidth});
  external void paint(Canvas canvas, Object offset);
  external Size get size;
  external double get width;
  external double get height;
}

// ======================================================================
// Scroll physics
// ======================================================================

// The parameters describing a spring's motion — Flutter's `SpringDescription`.
@JavaName('com.codename1.flutter.physics.SpringDescription')
class SpringDescription {
  external SpringDescription({double mass, double stiffness, double damping});
  external static SpringDescription withDampingRatio({double mass, double stiffness, double ratio});
}

@JavaName('com.codename1.flutter.widgets.ScrollPhysics')
class ScrollPhysics {
  external ScrollPhysics({ScrollPhysics? parent});
  // The drag pipeline: a raw finger delta becomes the offset actually applied to the
  // scroll position, which is where overscroll resistance lives.
  external double applyPhysicsToUserOffset(ScrollMetrics position, double offset);
  external bool shouldAcceptUserOffset(ScrollMetrics position);
  external double get minFlingVelocity;
  external double get maxFlingVelocity;
  external double get minFlingDistance;
  // Physics-subclass plumbing used by the home page's _SnappingScrollPhysics —
  // Flutter's `ScrollPhysics.applyTo/buildParent/toleranceFor/
  // createBallisticSimulation`. `spring` is the default spring an overriding
  // createBallisticSimulation feeds to a ScrollSpringSimulation.
  external ScrollPhysics applyTo(ScrollPhysics? ancestor);
  external ScrollPhysics? buildParent(ScrollPhysics? ancestor);
  external Tolerance toleranceFor(ScrollMetrics position);
  external Simulation? createBallisticSimulation(ScrollMetrics position, double velocity);
  external SpringDescription get spring;
}

// The base for the app-wide scroll configuration — Flutter's `ScrollBehavior`.
// `MaterialScrollBehavior` is the Material default; new_gallery's shrine app
// passes `const MaterialScrollBehavior().copyWith(scrollbars: false)`.
@JavaName('com.codename1.flutter.widgets.ScrollBehavior')
class ScrollBehavior {
  external ScrollBehavior();
  external ScrollBehavior copyWith({bool? scrollbars, bool? overscroll, Object? physics, Object? platform, Object? dragDevices});
}

@JavaName('com.codename1.flutter.material.MaterialScrollBehavior')
class MaterialScrollBehavior extends ScrollBehavior {
  external MaterialScrollBehavior();
}

@JavaName('com.codename1.flutter.widgets.NeverScrollableScrollPhysics')
class NeverScrollableScrollPhysics extends ScrollPhysics {
  external NeverScrollableScrollPhysics({ScrollPhysics? parent});
}

@JavaName('com.codename1.flutter.widgets.ClampingScrollPhysics')
class ClampingScrollPhysics extends ScrollPhysics {
  external ClampingScrollPhysics({ScrollPhysics? parent});
}

@JavaName('com.codename1.flutter.widgets.BouncingScrollPhysics')
class BouncingScrollPhysics extends ScrollPhysics {
  external BouncingScrollPhysics({ScrollPhysics? parent});
  // The rubber band, as a function of how far past the edge you already are.
  external double frictionFactor(double overscrollFraction);
}

@JavaName('com.codename1.flutter.widgets.AlwaysScrollableScrollPhysics')
class AlwaysScrollableScrollPhysics extends ScrollPhysics {
  external AlwaysScrollableScrollPhysics({ScrollPhysics? parent});
}

// ======================================================================
// Transitions (animation-driven single child)
// ======================================================================

@JavaName('com.codename1.flutter.animation.SizeTransition')
class SizeTransition extends Widget {
  external SizeTransition({Key? key, Object? axis, Animation<double> sizeFactor, double? axisAlignment, Widget? child});
}

@JavaName('com.codename1.flutter.animation.PageTransitionSwitcher')
class PageTransitionSwitcher extends Widget {
  external PageTransitionSwitcher({Key? key, Duration? duration, bool? reverse, Object transitionBuilder, Widget? child});
}

// ======================================================================
// animations package — OpenContainer
// ======================================================================

@JavaName('com.codename1.flutter.animations.OpenContainer')
class OpenContainer extends Widget {
  external OpenContainer({Key? key, Object? onClosed, Object closedBuilder, Object openBuilder, bool? tappable, Duration? transitionDuration, Object? transitionType, Color? closedColor, Color? openColor, Color? middleColor, double? closedElevation, double? openElevation, Object? closedShape, Object? openShape, String? routeSettings, bool? useRootNavigator});
}

// The `closedBuilder` signature of an OpenContainer — the animations package's
// `CloseContainerBuilder` (`Widget Function(BuildContext, VoidCallback)`). A SAM
// the transpiler binds closures to; new_gallery's _OpenContainerWrapper stores one.
@JavaName('com.codename1.flutter.animations.CloseContainerBuilder')
class CloseContainerBuilder {}

// ======================================================================
// navigation — DialogRoute
// ======================================================================

@JavaName('com.codename1.flutter.navigation.DialogRoute')
class DialogRoute<T> extends Route<T> {
  external DialogRoute({Key? key, BuildContext context, WidgetBuilder builder, Object? settings, Color? barrierColor, bool? barrierDismissible, String? barrierLabel, bool? useSafeArea, Object? themes, Object? anchorPoint, Object? traversalEdgeBehavior});
}
