// Codename One Flutter runtime API stubs -- forms, navigation bar, table and layout
// helpers composed from existing widgets (see gallery_p4_apitail.dart for the
// conventions: named params -> setters, named constructors -> static methods).

@JavaName('com.codename1.flutter.material.CheckboxListTile')
class CheckboxListTile extends Widget {
  external CheckboxListTile({Key? key, bool? value, BoolCallback? onChanged, Widget? title, Widget? subtitle,
      Widget? secondary, Object? controlAffinity, bool? tristate, bool? dense, bool? isThreeLine,
      Object? activeColor, Object? checkColor, Object? contentPadding, bool? enabled});
}

@JavaName('com.codename1.flutter.material.DropdownMenuItem')
class DropdownMenuItem<T> extends Widget {
  external DropdownMenuItem({Key? key, T? value, required Widget child, bool? enabled, VoidCallback? onTap, Object? alignment});
  external T? get value;
}

@JavaName('com.codename1.flutter.material.DropdownButton')
class DropdownButton<T> extends Widget {
  external DropdownButton({Key? key, List<DropdownMenuItem<T>>? items, T? value, DynamicCallback? onChanged,
      Widget? hint, Widget? disabledHint, Widget? icon, Widget? underline, bool? isExpanded, TextStyle? style,
      Color? dropdownColor, bool? isDense, int? elevation, double? iconSize, Object? borderRadius,
      double? menuMaxHeight, Object? alignment, Object? padding, Object? focusNode, bool? autofocus,
      Object? selectedItemBuilder});
}

@JavaName('com.codename1.flutter.material.DropdownButtonFormField')
class DropdownButtonFormField<T> extends Widget {
  external DropdownButtonFormField({Key? key, List<DropdownMenuItem<T>>? items, T? value, T? initialValue,
      DynamicCallback? onChanged, Widget? hint, Widget? disabledHint, Widget? icon, bool? isExpanded,
      TextStyle? style, Color? dropdownColor, InputDecoration? decoration, Object? validator, Object? onSaved,
      Object? autovalidateMode, bool? isDense, int? elevation, double? iconSize, Object? borderRadius,
      double? menuMaxHeight, Object? alignment, Object? padding, Object? focusNode, bool? autofocus,
      Object? selectedItemBuilder});
}

@JavaName('com.codename1.flutter.material.DropdownButtonHideUnderline')
class DropdownButtonHideUnderline extends Widget {
  external DropdownButtonHideUnderline({Key? key, required Widget child});
}

@JavaName('com.codename1.flutter.material.InputDecorator')
class InputDecorator extends Widget {
  external InputDecorator({Key? key, InputDecoration? decoration, Widget? child, bool? isEmpty, bool? isFocused,
      bool? isHovering, bool? expands, Object? baseStyle, Object? textAlign});
}

// The pre-Material-3 spelling of InputDecorationThemeData; the same Java value type.
@JavaName('com.codename1.flutter.material.InputDecorationThemeData')
class InputDecorationTheme extends InputDecorationThemeData {
  external InputDecorationTheme(
      {TextStyle? labelStyle, TextStyle? floatingLabelStyle, TextStyle? helperStyle,
       TextStyle? hintStyle, TextStyle? errorStyle, TextStyle? prefixStyle,
       TextStyle? suffixStyle, TextStyle? counterStyle, bool? filled, Color? fillColor,
       Object? border, Object? enabledBorder, Object? focusedBorder, Object? errorBorder,
       Object? contentPadding, bool? isDense});
}

@JavaName('com.codename1.flutter.material.NavigationDestination')
class NavigationDestination extends Widget {
  external NavigationDestination({Key? key, required Widget icon, Widget? selectedIcon, required String label,
      String? tooltip, bool? enabled});
}

@JavaName('com.codename1.flutter.material.NavigationBar')
class NavigationBar extends Widget {
  external NavigationBar({Key? key, required List<Widget> destinations, int? selectedIndex,
      IntCallback? onDestinationSelected, Color? backgroundColor, Color? indicatorColor, double? height,
      Object? labelBehavior, double? elevation, Object? surfaceTintColor, Object? shadowColor,
      Object? animationDuration, Object? indicatorShape});
}

@JavaName('com.codename1.flutter.material.NavigationBarThemeData')
class NavigationBarThemeData {
  external NavigationBarThemeData({Color? backgroundColor, Color? indicatorColor, double? height,
      Object? labelTextStyle, Object? labelBehavior, double? elevation, Object? iconTheme,
      Object? surfaceTintColor, Object? shadowColor, Object? indicatorShape});
}

enum NavigationDestinationLabelBehavior { alwaysShow, alwaysHide, onlyShowSelected }

@JavaName('com.codename1.flutter.material.FlexibleSpaceBar')
class FlexibleSpaceBar extends Widget {
  external FlexibleSpaceBar({Key? key, Widget? title, Widget? background, bool? centerTitle,
      Object? titlePadding, Object? collapseMode, Object? stretchModes, Object? expandedTitleScale});
}

@JavaName('com.codename1.flutter.material.ButtonSegment')
class ButtonSegment<T> {
  external ButtonSegment({required T value, Widget? label, Widget? icon, bool? enabled, String? tooltip});
}

@JavaName('com.codename1.flutter.material.SegmentedButton')
class SegmentedButton<T> extends Widget {
  external SegmentedButton({Key? key, required List<ButtonSegment<T>> segments, required Set<T> selected,
      DynamicCallback? onSelectionChanged, bool? multiSelectionEnabled, bool? emptySelectionAllowed,
      bool? showSelectedIcon, Object? style, Object? selectedIcon});
}

@JavaName('com.codename1.flutter.widgets.KeyedSubtree')
class KeyedSubtree extends Widget {
  external KeyedSubtree({Key? key, required Widget child});
  external static KeyedSubtree wrap(Widget child, int childIndex);
}

@JavaName('com.codename1.flutter.widgets.ListBody')
class ListBody extends Widget {
  external ListBody({Key? key, Axis? mainAxis, bool? reverse, List<Widget>? children});
}

@JavaName('com.codename1.flutter.widgets.AnimatedScale')
class AnimatedScale extends Widget {
  external AnimatedScale({Key? key, required double scale, required Duration duration, Widget? child,
      Object? alignment, Curve? curve, Object? onEnd, Object? filterQuality});
}

@JavaName('com.codename1.flutter.widgets.CrossFadeState')
enum CrossFadeState { showFirst, showSecond }

@JavaName('com.codename1.flutter.widgets.AnimatedCrossFade')
class AnimatedCrossFade extends Widget {
  external AnimatedCrossFade({Key? key, required Widget firstChild, required Widget secondChild,
      required CrossFadeState crossFadeState, required Duration duration, Duration? reverseDuration,
      Object? firstCurve, Object? secondCurve, Curve? sizeCurve, Object? alignment, Object? layoutBuilder,
      bool? excludeBottomFocus});
}

// dart:ui's ImageFilter; blur is the filter the runtime renders.
@JavaName('com.codename1.flutter.ImageFilter')
class ImageFilter {
  external static ImageFilter blur({double? sigmaX, double? sigmaY, TileMode? tileMode});
}

// Flutter's CustomClipper: subclass it and return the clip for the child's size.
@JavaName('com.codename1.flutter.rendering.CustomClipper')
abstract class CustomClipper<T> {
  external CustomClipper({Object? reclip});
  T getClip(Size size);
  bool shouldReclip(covariant CustomClipper<T> oldClipper);
}

@JavaName('com.codename1.flutter.widgets.ShaderMask')
class ShaderMask extends Widget {
  external ShaderMask({Key? key, required Shader Function(Rect bounds) shaderCallback, BlendMode? blendMode,
      Widget? child});
}

@JavaName('com.codename1.flutter.widgets.BackdropFilter')
class BackdropFilter extends Widget {
  external BackdropFilter({Key? key, required ImageFilter filter, Widget? child, BlendMode? blendMode,
      bool? enabled});
}

@JavaName('com.codename1.flutter.widgets.ImageFiltered')
class ImageFiltered extends Widget {
  external ImageFiltered({Key? key, required ImageFilter imageFilter, Widget? child, bool? enabled});
}

@JavaName('com.codename1.flutter.widgets.ClipPath')
class ClipPath extends Widget {
  external ClipPath({Key? key, CustomClipper<Path>? clipper, Object? clipBehavior, Widget? child});
}

// The Material picker dialogs: each completes with the choice, or null when cancelled.
@JavaName('com.codename1.flutter.material.DatePickerDialog.show')
external Future<DateTime?> showDatePicker({required BuildContext context, DateTime? initialDate,
    required DateTime firstDate, required DateTime lastDate, DateTime? currentDate, String? helpText,
    String? cancelText, String? confirmText});

@JavaName('com.codename1.flutter.material.TimePickerDialog.show')
external Future<TimeOfDay?> showTimePicker({required BuildContext context, required TimeOfDay initialTime,
    String? helpText, String? cancelText, String? confirmText});

@JavaName('com.codename1.flutter.material.DateRangePickerDialog.show')
external Future<DateTimeRange?> showDateRangePicker({required BuildContext context, required DateTime firstDate,
    required DateTime lastDate, DateTimeRange? initialDateRange, DateTime? currentDate, String? helpText,
    String? cancelText, String? saveText});

@JavaName('com.codename1.flutter.widgets.TableColumnWidth')
abstract class TableColumnWidth {}

@JavaName('com.codename1.flutter.widgets.FlexColumnWidth')
class FlexColumnWidth extends TableColumnWidth {
  external FlexColumnWidth([double value]);
}

@JavaName('com.codename1.flutter.widgets.FixedColumnWidth')
class FixedColumnWidth extends TableColumnWidth {
  external FixedColumnWidth(double value);
}

@JavaName('com.codename1.flutter.widgets.TableRow')
class TableRow {
  external TableRow({Object? key, Object? decoration, List<Widget>? children});
}

enum TableCellVerticalAlignment { top, middle, bottom, baseline, fill, intrinsicHeight }

@JavaName('com.codename1.flutter.widgets.TableCell')
class TableCell extends Widget {
  external TableCell({Key? key, TableCellVerticalAlignment? verticalAlignment, required Widget child});
}

@JavaName('com.codename1.flutter.widgets.TableBorder')
class TableBorder {
  external static TableBorder all({Color? color, double? width, Object? style, Object? borderRadius});
  external static TableBorder symmetric({Object? inside, Object? outside});
}

@JavaName('com.codename1.flutter.widgets.Table')
class Table extends Widget {
  external Table({Key? key, List<TableRow>? children, Map<int, TableColumnWidth>? columnWidths,
      TableColumnWidth? defaultColumnWidth, TableBorder? border,
      TableCellVerticalAlignment? defaultVerticalAlignment, Object? textDirection, Object? textBaseline});
}

// --- implicit animations ------------------------------------------------

@JavaName('com.codename1.flutter.widgets.AnimatedAlign')
class AnimatedAlign extends Widget {
  external AnimatedAlign({Key? key, required Object alignment, required Duration duration,
      Curve? curve, double? widthFactor, double? heightFactor, VoidCallback? onEnd, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.AnimatedPositioned')
class AnimatedPositioned extends Widget {
  external AnimatedPositioned({Key? key, required Duration duration, Curve? curve, double? left,
      double? top, double? right, double? bottom, double? width, double? height, VoidCallback? onEnd,
      required Widget child});
}

@JavaName('com.codename1.flutter.widgets.AnimatedRotation')
class AnimatedRotation extends Widget {
  external AnimatedRotation({Key? key, required double turns, required Duration duration, Curve? curve,
      Alignment? alignment, Object? filterQuality, VoidCallback? onEnd, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.AnimatedSlide')
class AnimatedSlide extends Widget {
  external AnimatedSlide({Key? key, required Offset offset, required Duration duration, Curve? curve,
      VoidCallback? onEnd, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.AnimatedDefaultTextStyle')
class AnimatedDefaultTextStyle extends Widget {
  external AnimatedDefaultTextStyle({Key? key, required TextStyle style, required Duration duration,
      Curve? curve, TextAlign? textAlign, bool? softWrap, TextOverflow? overflow, int? maxLines,
      Object? textWidthBasis, Object? textHeightBehavior, VoidCallback? onEnd, required Widget child});
}

@JavaName('com.codename1.flutter.widgets.TweenAnimationBuilder')
class TweenAnimationBuilder<T> extends Widget {
  external TweenAnimationBuilder({Key? key, required Tween<T> tween, required Duration duration,
      Curve? curve, required Widget Function(BuildContext context, T value, Widget? child) builder,
      VoidCallback? onEnd, Widget? child});
}
