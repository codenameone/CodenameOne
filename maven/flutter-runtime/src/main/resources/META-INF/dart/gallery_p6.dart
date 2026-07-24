// Codename One Flutter runtime API stubs — "apiStaticTail" category (new_gallery, Pass 6).
//
// The final long tail of brand-new Flutter widget / value-type / enum / theme
// symbols new_gallery references that were still unresolved after Passes 1-5
// (diagnostics E0135 unresolved-constructor, E0129 unresolved-identifier,
// E0132 unresolved-member, E0136 unresolved-static). Static members and getters
// that hang off ALREADY-declared types (ThemeData.navigationRailTheme,
// MediaQuery.sizeOf/paddingOf/viewInsetsOf, BorderSide.lerp, BorderRadius.lerp,
// OutlineInputBorder.borderSide/borderRadius/gapPadding/lerpFrom/lerpTo,
// Directionality.of, MouseCursor.defer, ScaffoldMessengerState.hideCurrentSnackBar)
// were appended to their owning classes in their existing stub files. Only the
// brand-new types live here.
//
// Conventions (see gallery_p4_apitail.dart header):
//  - positional constructor params -> Java constructor arguments
//  - named constructor params       -> void setter methods of the same name
//  - named constructors (X.name)    -> `external static X name(...)` / factory
//  - top-level functions            -> `@JavaName('fqcn.method') external ...`
//  - callbacks / types owned elsewhere are declared loosely as `Object?`.

// ======================================================================
// NavigationRail (Material side-nav) — studies/reply + navigation_rail_demo
// ======================================================================

// A vertical Material navigation rail, the desktop/tablet counterpart of a
// BottomNavigationBar — Flutter's `NavigationRail`. `extendedAnimation` exposes
// the 0..1 animation driving the collapsed<->extended transition so descendants
// (labels, folder section) can react to it.
@JavaName('com.codename1.flutter.material.NavigationRail')
class NavigationRail extends Widget {
  external NavigationRail(
      {Key? key, Color? backgroundColor, bool? extended, Widget? leading,
       Widget? trailing, List<NavigationRailDestination> destinations,
       int selectedIndex, Object? onDestinationSelected, double? elevation,
       double? groupAlignment, NavigationRailLabelType? labelType,
       TextStyle? unselectedLabelTextStyle, TextStyle? selectedLabelTextStyle,
       IconThemeData? unselectedIconTheme, IconThemeData? selectedIconTheme,
       double? minWidth, double? minExtendedWidth, bool? useIndicator,
       Color? indicatorColor, Object? indicatorShape});
  // The 0..1 animation of the rail's extended state, read via the ambient
  // rail — Flutter's `NavigationRail.extendedAnimation(context)`.
  external static Animation<double> extendedAnimation(BuildContext context);
}

// A single selectable entry in a NavigationRail — Flutter's
// `NavigationRailDestination`.
@JavaName('com.codename1.flutter.material.NavigationRailDestination')
class NavigationRailDestination {
  external NavigationRailDestination(
      {Widget icon, Widget? selectedIcon, Widget label, EdgeInsetsGeometry? padding,
       bool? disabled, String? indicatorColorTooltip});
}

// How/whether a NavigationRail labels its destinations — Flutter's
// `NavigationRailLabelType`.
@JavaName('com.codename1.flutter.material.NavigationRailLabelType')
enum NavigationRailLabelType { none, selected, all }

// The theming values for descendant NavigationRails — Flutter's
// `NavigationRailThemeData`. Reached both as a constructed theme value and via
// `Theme.of(context).navigationRailTheme`.
@JavaName('com.codename1.flutter.material.NavigationRailThemeData')
class NavigationRailThemeData {
  external NavigationRailThemeData(
      {Color? backgroundColor, double? elevation, TextStyle? unselectedLabelTextStyle,
       TextStyle? selectedLabelTextStyle, IconThemeData? unselectedIconTheme,
       IconThemeData? selectedIconTheme, double? groupAlignment,
       NavigationRailLabelType? labelType, bool? useIndicator, Color? indicatorColor,
       Object? indicatorShape, double? minWidth, double? minExtendedWidth});
  external Color? get backgroundColor;
  external double? get elevation;
  external TextStyle? get unselectedLabelTextStyle;
  external TextStyle? get selectedLabelTextStyle;
  external IconThemeData? get unselectedIconTheme;
  external IconThemeData? get selectedIconTheme;
}

// ======================================================================
// FlutterLogo
// ======================================================================

// The animated Flutter logo — Flutter's `FlutterLogo`. Used by the Cupertino
// context-menu demo as a large decorative image.
@JavaName('com.codename1.flutter.widgets.FlutterLogo')
class FlutterLogo extends Widget {
  external FlutterLogo(
      {Key? key, double? size, Color? textColor, Object? style, Duration? duration,
       Curve? curve});
}

// ======================================================================
// License page (about screen)
// ======================================================================

// The Material page listing the open-source licenses of the app's packages —
// Flutter's `LicensePage`.
@JavaName('com.codename1.flutter.material.LicensePage')
class LicensePage extends Widget {
  external LicensePage(
      {Key? key, String? applicationName, String? applicationVersion,
       Widget? applicationIcon, String? applicationLegalese});
}

// Pushes a Material license page onto the navigator — Flutter's top-level
// `showLicensePage`.
@JavaName('com.codename1.flutter.material.LicensePage.show')
external void showLicensePage(
    {BuildContext context, String? applicationName, String? applicationVersion,
     Widget? applicationIcon, String? applicationLegalese, bool? useRootNavigator});

// ======================================================================
// InputDecorationThemeData (Material 3 renamed InputDecorationTheme)
// ======================================================================

// The theming values applied to descendant InputDecorators — Flutter's
// `InputDecorationThemeData` (the Material-3 value-type spelling of the older
// `InputDecorationTheme`). Declared loosely: the gallery only sets a handful of
// fields and never reads them back.
@JavaName('com.codename1.flutter.material.InputDecorationThemeData')
class InputDecorationThemeData {
  external InputDecorationThemeData(
      {TextStyle? labelStyle, TextStyle? floatingLabelStyle, TextStyle? helperStyle,
       TextStyle? hintStyle, TextStyle? errorStyle, TextStyle? prefixStyle,
       TextStyle? suffixStyle, TextStyle? counterStyle, bool? filled, Color? fillColor,
       Color? focusColor, Color? hoverColor, EdgeInsetsGeometry? contentPadding,
       bool? isDense, bool? isCollapsed, Object? border, Object? enabledBorder,
       Object? focusedBorder, Object? errorBorder, Object? focusedErrorBorder,
       Object? disabledBorder, Object? floatingLabelBehavior, double? gapPadding,
       bool? alignLabelWithHint, Object? constraints});
}

// ======================================================================
// Notched shapes (BottomAppBar FAB notch)
// ======================================================================

// The strategy that carves a notch out of a shape for a docked FAB — Flutter's
// `NotchedShape` interface.
@JavaName('com.codename1.flutter.material.NotchedShape')
abstract class NotchedShape {}

// A NotchedShape that cuts a circular notch with small flanking fillets —
// Flutter's `CircularNotchedRectangle`.
@JavaName('com.codename1.flutter.material.CircularNotchedRectangle')
class CircularNotchedRectangle extends NotchedShape {
  external CircularNotchedRectangle({double? inverted});
}

// ======================================================================
// Back button icon
// ======================================================================

// The platform-appropriate back-arrow glyph, decoupled from its button —
// Flutter's `BackButtonIcon`.
@JavaName('com.codename1.flutter.material.BackButtonIcon')
class BackButtonIcon extends Widget {
  external BackButtonIcon({Key? key});
}

// ======================================================================
// Sliver grid delegates
// ======================================================================

// Base type for a sliver-grid layout strategy — Flutter's `SliverGridDelegate`.
@JavaName('com.codename1.flutter.rendering.SliverGridDelegate')
abstract class SliverGridDelegate {}

// Lays a grid out with a fixed number of tiles across the cross axis —
// Flutter's `SliverGridDelegateWithFixedCrossAxisCount`.
@JavaName('com.codename1.flutter.rendering.SliverGridDelegateWithFixedCrossAxisCount')
class SliverGridDelegateWithFixedCrossAxisCount extends SliverGridDelegate {
  external SliverGridDelegateWithFixedCrossAxisCount(
      {int crossAxisCount, double? mainAxisSpacing, double? crossAxisSpacing,
       double? childAspectRatio, double? mainAxisExtent});
}

// Lays a grid out with tiles no wider than a maximum cross-axis extent —
// Flutter's `SliverGridDelegateWithMaxCrossAxisExtent`.
@JavaName('com.codename1.flutter.rendering.SliverGridDelegateWithMaxCrossAxisExtent')
class SliverGridDelegateWithMaxCrossAxisExtent extends SliverGridDelegate {
  external SliverGridDelegateWithMaxCrossAxisExtent(
      {double maxCrossAxisExtent, double? mainAxisSpacing, double? crossAxisSpacing,
       double? childAspectRatio, double? mainAxisExtent});
}

// ======================================================================
// animations package — fade-through transition
// ======================================================================

// Fades the outgoing child out then the incoming child in (Material shared-Z
// motion) — the `animations` package's `FadeThroughTransition`.
@JavaName('com.codename1.flutter.animations.FadeThroughTransition')
class FadeThroughTransition extends Widget {
  external FadeThroughTransition(
      {Key? key, Animation<double> animation, Animation<double> secondaryAnimation,
       Color? fillColor, Widget? child});
}

// ======================================================================
// Easing — Material 3 motion curves (package:flutter/animation)
// ======================================================================

// The Material 3 named easing curves — Flutter's `Easing`. Each is a static
// const Curve (`legacy` etc. are Cubic instances); the reply study reads
// `Easing.legacy` and `Easing.legacy.flipped`.
@JavaName('com.codename1.flutter.animation.Easing')
abstract class Easing {
  external static Curve get linear;
  external static Curve get legacy;
  external static Curve get legacyDecelerate;
  external static Curve get legacyAccelerate;
  external static Curve get standard;
  external static Curve get standardAccelerate;
  external static Curve get standardDecelerate;
  external static Curve get emphasized;
  external static Curve get emphasizedAccelerate;
  external static Curve get emphasizedDecelerate;
}

// ======================================================================
// Ink — a Material-aware decorated box
// ======================================================================

// Paints a decoration (or image) as part of the Material so ink splashes render
// above it — Flutter's `Ink` (and its `Ink.image` named constructor).
@JavaName('com.codename1.flutter.material.Ink')
class Ink extends Widget {
  external Ink(
      {Key? key, EdgeInsetsGeometry? padding, Color? color, Decoration? decoration,
       double? width, double? height, Widget? child});
  external static Ink image(
      {Key? key, ImageProvider image, BoxFit? fit, Widget? child, double? width,
       double? height, EdgeInsetsGeometry? padding, Object? colorFilter,
       Object? alignment, Object? repeat, Object? centerSlice, Object? onImageError});
}

// ======================================================================
// Scroll direction / autovalidate enums
// ======================================================================

// The user-scroll direction reported by a UserScrollNotification — Flutter's
// `ScrollDirection`.
@JavaName('com.codename1.flutter.rendering.ScrollDirection')
enum ScrollDirection { idle, forward, reverse }

// When a Form (or FormField) auto-validates its fields — Flutter's
// `AutovalidateMode`. Modelled as a class (not an enum) because the text-field
// demo reads `.index` off a value and indexes `.values` to round-trip the
// choice through a RestorableInt.
@JavaName('com.codename1.flutter.material.AutovalidateMode')
class AutovalidateMode {
  external int get index;
  external static AutovalidateMode get disabled;
  external static AutovalidateMode get always;
  external static AutovalidateMode get onUserInteraction;
  external static List<AutovalidateMode> get values;
}

// ======================================================================
// adaptive_breakpoints package — window size buckets
// ======================================================================

// The Material breakpoint bucket for the current window — the
// `adaptive_breakpoints` package's `AdaptiveWindowType`.
@JavaName('com.codename1.flutter.layout.AdaptiveWindowType')
enum AdaptiveWindowType { xsmall, small, medium, large, xlarge }

// Returns the AdaptiveWindowType bucket for the given context's window — the
// `adaptive_breakpoints` package's top-level `getWindowType`. Typed as the enum
// so the study's `>=` comparison lowers to an ordinal() compare in Java.
@JavaName('com.codename1.flutter.layout.AdaptiveBreakpoints.getWindowType')
external AdaptiveWindowType getWindowType(BuildContext context);
