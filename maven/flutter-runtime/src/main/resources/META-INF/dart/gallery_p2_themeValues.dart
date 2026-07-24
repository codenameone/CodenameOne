// Codename One Flutter runtime API stubs (M-P2: themeValues).
//
// Signature-only declarations for the Material "theme value" surface the real
// new_gallery app leans on: the MaterialState/WidgetState property machinery,
// VisualDensity, the component *ThemeData bundles, and ButtonStyle (the button
// styleFrom factories live on the button classes in flutter_material.dart).
//
// Only NEW symbols live here. The copyWith/withOpacity/apply cascade fixers on
// the pre-existing value types (Color, TextStyle, TextTheme, and the button
// classes) are added in-place in flutter_material.dart, because the stub
// registry keys a class by name and a second declaration would clobber the
// first. ThemeData/ColorScheme/TextTheme.copyWith already exist there.
//
// See flutter_material.dart for the stub conventions this file follows.

// --- MaterialState / WidgetState property machinery -------------------

@JavaName('com.codename1.flutter.material.MaterialState')
enum MaterialState { hovered, focused, pressed, dragged, selected, scrolledUnder, disabled, error }

// Material-3 rename of MaterialState (same members). Newer Flutter aliases the
// whole "MaterialStateX" family to "WidgetStateX"; both spellings appear in the
// wild, so both resolve.
@JavaName('com.codename1.flutter.material.WidgetState')
enum WidgetState { hovered, focused, pressed, dragged, selected, scrolledUnder, disabled, error }

// resolveWith takes `Color? Function(Set<MaterialState>)`. The transpiler needs
// a named typedef to bind an untyped-target lambda to a Funcs SAM, so the param
// type is the transpiler-internal MaterialPropertyResolver typedef
// (Set<MaterialState> -> Color); see JavaEmitter.TYPEDEFS.
@JavaName('com.codename1.flutter.material.MaterialStateProperty')
abstract class MaterialStateProperty {
  external static MaterialStateProperty all(dynamic value);
  external static MaterialStateProperty resolveWith(MaterialPropertyResolver resolver);
}

@JavaName('com.codename1.flutter.material.WidgetStateProperty')
abstract class WidgetStateProperty {
  external static WidgetStateProperty all(dynamic value);
  external static WidgetStateProperty resolveWith(MaterialPropertyResolver resolver);
}

@JavaName('com.codename1.flutter.material.VisualDensity')
class VisualDensity {
  external VisualDensity({double? horizontal, double? vertical});
  external static VisualDensity get adaptivePlatformDensity;
  external static VisualDensity get comfortable;
  external static VisualDensity get compact;
  external static VisualDensity get standard;
}

// --- component theme-data bundles -------------------------------------

@JavaName('com.codename1.flutter.material.SnackBarBehavior')
enum SnackBarBehavior { fixed, floating }

@JavaName('com.codename1.flutter.material.RadioThemeData')
class RadioThemeData {
  external RadioThemeData({dynamic fillColor, dynamic overlayColor, dynamic splashRadius,
      dynamic materialTapTargetSize, dynamic visualDensity, dynamic mouseCursor});
}

@JavaName('com.codename1.flutter.material.SwitchThemeData')
class SwitchThemeData {
  external SwitchThemeData({dynamic thumbColor, dynamic trackColor, dynamic trackOutlineColor,
      dynamic overlayColor, dynamic splashRadius, dynamic materialTapTargetSize,
      dynamic thumbIcon, dynamic mouseCursor});
}

@JavaName('com.codename1.flutter.material.SnackBarThemeData')
class SnackBarThemeData {
  external SnackBarThemeData({Color? backgroundColor, Color? actionTextColor,
      Color? disabledActionTextColor, TextStyle? contentTextStyle, double? elevation,
      dynamic shape, SnackBarBehavior? behavior, double? width, dynamic insetPadding,
      bool? showCloseIcon, Color? closeIconColor});
  external Color? get backgroundColor;
  external SnackBarBehavior? get behavior;
}

@JavaName('com.codename1.flutter.material.TabBarTheme')
class TabBarTheme {
  external TabBarTheme({Color? indicatorColor, Color? labelColor, Color? unselectedLabelColor,
      TextStyle? labelStyle, TextStyle? unselectedLabelStyle, dynamic indicator,
      dynamic indicatorSize, dynamic labelPadding, dynamic overlayColor, dynamic dividerColor});
}

@JavaName('com.codename1.flutter.material.TabBarThemeData')
class TabBarThemeData {
  external TabBarThemeData({Color? indicatorColor, Color? labelColor, Color? unselectedLabelColor,
      TextStyle? labelStyle, TextStyle? unselectedLabelStyle, dynamic indicator,
      dynamic indicatorSize, dynamic labelPadding, dynamic overlayColor, dynamic dividerColor});
}

@JavaName('com.codename1.flutter.material.DialogTheme')
class DialogTheme {
  external DialogTheme({Color? backgroundColor, double? elevation, Color? shadowColor,
      Color? surfaceTintColor, dynamic shape, dynamic alignment, TextStyle? titleTextStyle,
      TextStyle? contentTextStyle, dynamic iconColor});
}

@JavaName('com.codename1.flutter.material.DialogThemeData')
class DialogThemeData {
  external DialogThemeData({Color? backgroundColor, double? elevation, Color? shadowColor,
      Color? surfaceTintColor, dynamic shape, dynamic alignment, TextStyle? titleTextStyle,
      TextStyle? contentTextStyle, dynamic iconColor});
}

@JavaName('com.codename1.flutter.material.TooltipThemeData')
class TooltipThemeData {
  external TooltipThemeData({double? height, EdgeInsets? padding, EdgeInsets? margin,
      double? verticalOffset, bool? preferBelow, bool? excludeFromSemantics, dynamic decoration,
      TextStyle? textStyle, dynamic textAlign, Duration? waitDuration, Duration? showDuration,
      dynamic triggerMode, bool? enableFeedback});
}

@JavaName('com.codename1.flutter.material.FloatingActionButtonThemeData')
class FloatingActionButtonThemeData {
  external FloatingActionButtonThemeData({Color? foregroundColor, Color? backgroundColor,
      Color? focusColor, Color? hoverColor, Color? splashColor, double? elevation,
      double? focusElevation, double? hoverElevation, double? disabledElevation,
      double? highlightElevation, dynamic shape, bool? enableFeedback, double? iconSize,
      dynamic sizeConstraints, TextStyle? extendedTextStyle});
}

// --- ButtonStyle + styleFrom ------------------------------------------
// ButtonStyle is opaque: it is only ever produced by *.styleFrom (declared on
// the button classes in flutter_material.dart) and consumed by the buttons'
// `style:` parameter, so it needs no members here.

@JavaName('com.codename1.flutter.material.ButtonStyle')
class ButtonStyle {}

// Note: FilterChip / ChoiceChip / InputChip are owned by the widgetsMore
// category (gallery_p2_widgetsMore.dart), not this file, to avoid a duplicate
// stub declaration.
