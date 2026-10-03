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
abstract class BuildContext {}

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
}

@JavaName('com.codename1.flutter.Colors')
abstract class Colors {
  external static Color get deepPurple;
  external static Color get blue;
  external static Color get red;
  external static Color get green;
  external static Color get orange;
  external static Color get purple;
  external static Color get white;
  external static Color get black;
  external static Color get grey;
  external static Color get transparent;
}

@JavaName('com.codename1.flutter.EdgeInsets')
class EdgeInsets {
  external static EdgeInsets all(double value);
  external static EdgeInsets only({double left, double top, double right, double bottom});
  external static EdgeInsets symmetric({double horizontal, double vertical});
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
}

@JavaName('com.codename1.flutter.IconData')
class IconData {}

@JavaName('com.codename1.flutter.Icons')
abstract class Icons {
  external static IconData get add;
  external static IconData get remove;
  external static IconData get menu;
  external static IconData get home;
  external static IconData get settings;
  external static IconData get search;
  external static IconData get arrow_back;
  external static IconData get arrow_forward;
  external static IconData get close;
  external static IconData get check;
  external static IconData get edit;
  external static IconData get delete;
  external static IconData get favorite;
  external static IconData get share;
  external static IconData get more_vert;
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
}

@JavaName('com.codename1.flutter.widgets.Expanded')
class Expanded extends Widget {
  external Expanded({Key? key, int flex, Widget child});
}

// --- material ---------------------------------------------------------

@JavaName('com.codename1.flutter.material.MaterialApp')
class MaterialApp extends Widget {
  external MaterialApp({Key? key, String? title, ThemeData? theme, ThemeData? darkTheme, ThemeMode? themeMode, Widget? home});
}

@JavaName('com.codename1.flutter.material.Scaffold')
class Scaffold extends Widget {
  external Scaffold({Key? key, Widget? appBar, Widget? body, Widget? floatingActionButton, Widget? drawer, Widget? bottomNavigationBar});
}

@JavaName('com.codename1.flutter.material.AppBar')
class AppBar extends Widget {
  external AppBar({Key? key, Widget? title, Color? backgroundColor, bool? centerTitle});
}

@JavaName('com.codename1.flutter.material.FloatingActionButton')
class FloatingActionButton extends Widget {
  external FloatingActionButton({Key? key, VoidCallback? onPressed, String? tooltip, Widget? child});
}

@JavaName('com.codename1.flutter.material.ThemeData')
class ThemeData {
  external ThemeData({ColorScheme? colorScheme, bool? useMaterial3, Brightness? brightness});
  external ColorScheme get colorScheme;
  external TextTheme get textTheme;
}

@JavaName('com.codename1.flutter.material.ColorScheme')
class ColorScheme {
  external static ColorScheme fromSeed({Color seedColor, Brightness? brightness});
  external Color get primary;
  external Color get inversePrimary;
  external Color get onPrimary;
  external Color get surface;
  external Color get onSurface;
  external Color get secondary;
}

@JavaName('com.codename1.flutter.material.TextTheme')
class TextTheme {
  external TextStyle get headlineMedium;
  external TextStyle get bodyMedium;
  external TextStyle get titleLarge;
}

@JavaName('com.codename1.flutter.material.Theme')
abstract class Theme {
  external static ThemeData of(BuildContext context);
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
}

@JavaName('com.codename1.flutter.widgets.GridView')
class GridView extends Widget {
  external static GridView count({Key? key, int crossAxisCount, double? childAspectRatio, double? mainAxisSpacing, double? crossAxisSpacing, EdgeInsets? padding, List<Widget> children});
}

@JavaName('com.codename1.flutter.widgets.SingleChildScrollView')
class SingleChildScrollView extends Widget {
  external SingleChildScrollView({Key? key, EdgeInsets? padding, Axis? scrollDirection, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Image')
class Image extends Widget {
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
  external ElevatedButton({Key? key, VoidCallback? onPressed, Widget? child});
}

@JavaName('com.codename1.flutter.material.TextButton')
class TextButton extends Widget {
  external TextButton({Key? key, VoidCallback? onPressed, Widget? child});
}

@JavaName('com.codename1.flutter.material.OutlinedButton')
class OutlinedButton extends Widget {
  external OutlinedButton({Key? key, VoidCallback? onPressed, Widget? child});
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
  external void clear();
}

@JavaName('com.codename1.flutter.material.InputDecoration')
class InputDecoration {
  external InputDecoration({String? labelText, String? hintText});
}

@JavaName('com.codename1.flutter.material.TextField')
class TextField extends Widget {
  external TextField({Key? key, TextEditingController? controller, InputDecoration? decoration, bool? obscureText, bool? enabled, StringCallback? onChanged, StringCallback? onSubmitted});
}

@JavaName('com.codename1.flutter.material.Checkbox')
class Checkbox extends Widget {
  external Checkbox({Key? key, bool value, BoolCallback? onChanged});
}

@JavaName('com.codename1.flutter.material.Radio')
class Radio extends Widget {
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
class MaterialPageRoute {
  external MaterialPageRoute({WidgetBuilder builder});
}

@JavaName('com.codename1.flutter.navigation.Navigator')
abstract class Navigator {
  external static void push(BuildContext context, MaterialPageRoute route);
  external static void pop(BuildContext context);
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
}

@JavaName('com.codename1.flutter.material.Drawer')
class Drawer extends Widget {
  external Drawer({Key? key, Widget? child});
}

@JavaName('com.codename1.flutter.material.BottomNavigationBarItem')
class BottomNavigationBarItem {
  external BottomNavigationBarItem({Widget? icon, String? label});
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
}

@JavaName('com.codename1.flutter.MediaQueryData')
abstract class MediaQueryData {
  external Size get size;
  external double get devicePixelRatio;
  external Brightness get platformBrightness;
}

@JavaName('com.codename1.flutter.rendering.Size')
class Size {
  external Size(double width, double height);
  external double get width;
  external double get height;
}

@JavaName('com.codename1.flutter.widgets.RichText')
class RichText extends Widget {
  external RichText({Key? key, TextSpan text, TextAlign? textAlign});
}

@JavaName('com.codename1.flutter.widgets.TextSpan')
class TextSpan {
  external TextSpan({String? text, TextStyle? style, List<TextSpan>? children});
}
