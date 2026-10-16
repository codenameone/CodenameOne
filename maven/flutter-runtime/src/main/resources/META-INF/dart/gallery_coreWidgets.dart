// Codename One Flutter runtime API stubs — coreWidgets category (new_gallery).
//
// Structural / decoration / a11y / clipping widgets and their enums. These
// are signature-only declarations resolved by the Dart transpiler; each maps
// to a hand-written Java runtime class via @JavaName. Loosely-typed params
// (Object?) intentionally accept cross-category value types (BorderRadius,
// EdgeInsetsDirectional, gradients, cursors, ...) without this file having to
// declare them — the transpiler resolves those argument expressions against
// whatever category owns them.

// --- enums ------------------------------------------------------------

@JavaName('com.codename1.flutter.Axis')
enum Axis { horizontal, vertical }

@JavaName('com.codename1.flutter.TextDirection')
enum TextDirection { rtl, ltr }

@JavaName('com.codename1.flutter.Clip')
enum Clip { none, hardEdge, antiAlias, antiAliasWithSaveLayer }

@JavaName('com.codename1.flutter.BoxShape')
enum BoxShape { rectangle, circle }

@JavaName('com.codename1.flutter.StackFit')
enum StackFit { loose, expand, passthrough }

@JavaName('com.codename1.flutter.FlexFit')
enum FlexFit { tight, loose }

@JavaName('com.codename1.flutter.WrapAlignment')
enum WrapAlignment { start, end, center, spaceBetween, spaceAround, spaceEvenly }

@JavaName('com.codename1.flutter.WrapCrossAlignment')
enum WrapCrossAlignment { start, end, center }

// --- directional alignment -------------------------------------------

@JavaName('com.codename1.flutter.AlignmentDirectional')
abstract class AlignmentDirectional {
  external static AlignmentDirectional get topStart;
  external static AlignmentDirectional get topCenter;
  external static AlignmentDirectional get topEnd;
  external static AlignmentDirectional get centerStart;
  external static AlignmentDirectional get center;
  external static AlignmentDirectional get centerEnd;
  external static AlignmentDirectional get bottomStart;
  external static AlignmentDirectional get bottomCenter;
  external static AlignmentDirectional get bottomEnd;
}

// --- decorations / image providers -----------------------------------

@JavaName('com.codename1.flutter.Decoration')
abstract class Decoration {}

@JavaName('com.codename1.flutter.BoxDecoration')
class BoxDecoration extends Decoration {
  external BoxDecoration({Color? color, Object? image, Object? border, Object? borderRadius, Object? boxShadow, Object? gradient, Object? backgroundBlendMode, BoxShape? shape});
}

@JavaName('com.codename1.flutter.ImageProvider')
abstract class ImageProvider {}

@JavaName('com.codename1.flutter.AssetImage')
class AssetImage extends ImageProvider {
  external AssetImage(String assetName, {String? package, Object? bundle});
}

@JavaName('com.codename1.flutter.NetworkImage')
class NetworkImage extends ImageProvider {
  external NetworkImage(String url, {double? scale, Object? headers});
}

@JavaName('com.codename1.flutter.DecorationImage')
class DecorationImage {
  external DecorationImage({ImageProvider? image, BoxFit? fit, Object? alignment, Object? colorFilter, Object? repeat, double? scale, double? opacity, bool? matchTextDirection});
}

// --- core structural widgets -----------------------------------------

@JavaName('com.codename1.flutter.widgets.Container')
class Container extends Widget {
  external Container({Key? key, Object? alignment, Object? padding, Color? color, Object? decoration, Object? foregroundDecoration, double? width, double? height, BoxConstraints? constraints, Object? margin, Object? transform, Object? transformAlignment, Clip? clipBehavior, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.DecoratedBox')
class DecoratedBox extends Widget {
  external DecoratedBox({Key? key, Object decoration, Object? position, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.ColoredBox')
class ColoredBox extends Widget {
  external ColoredBox({Key? key, Color color, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.LayoutBuilder')
class LayoutBuilder extends Widget {
  external LayoutBuilder({Key? key, LayoutWidgetBuilder builder});
}

@JavaName('com.codename1.flutter.widgets.Builder')
class Builder extends Widget {
  external Builder({Key? key, WidgetBuilder builder});
}

@JavaName('com.codename1.flutter.widgets.SafeArea')
class SafeArea extends Widget {
  external SafeArea({Key? key, bool? left, bool? top, bool? right, bool? bottom, EdgeInsets? minimum, bool? maintainBottomViewPadding, Widget? child});
}

@JavaName('com.codename1.flutter.material.Material')
class Material extends Widget {
  external Material({Key? key, Object? type, double? elevation, Color? color, Color? shadowColor, Color? surfaceTintColor, TextStyle? textStyle, Object? borderRadius, Object? shape, bool? borderOnForeground, Clip? clipBehavior, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Flexible')
class Flexible extends Widget {
  external Flexible({Key? key, int? flex, FlexFit? fit, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Wrap')
class Wrap extends Widget {
  external Wrap({Key? key, Axis? direction, WrapAlignment? alignment, double? spacing, WrapAlignment? runAlignment, double? runSpacing, WrapCrossAlignment? crossAxisAlignment, Object? textDirection, Object? verticalDirection, Clip? clipBehavior, List<Widget> children});
}

@JavaName('com.codename1.flutter.widgets.FractionallySizedBox')
class FractionallySizedBox extends Widget {
  external FractionallySizedBox({Key? key, Object? alignment, double? widthFactor, double? heightFactor, Widget? child});
}

// --- accessibility (pass-through wrappers) ---------------------------

@JavaName('com.codename1.flutter.widgets.Semantics')
class Semantics extends Widget {
  external Semantics({Key? key, Widget? child, bool? container, bool? explicitChildNodes, bool? excludeSemantics, bool? enabled, bool? checked, bool? selected, bool? toggled, bool? button, bool? link, bool? header, bool? textField, bool? readOnly, bool? focusable, bool? focused, bool? image, bool? liveRegion, bool? hidden, bool? obscured, bool? multiline, String? label, String? value, String? increasedValue, String? decreasedValue, String? hint, String? tooltip, Object? sortKey, VoidCallback? onTap, VoidCallback? onLongPress});
  // Builds a Semantics node from a pre-assembled SemanticsProperties bag —
  // Flutter's `Semantics.fromProperties` (rally finance.dart).
  external static Semantics fromProperties({Key? key, required SemanticsProperties properties, bool? container, bool? explicitChildNodes, bool? excludeSemantics, Widget? child});
}

// A bag of semantic annotations passed to Semantics.fromProperties and to
// CustomPainterSemantics — Flutter's `SemanticsProperties`.
@JavaName('com.codename1.flutter.widgets.SemanticsProperties')
class SemanticsProperties {
  external SemanticsProperties({bool? enabled, bool? checked, bool? selected, bool? toggled, bool? button, bool? link, bool? header, bool? textField, bool? readOnly, bool? focusable, bool? focused, bool? inMutuallyExclusiveGroup, bool? hidden, bool? obscured, bool? multiline, bool? scopesRoute, bool? namesRoute, bool? image, bool? liveRegion, String? label, String? value, String? increasedValue, String? decreasedValue, String? hint, String? onTapHint, String? onLongPressHint, TextDirection? textDirection, Object? sortKey, VoidCallback? onTap, VoidCallback? onLongPress});
}

@JavaName('com.codename1.flutter.widgets.ExcludeSemantics')
class ExcludeSemantics extends Widget {
  external ExcludeSemantics({Key? key, bool? excluding, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.MergeSemantics')
class MergeSemantics extends Widget {
  external MergeSemantics({Key? key, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.AnnotatedRegion')
class AnnotatedRegion<T> extends Widget {
  external AnnotatedRegion({Key? key, Widget? child, Object? value, bool? sized});
}

@JavaName('com.codename1.flutter.widgets.MouseRegion')
class MouseRegion extends Widget {
  external MouseRegion({Key? key, Object? cursor, bool? opaque, Object? onEnter, Object? onExit, Object? onHover, Object? hitTestBehavior, Widget? child});
}

// --- clipping (pass-through wrappers) --------------------------------

@JavaName('com.codename1.flutter.widgets.ClipRRect')
class ClipRRect extends Widget {
  external ClipRRect({Key? key, Object? borderRadius, Object? clipper, Clip? clipBehavior, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.ClipRect')
class ClipRect extends Widget {
  external ClipRect({Key? key, Object? clipper, Clip? clipBehavior, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.ClipOval')
class ClipOval extends Widget {
  external ClipOval({Key? key, Object? clipper, Clip? clipBehavior, Widget? child});
}

// --- scrolling / overlays (pass-through) -----------------------------

@JavaName('com.codename1.flutter.widgets.Scrollbar')
class Scrollbar extends Widget {
  external Scrollbar({Key? key, Object? controller, bool? thumbVisibility, bool? trackVisibility, double? thickness, Object? radius, bool? interactive, Object? notificationPredicate, Object? scrollbarOrientation, Widget? child});
}

@JavaName('com.codename1.flutter.material.Tooltip')
class Tooltip extends Widget {
  external Tooltip({Key? key, String? message, Object? richMessage, double? height, Object? padding, Object? margin, double? verticalOffset, bool? preferBelow, bool? excludeFromSemantics, Object? decoration, TextStyle? textStyle, Object? waitDuration, Object? showDuration, Object? triggerMode, Widget? child});
}

// --- selectable text --------------------------------------------------

@JavaName('com.codename1.flutter.widgets.SelectableText')
class SelectableText extends Widget {
  external SelectableText(String data, {Key? key, TextStyle? style, TextAlign? textAlign, int? maxLines, double? textScaleFactor, bool? showCursor, Object? cursorColor, Object? onTap, Object? focusNode, Object? scrollPhysics});
  external static SelectableText rich(TextSpan textSpan, {Key? key, TextStyle? style, TextAlign? textAlign, int? maxLines, TextDirection? textDirection});
}

// --- popup menus ------------------------------------------------------

@JavaName('com.codename1.flutter.material.PopupMenuEntry')
abstract class PopupMenuEntry<T> extends Widget {}

@JavaName('com.codename1.flutter.material.PopupMenuItem')
class PopupMenuItem<T> extends PopupMenuEntry<T> {
  external PopupMenuItem({Key? key, T? value, bool? enabled, double? height, Object? padding, Object? textStyle, Object? mouseCursor, VoidCallback? onTap, Widget? child});
}

@JavaName('com.codename1.flutter.material.PopupMenuButton')
class PopupMenuButton<T> extends Widget {
  external PopupMenuButton({Key? key, PopupMenuItemBuilder itemBuilder, T? initialValue, DynamicCallback? onSelected, VoidCallback? onCanceled, String? tooltip, double? elevation, Object? padding, Widget? icon, double? iconSize, Object? offset, bool? enabled, Object? shape, Color? color, Object? position, Widget? child});
}
