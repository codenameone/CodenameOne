// Codename One Flutter runtime API stubs (M1).
//
// These signature-only declarations tell the Dart transpiler how the
// hand-written Java runtime (codenameone-flutter-runtime) looks from Dart:
// which classes exist, their Java names, and — crucially — parameter shapes.
//
// Conventions the emitter applies to stub classes:
//  - positional constructor parameters  -> Java constructor arguments
//  - named constructor parameters       -> void setter methods of the same name
//  - instance getters                   -> no-arg method calls (name())
//  - static getters                     -> static field access (Name.field)
//  - named parameters of methods        -> canonical positional order as declared
//  - the VoidCallback type              -> dart.runtime.Funcs.VoidFunc0
//
// M1 scope only. This file is parsed with the transpiler's own Dart parser.

// --- entry points -----------------------------------------------------

@JavaName('com.codename1.flutter.FlutterUI.runApp')
external void runApp(Widget app);

// --- framework core ---------------------------------------------------

@JavaName('com.codename1.flutter.Key')
abstract class Key {}

@JavaName('com.codename1.flutter.ValueKey')
class ValueKey extends Key {
  external ValueKey(Object value);
}

@JavaName('com.codename1.flutter.BuildContext')
abstract class BuildContext {
  // Return type `T` is a method type parameter: the transpiler recovers the
  // Dart <T> witness (dropped from the call) as a trailing Class<T> token and
  // casts the result, so the Java runtime methods take a trailing Class<T>.
  external T dependOnInheritedWidgetOfExactType();
  external T watch();
  external T read();
  // The render object of this element — Flutter's `BuildContext.findRenderObject`.
  external RenderObject? findRenderObject();
  // The nearest ancestor State of the given type (recovered via the trailing
  // Class<T> witness) — Flutter's `BuildContext.findAncestorStateOfType<T>`.
  external T findAncestorStateOfType();
  external Size? get size;
}

@JavaName('com.codename1.flutter.Widget')
abstract class Widget {
  external Widget({Key? key});
}

@JavaName('com.codename1.flutter.StatelessWidget')
abstract class StatelessWidget extends Widget {
  external StatelessWidget({Key? key});
  Widget build(BuildContext context);
}

@JavaName('com.codename1.flutter.StatefulWidget')
abstract class StatefulWidget extends Widget {
  external StatefulWidget({Key? key});
  State createState();
}

@JavaName('com.codename1.flutter.State')
abstract class State<T> {
  external T get widget;
  external BuildContext get context;
  external void setState(VoidCallback fn);
  external void initState();
  external void dispose();
  Widget build(BuildContext context);
}

// --- value types ------------------------------------------------------

@JavaName('com.codename1.flutter.Color')
class Color {
  external Color(int value);
  // The 32-bit ARGB integer this color was built from — Flutter's `Color.value`.
  external int get value;
  // Cascade fixers: member access on a Color must stay statically typed (P2).
  external Color withOpacity(double opacity);
  external Color withAlpha(int a);
  external Color copyWith({int? alpha, int? red, int? green, int? blue});
  external static Color alphaBlend(Color foreground, Color background);
  external static Color fromRGBO(int r, int g, int b, double opacity);
}

@JavaName('com.codename1.flutter.Colors')
abstract class Colors {
  external static Color get transparent;
  external static Color get red;
  external static Color get redAccent;
  external static Color get pink;
  external static Color get pinkAccent;
  external static Color get purple;
  external static Color get purpleAccent;
  external static Color get deepPurple;
  external static Color get deepPurpleAccent;
  external static Color get indigo;
  external static Color get indigoAccent;
  external static Color get blue;
  external static Color get blueAccent;
  external static Color get lightBlue;
  external static Color get lightBlueAccent;
  external static Color get cyan;
  external static Color get cyanAccent;
  external static Color get teal;
  external static Color get tealAccent;
  external static Color get green;
  external static Color get greenAccent;
  external static Color get lightGreen;
  external static Color get lightGreenAccent;
  external static Color get lime;
  external static Color get limeAccent;
  external static Color get yellow;
  external static Color get yellowAccent;
  external static Color get amber;
  external static Color get amberAccent;
  external static Color get orange;
  external static Color get orangeAccent;
  external static Color get deepOrange;
  external static Color get deepOrangeAccent;
  external static Color get brown;
  external static Color get grey;
  external static Color get blueGrey;
  external static Color get white;
  external static Color get white70;
  external static Color get white60;
  external static Color get white54;
  external static Color get white38;
  external static Color get white30;
  external static Color get white24;
  external static Color get white12;
  external static Color get white10;
  external static Color get black;
  external static Color get black87;
  external static Color get black54;
  external static Color get black45;
  external static Color get black38;
  external static Color get black26;
  external static Color get black12;
}

@JavaName('com.codename1.flutter.EdgeInsets')
class EdgeInsets extends EdgeInsetsGeometry {
  external static EdgeInsets get zero;
  external static EdgeInsets all(double value);
  external static EdgeInsets only({double left, double top, double right, double bottom});
  external static EdgeInsets symmetric({double horizontal, double vertical});
  external static EdgeInsets fromLTRB(double left, double top, double right, double bottom);
  // The four resolved edge insets — Flutter's `EdgeInsets.left/top/right/bottom`.
  external double get left;
  external double get top;
  external double get right;
  external double get bottom;
  // Summed horizontal (left+right) and vertical (top+bottom) insets.
  external double get horizontal;
  external double get vertical;
  external EdgeInsets copyWith({double? left, double? top, double? right, double? bottom});
  external EdgeInsets add(EdgeInsetsGeometry other);
}

@JavaName('com.codename1.flutter.MainAxisAlignment')
enum MainAxisAlignment { start, end, center, spaceBetween, spaceAround, spaceEvenly }

@JavaName('com.codename1.flutter.CrossAxisAlignment')
enum CrossAxisAlignment { start, end, center, stretch }

@JavaName('com.codename1.flutter.MainAxisSize')
enum MainAxisSize { min, max }

@JavaName('com.codename1.flutter.TextAlign')
enum TextAlign { left, right, center, start, end }

@JavaName('com.codename1.flutter.FontWeight')
abstract class FontWeight {
  external static FontWeight get w100;
  external static FontWeight get w200;
  external static FontWeight get w300;
  external static FontWeight get w400;
  external static FontWeight get w500;
  external static FontWeight get w600;
  external static FontWeight get w700;
  external static FontWeight get w800;
  external static FontWeight get w900;
  external static FontWeight get normal;
  external static FontWeight get bold;
}

@JavaName('com.codename1.flutter.TextStyle')
class TextStyle {
  external TextStyle({double? fontSize, FontWeight? fontWeight, Color? color, String? fontFamily, double? letterSpacing, double? height});
  external Color? get color;
  external double? get fontSize;
  external FontWeight? get fontWeight;
  external String? get fontFamily;
  external double? get letterSpacing;
  external double? get height;
  external TextStyle copyWith({bool? inherit, Color? color, Color? backgroundColor, String? fontFamily,
      double? fontSize, FontWeight? fontWeight, dynamic fontStyle, double? letterSpacing,
      double? wordSpacing, double? height, dynamic background, dynamic foreground, dynamic decoration});
  external TextStyle apply({Color? color, Color? backgroundColor, String? fontFamily,
      double? fontSizeFactor, double? fontSizeDelta, dynamic decoration});
}

@JavaName('com.codename1.flutter.IconData')
class IconData {}

@JavaName('com.codename1.flutter.Icons')
abstract class Icons {
  external static IconData get access_alarm;
  external static IconData get access_time;
  external static IconData get account_circle;
  external static IconData get add;
  external static IconData get add_circle;
  external static IconData get add_circle_outline;
  external static IconData get add_comment;
  external static IconData get add_shopping_cart;
  external static IconData get airplanemode_active;
  external static IconData get alarm_on;
  external static IconData get arrow_back;
  external static IconData get arrow_back_ios;
  external static IconData get arrow_drop_down;
  external static IconData get arrow_drop_up;
  external static IconData get arrow_forward;
  external static IconData get arrow_forward_ios;
  external static IconData get arrow_left;
  external static IconData get attach_money;
  external static IconData get book;
  external static IconData get bookmark_border;
  external static IconData get brightness_5;
  external static IconData get calendar_today;
  external static IconData get camera_enhance;
  external static IconData get check;
  external static IconData get check_circle;
  external static IconData get check_circle_outline;
  external static IconData get chevron_right;
  external static IconData get close;
  external static IconData get code;
  external static IconData get comment;
  external static IconData get create;
  external static IconData get credit_card;
  external static IconData get date_range;
  external static IconData get delete;
  external static IconData get directions_bike;
  external static IconData get edit;
  external static IconData get email;
  external static IconData get favorite;
  external static IconData get favorite_border;
  external static IconData get feedback;
  external static IconData get format_bold;
  external static IconData get format_italic;
  external static IconData get format_underline;
  external static IconData get fullscreen;
  external static IconData get help;
  external static IconData get home;
  external static IconData get hotel;
  external static IconData get info;
  external static IconData get info_outline;
  external static IconData get keyboard_arrow_down;
  external static IconData get keyboard_arrow_up;
  external static IconData get library_books;
  external static IconData get link;
  external static IconData get lock;
  external static IconData get menu;
  external static IconData get mic;
  external static IconData get money_off;
  external static IconData get more_vert;
  external static IconData get not_interested;
  external static IconData get notifications;
  external static IconData get person;
  external static IconData get person_add;
  external static IconData get phone;
  external static IconData get photo;
  external static IconData get photo_library;
  external static IconData get pie_chart;
  external static IconData get place;
  external static IconData get remove;
  external static IconData get remove_circle_outline;
  external static IconData get refresh;
  external static IconData get replay;
  external static IconData get reply_all;
  external static IconData get restaurant_menu;
  external static IconData get search;
  external static IconData get security;
  external static IconData get settings;
  external static IconData get share;
  external static IconData get shopping_cart;
  external static IconData get sort;
  external static IconData get star;
  external static IconData get star_border;
  external static IconData get table_chart;
  external static IconData get tune;
  external static IconData get vertical_split;
  external static IconData get visibility;
  external static IconData get visibility_off;
  external static IconData get web_asset;
}

// --- basic widgets ----------------------------------------------------

@JavaName('com.codename1.flutter.widgets.Text')
class Text extends Widget {
  external Text(String data, {Key? key, TextStyle? style, TextAlign? textAlign});
}

@JavaName('com.codename1.flutter.widgets.Icon')
class Icon extends Widget {
  external Icon(IconData icon, {Key? key, double? size, Color? color});
}

@JavaName('com.codename1.flutter.widgets.Column')
class Column extends Widget {
  external Column({Key? key, MainAxisAlignment? mainAxisAlignment, CrossAxisAlignment? crossAxisAlignment, MainAxisSize? mainAxisSize, List<Widget> children});
}

@JavaName('com.codename1.flutter.widgets.Row')
class Row extends Widget {
  external Row({Key? key, MainAxisAlignment? mainAxisAlignment, CrossAxisAlignment? crossAxisAlignment, MainAxisSize? mainAxisSize, List<Widget> children});
}

@JavaName('com.codename1.flutter.widgets.Center')
class Center extends Widget {
  external Center({Key? key, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Padding')
class Padding extends Widget {
  external Padding({Key? key, EdgeInsets padding, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.SizedBox')
class SizedBox extends Widget {
  external SizedBox({Key? key, double? width, double? height, Widget? child});
  external static SizedBox shrink({Key? key, Widget? child});
  external static SizedBox expand({Key? key, Widget? child});
  external static SizedBox fromSize({Key? key, Size? size, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Expanded')
class Expanded extends Widget {
  external Expanded({Key? key, int flex, Widget child});
}

// --- material ---------------------------------------------------------

@JavaName('com.codename1.flutter.material.MaterialApp')
class MaterialApp extends Widget {
  external MaterialApp({Key? key, String? title, ThemeData? theme, ThemeData? darkTheme, ThemeMode? themeMode, Widget? home, Route Function(RouteSettings)? onGenerateRoute});
}

@JavaName('com.codename1.flutter.material.Scaffold')
class Scaffold extends Widget {
  external Scaffold({Key? key, Widget? appBar, Widget? body, Widget? floatingActionButton, Widget? drawer, Widget? bottomNavigationBar});
  external static ScaffoldState of(BuildContext context);
}

@JavaName('com.codename1.flutter.material.AppBar')
class AppBar extends Widget {
  external AppBar({Key? key, Widget? title, Color? backgroundColor, bool? centerTitle});
  // The height this app bar prefers to occupy — Flutter's
  // `AppBar.preferredSize` (an app bar implements PreferredSizeWidget).
  external Size get preferredSize;
}

@JavaName('com.codename1.flutter.material.FloatingActionButton')
class FloatingActionButton extends Widget {
  external FloatingActionButton({Key? key, VoidCallback? onPressed, String? tooltip, Widget? child});
  external static FloatingActionButton extended({Key? key, VoidCallback? onPressed, Widget? label, Widget? icon, String? tooltip, Object? heroTag, Color? backgroundColor});
}

@JavaName('com.codename1.flutter.material.ThemeData')
class ThemeData {
  external ThemeData({ColorScheme? colorScheme, Color? colorSchemeSeed, bool? useMaterial3, Brightness? brightness,
      TextTheme? textTheme, TextTheme? primaryTextTheme, Color? primaryColor,
      Color? scaffoldBackgroundColor, Color? canvasColor, Color? cardColor, Color? dividerColor,
      Color? focusColor, Color? highlightColor, Color? splashColor, Color? hintColor,
      Color? disabledColor, Color? shadowColor, Color? indicatorColor, Color? secondaryHeaderColor,
      IconThemeData? iconTheme, IconThemeData? primaryIconTheme, AppBarTheme? appBarTheme,
      ChipThemeData? chipTheme, CheckboxThemeData? checkboxTheme, CardTheme? cardTheme,
      BottomAppBarThemeData? bottomAppBarTheme, DividerThemeData? dividerTheme,
      dynamic snackBarTheme, dynamic inputDecorationTheme, dynamic radioTheme, dynamic switchTheme,
      dynamic tooltipTheme, dynamic bottomSheetTheme, dynamic floatingActionButtonTheme,
      dynamic elevatedButtonTheme, dynamic textButtonTheme, dynamic outlinedButtonTheme,
      dynamic pageTransitionsTheme, dynamic visualDensity, dynamic typography, dynamic platform,
      NavigationRailThemeData? navigationRailTheme,
      bool? applyElevationOverlayColor, String? fontFamily});
  external static ThemeData dark({bool? useMaterial3});
  external ColorScheme get colorScheme;
  external TextTheme get textTheme;
  external TextTheme get primaryTextTheme;
  external Brightness get brightness;
  external IconThemeData? get iconTheme;
  external AppBarTheme? get appBarTheme;
  external ChipThemeData? get chipTheme;
  external CardTheme? get cardTheme;
  external DividerThemeData? get dividerTheme;
  external Color? get primaryColor;
  external Color? get scaffoldBackgroundColor;
  external Color? get canvasColor;
  external Color? get cardColor;
  external Color? get dividerColor;
  external Color? get focusColor;
  external Color? get highlightColor;
  external Color? get splashColor;
  external Color? get hintColor;
  external Color? get disabledColor;
  external dynamic get platform;
  // The ambient NavigationRail theme — Flutter's `ThemeData.navigationRailTheme`.
  external NavigationRailThemeData get navigationRailTheme;
  // The ambient Slider / BottomSheet themes — Flutter's `ThemeData.sliderTheme`
  // / `ThemeData.bottomSheetTheme`.
  external SliderThemeData get sliderTheme;
  external BottomSheetThemeData get bottomSheetTheme;
  external ThemeData copyWith({ColorScheme? colorScheme, TextTheme? textTheme, TextTheme? primaryTextTheme,
      Brightness? brightness, Color? primaryColor, Color? scaffoldBackgroundColor, Color? canvasColor,
      Color? cardColor, Color? dividerColor, Color? focusColor, Color? highlightColor, Color? splashColor,
      Color? hintColor, Color? disabledColor, IconThemeData? iconTheme, AppBarTheme? appBarTheme,
      ChipThemeData? chipTheme, CardTheme? cardTheme, DividerThemeData? dividerTheme, dynamic platform,
      NavigationRailThemeData? navigationRailTheme,
      bool? applyElevationOverlayColor, BottomAppBarThemeData? bottomAppBarTheme, BottomSheetThemeData? bottomSheetTheme, Object? inputDecorationTheme, IconThemeData? primaryIconTheme, PageTransitionsTheme? pageTransitionsTheme, Color? indicatorColor, TextSelectionThemeData? textSelectionTheme, Object? tabBarTheme, Object? snackBarTheme, Object? tooltipTheme});
}

@JavaName('com.codename1.flutter.material.ColorScheme')
class ColorScheme {
  external ColorScheme({Brightness? brightness, Color? primary, Color? onPrimary,
      Color? primaryContainer, Color? onPrimaryContainer, Color? inversePrimary, Color? secondary,
      Color? onSecondary, Color? secondaryContainer, Color? onSecondaryContainer, Color? tertiary,
      Color? onTertiary, Color? tertiaryContainer, Color? onTertiaryContainer, Color? error,
      Color? onError, Color? errorContainer, Color? onErrorContainer, Color? surface, Color? onSurface,
      Color? surfaceVariant, Color? onSurfaceVariant, Color? background, Color? onBackground,
      Color? outline, Color? outlineVariant, Color? shadow, Color? scrim, Color? inverseSurface,
      Color? onInverseSurface});
  external static ColorScheme fromSeed({Color seedColor, Brightness? brightness});
  external static ColorScheme light({Color? primary, Color? onPrimary, Color? primaryContainer, Color? onPrimaryContainer, Color? secondary, Color? onSecondary, Color? secondaryContainer, Color? onSecondaryContainer, Color? tertiary, Color? onTertiary, Color? error, Color? onError, Color? errorContainer, Color? onErrorContainer, Color? surface, Color? onSurface, Color? surfaceVariant, Color? onSurfaceVariant, Color? background, Color? onBackground, Color? outline, Color? shadow, Color? inverseSurface, Color? onInverseSurface, Color? inversePrimary, Brightness? brightness});
  external static ColorScheme dark({Color? primary, Color? onPrimary, Color? primaryContainer, Color? onPrimaryContainer, Color? secondary, Color? onSecondary, Color? secondaryContainer, Color? onSecondaryContainer, Color? tertiary, Color? onTertiary, Color? error, Color? onError, Color? errorContainer, Color? onErrorContainer, Color? surface, Color? onSurface, Color? surfaceVariant, Color? onSurfaceVariant, Color? background, Color? onBackground, Color? outline, Color? shadow, Color? inverseSurface, Color? onInverseSurface, Color? inversePrimary, Brightness? brightness});
  external Brightness get brightness;
  external Color get primary;
  external Color get onPrimary;
  external Color get primaryContainer;
  external Color get onPrimaryContainer;
  external Color get inversePrimary;
  external Color get secondary;
  external Color get onSecondary;
  external Color get secondaryContainer;
  external Color get onSecondaryContainer;
  external Color get tertiary;
  external Color get onTertiary;
  external Color get tertiaryContainer;
  external Color get onTertiaryContainer;
  external Color get error;
  external Color get onError;
  external Color get errorContainer;
  external Color get onErrorContainer;
  external Color get surface;
  external Color get onSurface;
  external Color get surfaceVariant;
  external Color get onSurfaceVariant;
  external Color get background;
  external Color get onBackground;
  external Color get outline;
  external Color get outlineVariant;
  external Color get shadow;
  external Color get scrim;
  external Color get inverseSurface;
  external Color get onInverseSurface;
  external ColorScheme copyWith({Brightness? brightness, Color? primary, Color? onPrimary,
      Color? primaryContainer, Color? onPrimaryContainer, Color? secondary, Color? onSecondary,
      Color? secondaryContainer, Color? tertiary, Color? error, Color? onError, Color? surface,
      Color? onSurface, Color? surfaceVariant, Color? onSurfaceVariant, Color? background,
      Color? onBackground, Color? outline, Color? inversePrimary, Color? inverseSurface, Color? shadow});
}

@JavaName('com.codename1.flutter.material.TextTheme')
class TextTheme {
  external TextStyle get displayLarge;
  external TextStyle get displayMedium;
  external TextStyle get displaySmall;
  external TextStyle get headlineLarge;
  external TextStyle get headlineMedium;
  external TextStyle get headlineSmall;
  external TextStyle get titleLarge;
  external TextStyle get titleMedium;
  external TextStyle get titleSmall;
  external TextStyle get bodyLarge;
  external TextStyle get bodyMedium;
  external TextStyle get bodySmall;
  external TextStyle get labelLarge;
  external TextStyle get labelMedium;
  external TextStyle get labelSmall;
  external TextTheme copyWith({TextStyle? displayLarge, TextStyle? displayMedium, TextStyle? displaySmall,
      TextStyle? headlineLarge, TextStyle? headlineMedium, TextStyle? headlineSmall, TextStyle? titleLarge,
      TextStyle? titleMedium, TextStyle? titleSmall, TextStyle? bodyLarge, TextStyle? bodyMedium,
      TextStyle? bodySmall, TextStyle? labelLarge, TextStyle? labelMedium, TextStyle? labelSmall});
  external TextTheme apply({String? fontFamily, double? fontSizeFactor, double? fontSizeDelta,
      Color? displayColor, Color? bodyColor, dynamic decoration, dynamic decorationColor});
}

@JavaName('com.codename1.flutter.material.Theme')
abstract class Theme {
  external static ThemeData of(BuildContext context);
  external static Brightness brightnessOf(BuildContext context);
}

// --- M2 additions -------------------------------------------------------

@JavaName('com.codename1.flutter.BoxFit')
enum BoxFit { fill, contain, cover, fitWidth, fitHeight, none }

@JavaName('com.codename1.flutter.Alignment')
abstract class Alignment {
  external static Alignment get topLeft;
  external static Alignment get topCenter;
  external static Alignment get topRight;
  external static Alignment get centerLeft;
  external static Alignment get center;
  external static Alignment get centerRight;
  external static Alignment get bottomLeft;
  external static Alignment get bottomCenter;
  external static Alignment get bottomRight;
}

@JavaName('com.codename1.flutter.widgets.ListView')
class ListView extends Widget {
  external ListView({Key? key, List<Widget> children, EdgeInsets? padding, bool? shrinkWrap});
  external static ListView builder({Key? key, int? itemCount, IndexedWidgetBuilder itemBuilder, EdgeInsets? padding, bool? shrinkWrap, Object? physics, Object? scrollDirection, Object? controller, String? restorationId, bool? primary, double? itemExtent, bool? reverse});
  external static ListView separated({Key? key, bool? primary, int? itemCount, IndexedWidgetBuilder itemBuilder, IndexedWidgetBuilder separatorBuilder, EdgeInsets? padding, bool? shrinkWrap});
}

@JavaName('com.codename1.flutter.widgets.GridView')
class GridView extends Widget {
  external static GridView count({Key? key, String? restorationId, Object? physics, bool? primary, int crossAxisCount, double? childAspectRatio, double? mainAxisSpacing, double? crossAxisSpacing, EdgeInsets? padding, List<Widget> children});
  external static GridView builder({Key? key, int? itemCount, IndexedWidgetBuilder itemBuilder, Object? gridDelegate, EdgeInsets? padding, bool? shrinkWrap, Object? physics});
}

@JavaName('com.codename1.flutter.widgets.SingleChildScrollView')
class SingleChildScrollView extends Widget {
  external SingleChildScrollView({Key? key, EdgeInsets? padding, Axis? scrollDirection, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Image')
class Image extends Widget {
  external Image({Key? key, ImageProvider? image, double? width, double? height, BoxFit? fit, bool? excludeFromSemantics, Object? frameBuilder});
  external static Image asset(String name, {Key? key, double? width, double? height, BoxFit? fit, String? package, bool? excludeFromSemantics, bool? gaplessPlayback, int? cacheWidth, int? cacheHeight, Color? color, Object? colorBlendMode, Object? alignment, Object? semanticLabel});
  external static Image network(String src, {Key? key, double? width, double? height, BoxFit? fit});
}

@JavaName('com.codename1.flutter.widgets.Stack')
class Stack extends Widget {
  external Stack({Key? key, Alignment? alignment, List<Widget> children});
}

@JavaName('com.codename1.flutter.widgets.Positioned')
class Positioned extends Widget {
  external Positioned({Key? key, double? left, double? top, double? right, double? bottom, double? width, double? height, Widget child});
  external static Positioned fill({Key? key, double? left, double? top, double? right, double? bottom, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Align')
class Align extends Widget {
  external Align({Key? key, Alignment? alignment, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.ConstrainedBox')
class ConstrainedBox extends Widget {
  external ConstrainedBox({Key? key, BoxConstraints constraints, Widget? child});
}

@JavaName('com.codename1.flutter.rendering.BoxConstraints')
class BoxConstraints {
  external BoxConstraints({double? minWidth, double? maxWidth, double? minHeight, double? maxHeight});
  external double get minWidth;
  external double get maxWidth;
  external double get minHeight;
  external double get maxHeight;
  external bool get hasBoundedWidth;
  external bool get hasBoundedHeight;
  external Size get biggest;
  external Size get smallest;
}

@JavaName('com.codename1.flutter.material.Card')
class Card extends Widget {
  external Card({Key? key, Color? color, double? elevation, EdgeInsets? margin, Widget? child});
}

@JavaName('com.codename1.flutter.material.Divider')
class Divider extends Widget {
  external Divider({Key? key, double? height, double? thickness, Color? color});
}

@JavaName('com.codename1.flutter.material.ElevatedButton')
class ElevatedButton extends Widget {
  external ElevatedButton({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? child});
  external static ButtonStyle styleFrom({Color? foregroundColor, Color? backgroundColor,
      Color? shadowColor, double? elevation, TextStyle? textStyle, EdgeInsets? padding,
      dynamic side, dynamic shape, dynamic alignment, dynamic tapTargetSize, dynamic visualDensity});
  external static ElevatedButton icon({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? icon, Widget? label});
}

@JavaName('com.codename1.flutter.material.TextButton')
class TextButton extends Widget {
  external TextButton({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? child});
  external static ButtonStyle styleFrom({Color? foregroundColor, Color? backgroundColor,
      Color? shadowColor, double? elevation, TextStyle? textStyle, EdgeInsets? padding,
      dynamic side, dynamic shape, dynamic alignment, dynamic tapTargetSize, dynamic visualDensity});
  external static TextButton icon({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? icon, Widget? label});
}

@JavaName('com.codename1.flutter.material.OutlinedButton')
class OutlinedButton extends Widget {
  external OutlinedButton({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? child});
  external static ButtonStyle styleFrom({Color? foregroundColor, Color? backgroundColor,
      Color? shadowColor, double? elevation, TextStyle? textStyle, EdgeInsets? padding,
      dynamic side, dynamic shape, dynamic alignment, dynamic tapTargetSize, dynamic visualDensity});
  external static OutlinedButton icon({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? icon, Widget? label});
}

@JavaName('com.codename1.flutter.material.IconButton')
class IconButton extends Widget {
  external IconButton({Key? key, VoidCallback? onPressed, Widget? icon, double? iconSize, Color? color});
}

@JavaName('com.codename1.flutter.widgets.GestureDetector')
class GestureDetector extends Widget {
  external GestureDetector({Key? key, VoidCallback? onTap, VoidCallback? onLongPress, Widget? child});
}

@JavaName('com.codename1.flutter.material.InkWell')
class InkWell extends Widget {
  external InkWell({Key? key, VoidCallback? onTap, VoidCallback? onLongPress, Widget? child});
}

// --- M3 additions -------------------------------------------------------
// Callback typedefs below (StringCallback etc.) are transpiler-internal
// names mapped to dart.runtime.Funcs SAMs; they type untyped lambda params.

@JavaName('com.codename1.flutter.material.TextEditingController')
class TextEditingController {
  external TextEditingController({String? text});
  external String get text;
  external void setText(String value);
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void clear();
  // Releases the controller's listeners — Flutter's `ChangeNotifier.dispose`.
  external void dispose();
}

@JavaName('com.codename1.flutter.material.InputDecoration')
class InputDecoration {
  external InputDecoration({String? labelText, String? hintText});
  external static InputDecoration collapsed({String? hintText, dynamic hintStyle, dynamic border, bool? filled, Color? fillColor});
}

@JavaName('com.codename1.flutter.material.TextField')
class TextField extends Widget {
  external TextField({Key? key, TextEditingController? controller, InputDecoration? decoration, bool? obscureText, bool? enabled, StringCallback? onChanged, StringCallback? onSubmitted});
}

@JavaName('com.codename1.flutter.material.Checkbox')
class Checkbox extends Widget {
  external Checkbox({Key? key, bool? value, bool tristate, BoolCallback? onChanged});
}

@JavaName('com.codename1.flutter.material.Radio')
class Radio<T> extends Widget {
  external Radio({Key? key, Object value, Object? groupValue, DynamicCallback? onChanged});
}

@JavaName('com.codename1.flutter.material.Switch')
class Switch extends Widget {
  external Switch({Key? key, bool value, BoolCallback? onChanged});
}

@JavaName('com.codename1.flutter.material.Slider')
class Slider extends Widget {
  external Slider({Key? key, double value, double? min, double? max, int? divisions, DoubleCallback? onChanged});
}

@JavaName('com.codename1.flutter.navigation.MaterialPageRoute')
class MaterialPageRoute<T> extends Route<T> {
  external MaterialPageRoute({WidgetBuilder builder});
}

@JavaName('com.codename1.flutter.navigation.Navigator')
class Navigator extends Widget {
  external Navigator({Key? key});
  // push completes with the value the route is popped with, as Flutter's does.
  external static Future<Object?> push(BuildContext context, MaterialPageRoute route);
  external static void pop(BuildContext context, [Object? result]);
  external static NavigatorState of(BuildContext context, {bool? rootNavigator});
  external static String restorablePush(BuildContext context, dynamic routeBuilder, {dynamic arguments});
  external static bool maybePop(BuildContext context);
}

@JavaName('com.codename1.flutter.material.Dialogs.showDialog')
external void showDialog({BuildContext context, WidgetBuilder builder});

@JavaName('com.codename1.flutter.material.AlertDialog')
class AlertDialog extends Widget {
  external AlertDialog({Key? key, Widget? title, Widget? content, List<Widget>? actions});
}

@JavaName('com.codename1.flutter.material.SnackBar')
class SnackBar extends Widget {
  external SnackBar({Key? key, Widget content, Duration? duration});
}

@JavaName('com.codename1.flutter.material.ScaffoldMessenger')
abstract class ScaffoldMessenger {
  external static ScaffoldMessengerState of(BuildContext context);
}

@JavaName('com.codename1.flutter.material.ScaffoldMessengerState')
abstract class ScaffoldMessengerState {
  external void showSnackBar(SnackBar snackBar);
  // Dismisses the visible SnackBar immediately — Flutter's `hideCurrentSnackBar`.
  external void hideCurrentSnackBar({dynamic reason});
}

@JavaName('com.codename1.flutter.material.Drawer')
class Drawer extends Widget {
  external Drawer({Key? key, Widget? child});
}

@JavaName('com.codename1.flutter.material.BottomNavigationBarItem')
class BottomNavigationBarItem {
  external BottomNavigationBarItem({Widget? icon, Widget? activeIcon, String? label,
      Color? backgroundColor, String? tooltip});
  // The item's glyph and label — Flutter's `BottomNavigationBarItem.icon/label`.
  external Widget get icon;
  external String? get label;
}

@JavaName('com.codename1.flutter.material.BottomNavigationBar')
class BottomNavigationBar extends Widget {
  external BottomNavigationBar({Key? key, List<BottomNavigationBarItem> items, int? currentIndex, IntCallback? onTap});
}

@JavaName('com.codename1.flutter.material.ListTile')
class ListTile extends Widget {
  external ListTile({Key? key, Widget? leading, Widget? title, Widget? subtitle, Widget? trailing, VoidCallback? onTap});
}

// --- M4 additions -------------------------------------------------------

@JavaName('com.codename1.flutter.ThemeMode')
enum ThemeMode { system, light, dark }

@JavaName('com.codename1.flutter.Brightness')
enum Brightness { light, dark }

@JavaName('com.codename1.flutter.MediaQuery')
abstract class MediaQuery {
  external static MediaQueryData of(BuildContext context);
  // Targeted inherited-lookup helpers that depend only on one MediaQueryData
  // aspect — Flutter's `MediaQuery.sizeOf` / `paddingOf` / `viewInsetsOf`.
  external static Size sizeOf(BuildContext context);
  external static EdgeInsets paddingOf(BuildContext context);
  external static EdgeInsets viewInsetsOf(BuildContext context);
  external static Widget removePadding({BuildContext context, bool? removeLeft, bool? removeTop, bool? removeRight, bool? removeBottom, Widget? child});
}

@JavaName('com.codename1.flutter.MediaQueryData')
abstract class MediaQueryData {
  external Size get size;
  external double get devicePixelRatio;
  external Brightness get platformBrightness;
  external double get textScaleFactor;
  external EdgeInsets get padding;
  // The insets covered by system UI (keyboard, notches) — Flutter's
  // `MediaQueryData.viewInsets` / `viewPadding`.
  external EdgeInsets get viewInsets;
  external EdgeInsets get viewPadding;
  external MediaQueryData copyWith({Size? size, double? devicePixelRatio, double? textScaleFactor,
      EdgeInsets? padding, Brightness? platformBrightness});
  // Returns a copy with one or more padding edges removed — Flutter's
  // `MediaQueryData.removePadding`.
  external MediaQueryData removePadding({bool? removeLeft, bool? removeTop,
      bool? removeRight, bool? removeBottom});
  external MediaQueryData removeViewInsets({bool? removeLeft, bool? removeTop,
      bool? removeRight, bool? removeBottom});
}

@JavaName('com.codename1.flutter.rendering.Size')
class Size {
  external Size(double width, double height);
  external static Size fromRadius(double radius);
  external static Size fromHeight(double height);
  external static Size fromWidth(double width);
  external double get width;
  external double get height;
  external double get shortestSide;
  external double get longestSide;
  external double get aspectRatio;
  external bool get isEmpty;
  external Size get flipped;
  // The offset to the center of a rect of this size with the given origin —
  // Flutter's `Size.center(Offset origin)`.
  external Offset center(Offset origin);
  external Offset topLeft(Offset origin);
  external Offset topCenter(Offset origin);
  external Offset bottomCenter(Offset origin);
  external bool contains(Offset offset);
}

@JavaName('com.codename1.flutter.widgets.RichText')
class RichText extends Widget {
  external RichText({Key? key, TextSpan text, TextAlign? textAlign});
}

// The base of the styled-text tree — Flutter's `InlineSpan` (TextSpan's supertype).
@JavaName('com.codename1.flutter.widgets.InlineSpan')
class InlineSpan {
  external String toPlainText();
}

@JavaName('com.codename1.flutter.widgets.TextSpan')
class TextSpan extends InlineSpan {
  external TextSpan({String? text, TextStyle? style, List<TextSpan>? children});
  // Flattens this span tree to its raw text — Flutter's `InlineSpan.toPlainText`.
  external String toPlainText();
}

// Colours for text selection. Recorded rather than applied - selection is drawn with
// Codename One's own theme colours - but three of the four studies name one, so the type
// has to exist for their themes to transpile.
@JavaName('com.codename1.flutter.material.TextSelectionThemeData')
class TextSelectionThemeData {
  external TextSelectionThemeData({Color? cursorColor, Color? selectionColor, Color? selectionHandleColor});
}

// Per-platform page transition builders. Route transitions come from Codename One's own
// machinery; Rally configures one of these, so the type must resolve.
@JavaName('com.codename1.flutter.material.PageTransitionsTheme')
class PageTransitionsTheme {
  external PageTransitionsTheme({Object? builders});
}

// A route transition for one platform. Recorded, not run - Codename One drives its own
// transitions - but Rally builds a Map<TargetPlatform, PageTransitionsBuilder>, so the
// type must resolve for its theme to compile.
@JavaName('com.codename1.flutter.material.PageTransitionsBuilder')
class PageTransitionsBuilder {}
