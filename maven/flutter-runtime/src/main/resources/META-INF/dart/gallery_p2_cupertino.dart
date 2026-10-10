// Codename One Flutter runtime API stubs — cupertino category (new_gallery, Pass 2).
//
// Signature-only declarations for the Cupertino (iOS-style) widget subset the
// new_gallery demos use. Resolved by the Dart transpiler against these shapes;
// each maps to a hand-written Java runtime class via @JavaName. Most Cupertino
// widgets compose the existing material / core widgets in their Java build()
// (visually approximate this pass — correct API shape + real layout).
//
// Cross-category value types the demos also use (Border, BorderSide, Radius,
// TextInputType, TextInputAction, CustomScrollView / slivers, the Navigator
// widget, RouteSettings, DefaultTextStyle, FlutterLogo) are owned by other
// categories (painting / services / scrolling / navigation) and are NOT
// declared here.

// --- enums ------------------------------------------------------------

@JavaName('com.codename1.flutter.cupertino.CupertinoDatePickerMode')
enum CupertinoDatePickerMode { time, date, dateAndTime, monthYear }

@JavaName('com.codename1.flutter.cupertino.OverlayVisibilityMode')
enum OverlayVisibilityMode { never, editing, notEditing, always }

// --- color / icon / cursor constant holders ---------------------------

@JavaName('com.codename1.flutter.cupertino.CupertinoDynamicColor')
class CupertinoDynamicColor extends Color {
  external CupertinoDynamicColor(int value);
  external Color resolveFrom(BuildContext context);
}

@JavaName('com.codename1.flutter.cupertino.CupertinoColors')
abstract class CupertinoColors {
  external static CupertinoDynamicColor get systemBackground;
  external static CupertinoDynamicColor get label;
  external static CupertinoDynamicColor get inactiveGray;
  external static CupertinoDynamicColor get systemBlue;
  external static CupertinoDynamicColor get systemGrey;
  external static CupertinoDynamicColor get activeBlue;
  external static CupertinoDynamicColor get activeGreen;
  external static CupertinoDynamicColor get destructiveRed;
  external static CupertinoDynamicColor get white;
  external static CupertinoDynamicColor get black;
}

@JavaName('com.codename1.flutter.cupertino.CupertinoIcons')
abstract class CupertinoIcons {
  external static IconData get home;
  external static IconData get conversation_bubble;
  external static IconData get profile_circled;
  external static IconData get padlock_solid;
  external static IconData get search;
  external static IconData get settings;
  external static IconData get share;
  external static IconData get add;
  external static IconData get clear;
  external static IconData get back;
}

@JavaName('com.codename1.flutter.cupertino.MouseCursor')
abstract class MouseCursor {
  // A deferred cursor that lets the region behind it decide — Flutter's
  // `MouseCursor.defer` (a static const on MouseCursor).
  external static MouseCursor get defer;
}

@JavaName('com.codename1.flutter.cupertino.SystemMouseCursors')
abstract class SystemMouseCursors {
  external static MouseCursor get none;
  external static MouseCursor get basic;
  external static MouseCursor get click;
  external static MouseCursor get forbidden;
  external static MouseCursor get wait;
  external static MouseCursor get progress;
  external static MouseCursor get text;
  external static MouseCursor get grab;
  external static MouseCursor get grabbing;
  external static MouseCursor get move;
  external static MouseCursor get resizeUpDown;
  external static MouseCursor get resizeLeftRight;
  external static MouseCursor get resizeColumn;
  external static MouseCursor get resizeRow;
  external static MouseCursor get copy;
  external static MouseCursor get alias;
  external static MouseCursor get cell;
  external static MouseCursor get precise;
}

// --- theming ----------------------------------------------------------

@JavaName('com.codename1.flutter.cupertino.CupertinoTextThemeData')
class CupertinoTextThemeData {
  external TextStyle get textStyle;
  external TextStyle get actionTextStyle;
  external TextStyle get navTitleTextStyle;
  external TextStyle get navLargeTitleTextStyle;
  external TextStyle get tabLabelTextStyle;
  external TextStyle get pickerTextStyle;
}

@JavaName('com.codename1.flutter.cupertino.CupertinoThemeData')
class CupertinoThemeData {
  external CupertinoThemeData({Brightness? brightness, Color? primaryColor, Color? primaryContrastingColor, Color? scaffoldBackgroundColor, Color? barBackgroundColor, CupertinoTextThemeData? textTheme});
  external CupertinoTextThemeData get textTheme;
  external Brightness? get brightness;
  external Color get primaryColor;
  external Color get scaffoldBackgroundColor;
  external Color get barBackgroundColor;
  external CupertinoThemeData copyWith({Brightness? brightness, Color? primaryColor, Color? primaryContrastingColor, Color? scaffoldBackgroundColor, Color? barBackgroundColor, CupertinoTextThemeData? textTheme});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoTheme')
class CupertinoTheme extends Widget {
  external CupertinoTheme({Key? key, CupertinoThemeData data, Widget child});
  external static CupertinoThemeData of(BuildContext context);
}

// --- scaffolding / navigation chrome ----------------------------------

@JavaName('com.codename1.flutter.cupertino.CupertinoPageScaffold')
class CupertinoPageScaffold extends Widget {
  external CupertinoPageScaffold({Key? key, Widget? navigationBar, Color? backgroundColor, bool? resizeToAvoidBottomInset, Widget child});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoNavigationBar')
class CupertinoNavigationBar extends Widget {
  external CupertinoNavigationBar({Key? key, Widget? leading, bool? automaticallyImplyLeading, bool? automaticallyImplyMiddle, String? previousPageTitle, Widget? middle, Widget? trailing, Color? backgroundColor, Object? brightness, Object? padding, Object? border, Object? transitionBetweenRoutes});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoSliverNavigationBar')
class CupertinoSliverNavigationBar extends Widget {
  external CupertinoSliverNavigationBar({Key? key, Widget? largeTitle, Widget? leading, bool? automaticallyImplyLeading, bool? automaticallyImplyTitle, String? previousPageTitle, Widget? middle, Widget? trailing, Color? backgroundColor, Object? border, bool? stretch});
}

// --- controls ---------------------------------------------------------

@JavaName('com.codename1.flutter.cupertino.CupertinoButton')
class CupertinoButton extends Widget {
  external CupertinoButton({Key? key, VoidCallback? onPressed, Widget? child, Object? padding, Color? color, Color? disabledColor, double? minSize, double? pressedOpacity, Object? borderRadius, Object? alignment});
  external static CupertinoButton filled({Key? key, VoidCallback? onPressed, Widget? child});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoActivityIndicator')
class CupertinoActivityIndicator extends Widget {
  external CupertinoActivityIndicator({Key? key, Color? color, bool? animating, double? radius});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoSwitch')
class CupertinoSwitch extends Widget {
  external CupertinoSwitch({Key? key, bool value, BoolCallback? onChanged, Color? activeColor, Color? trackColor, Color? thumbColor});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoSlider')
class CupertinoSlider extends Widget {
  external CupertinoSlider({Key? key, double value, double? min, double? max, int? divisions, DoubleCallback? onChanged, Object? onChangeStart, Object? onChangeEnd, Color? activeColor, Color? thumbColor});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoScrollbar')
class CupertinoScrollbar extends Widget {
  external CupertinoScrollbar({Key? key, Object? controller, bool? thumbVisibility, double? thickness, double? thicknessWhileDragging, Object? radius, Object? radiusWhileDragging, Widget child});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoTextField')
class CupertinoTextField extends Widget {
  external CupertinoTextField({Key? key, Object? controller, Object? decoration, Object? padding, String? placeholder, Object? placeholderStyle, Widget? prefix, Object? prefixMode, Widget? suffix, Object? suffixMode, Object? clearButtonMode, Object? keyboardType, Object? textInputAction, bool? obscureText, bool? autocorrect, bool? enabled, String? restorationId, StringCallback? onChanged, StringCallback? onSubmitted});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoSearchTextField')
class CupertinoSearchTextField extends Widget {
  external CupertinoSearchTextField({Key? key, Object? controller, String? placeholder, Object? decoration, Object? padding, String? restorationId, StringCallback? onChanged, StringCallback? onSubmitted, VoidCallback? onSuffixTap});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoSegmentedControl')
class CupertinoSegmentedControl<T> extends Widget {
  external CupertinoSegmentedControl({Key? key, Object children, IntCallback? onValueChanged, Object? groupValue, Color? unselectedColor, Color? selectedColor, Color? borderColor, Color? pressedColor, Object? padding});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoSlidingSegmentedControl')
class CupertinoSlidingSegmentedControl<T> extends Widget {
  external CupertinoSlidingSegmentedControl({Key? key, Object children, IntCallback? onValueChanged, Object? groupValue, Color? thumbColor, Color? backgroundColor, Object? padding});
}

// --- dialogs / action sheets / context menus --------------------------

@JavaName('com.codename1.flutter.cupertino.CupertinoAlertDialog')
class CupertinoAlertDialog extends Widget {
  external CupertinoAlertDialog({Key? key, Widget? title, Widget? content, List<Widget>? actions, Object? scrollController, Object? actionScrollController});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoDialogAction')
class CupertinoDialogAction extends Widget {
  external CupertinoDialogAction({Key? key, VoidCallback? onPressed, bool? isDefaultAction, bool? isDestructiveAction, TextStyle? textStyle, Widget child});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoActionSheet')
class CupertinoActionSheet extends Widget {
  external CupertinoActionSheet({Key? key, Widget? title, Widget? message, List<Widget>? actions, Object? messageScrollController, Object? actionScrollController, Widget? cancelButton});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoActionSheetAction')
class CupertinoActionSheetAction extends Widget {
  external CupertinoActionSheetAction({Key? key, VoidCallback onPressed, bool? isDefaultAction, bool? isDestructiveAction, Widget child});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoContextMenu')
class CupertinoContextMenu extends Widget {
  external CupertinoContextMenu({Key? key, List<Widget> actions, Widget child, Object? previewBuilder});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoContextMenuAction')
class CupertinoContextMenuAction extends Widget {
  external CupertinoContextMenuAction({Key? key, VoidCallback? onPressed, bool? isDefaultAction, bool? isDestructiveAction, Widget? trailingIcon, Widget child});
}

// --- pickers ----------------------------------------------------------

@JavaName('com.codename1.flutter.cupertino.CupertinoPicker')
class CupertinoPicker extends Widget {
  external CupertinoPicker({Key? key, Color? backgroundColor, double itemExtent, double? diameterRatio, double? magnification, double? squeeze, bool? useMagnifier, Object? scrollController, IntCallback onSelectedItemChanged, List<Widget> children});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoDatePicker')
class CupertinoDatePicker extends Widget {
  external CupertinoDatePicker({Key? key, Color? backgroundColor, CupertinoDatePickerMode? mode, DateTime? initialDateTime, DateTime? minimumDate, DateTime? maximumDate, int? minimumYear, int? maximumYear, int? minuteInterval, bool? use24hFormat, DateTimeCallback onDateTimeChanged});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoTimerPicker')
class CupertinoTimerPicker extends Widget {
  external CupertinoTimerPicker({Key? key, Color? backgroundColor, Object? mode, Duration? initialTimerDuration, int? minuteInterval, int? secondInterval, DurationCallback onTimerDurationChanged});
}

// --- tabs -------------------------------------------------------------

@JavaName('com.codename1.flutter.cupertino.CupertinoTabBar')
class CupertinoTabBar extends Widget {
  external CupertinoTabBar({Key? key, List<BottomNavigationBarItem> items, IntCallback? onTap, int? currentIndex, Color? backgroundColor, Color? activeColor, Color? inactiveColor, double? iconSize, Object? border});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoTabView')
class CupertinoTabView extends Widget {
  external CupertinoTabView({Key? key, WidgetBuilder? builder, String? restorationScopeId, String? defaultTitle, Object? routes, Object? onGenerateRoute, Object? onUnknownRoute, Object? navigatorObservers});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoTabScaffold')
class CupertinoTabScaffold extends Widget {
  external CupertinoTabScaffold({Key? key, CupertinoTabBar tabBar, IndexedWidgetBuilder tabBuilder, Object? controller, Color? backgroundColor, bool? resizeToAvoidBottomInset, String? restorationId});
}

// --- routes -----------------------------------------------------------
// Minimal navigation Route base (no Route class existed in the stubs yet);
// the Cupertino routes below extend it so a demo function typed
// `Route<String>` can return a CupertinoDialogRoute<String>.

@JavaName('com.codename1.flutter.navigation.Route')
abstract class Route<T> {
  // The RouteSettings (name / arguments) this route was pushed with — Flutter's
  // `Route.settings`. new_gallery reads `route.settings.name` inside popUntil.
  external RouteSettings get settings;
  external bool get isCurrent;
  external bool get isFirst;
  external bool get isActive;
}

@JavaName('com.codename1.flutter.cupertino.CupertinoPageRoute')
class CupertinoPageRoute<T> extends Route<T> {
  external CupertinoPageRoute({WidgetBuilder builder, Object? settings, String? title, bool? maintainState, bool? fullscreenDialog});
  external Widget buildTransitions(BuildContext context, Animation<double> animation, Animation<double> secondaryAnimation, Widget child);
}

@JavaName('com.codename1.flutter.cupertino.CupertinoDialogRoute')
class CupertinoDialogRoute<T> extends Route<T> {
  external CupertinoDialogRoute({BuildContext context, WidgetBuilder builder, Object? settings, bool? barrierDismissible, Color? barrierColor, String? barrierLabel});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoModalPopupRoute')
class CupertinoModalPopupRoute<T> extends Route<T> {
  external CupertinoModalPopupRoute({WidgetBuilder builder, Object? settings, Color? barrierColor, bool? barrierDismissible, String? barrierLabel});
}

@JavaName('com.codename1.flutter.cupertino.CupertinoDialogs.showCupertinoDialog')
external Future<dynamic> showCupertinoDialog({BuildContext context, WidgetBuilder builder, bool? barrierDismissible, Color? barrierColor, String? barrierLabel, bool? useRootNavigator, Object? routeSettings});

@JavaName('com.codename1.flutter.cupertino.CupertinoDialogs.showCupertinoModalPopup')
external Future<dynamic> showCupertinoModalPopup({BuildContext context, WidgetBuilder builder, Color? barrierColor, bool? barrierDismissible, bool? useRootNavigator, Object? semanticsDismissible, Object? routeSettings});
