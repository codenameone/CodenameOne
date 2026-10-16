// Codename One Flutter runtime API stubs — state management & theming value
// classes (new_gallery "stateMgmt" gap category).
//
// Same conventions as flutter_material.dart:
//  - positional constructor parameters  -> Java constructor arguments
//  - named constructor parameters        -> void setter methods of the same name
//  - instance getters                    -> no-arg method calls (name())
//  - a method whose return type is a bare generic type parameter (e.g. `T`)
//    is emitted with its <T> witness recovered as a trailing T.class token; the
//    Java runtime method therefore takes a trailing Class<T> parameter.
//
// Types that belong to other gap categories (ShapeBorder, MaterialStateProperty,
// SystemUiOverlayStyle, SnackBarBehavior, InputBorder, ...) are typed `dynamic`
// here so these classes resolve without coupling to another agent's stubs.

// --- InheritedWidget --------------------------------------------------

@JavaName('com.codename1.flutter.widgets.InheritedWidget')
class InheritedWidget extends Widget {
  external InheritedWidget({Key? key, Widget? child});
  bool updateShouldNotify(InheritedWidget oldWidget);
}

// --- listenable / change notification ---------------------------------

// The root of the observable protocol — Flutter's `Listenable`. AnimatedWidget
// takes one; ChangeNotifier and Animation are Listenables.
@JavaName('com.codename1.flutter.foundation.Listenable')
abstract class Listenable {
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
}

@JavaName('com.codename1.flutter.foundation.ChangeNotifier')
class ChangeNotifier extends Listenable {
  external ChangeNotifier();
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void notifyListeners();
  external void dispose();
  external bool get hasListeners;
}

// --- provider package -------------------------------------------------

@JavaName('com.codename1.flutter.provider.SingleChildWidget')
class SingleChildWidget extends Widget {
  external SingleChildWidget({Key? key, Widget? child});
}

@JavaName('com.codename1.flutter.provider.Provider')
class Provider extends SingleChildWidget {
  external Provider({Key? key, dynamic create, Object? value, bool? lazy, Widget? child});
  // named constructors are declared as static factory methods for the emitter
  external static Provider value({Key? key, Object value, Widget? child});
  external static T of(BuildContext context, {bool listen});
}

@JavaName('com.codename1.flutter.provider.ChangeNotifierProvider')
class ChangeNotifierProvider extends Provider {
  external ChangeNotifierProvider({Key? key, dynamic create, bool? lazy, Widget? child});
  external static ChangeNotifierProvider value({Key? key, Object value, Widget? child});
}

@JavaName('com.codename1.flutter.provider.MultiProvider')
class MultiProvider extends Widget {
  external MultiProvider({Key? key, List<SingleChildWidget> providers, Widget child});
}

@JavaName('com.codename1.flutter.provider.Consumer')
class Consumer<T> extends StatelessWidget {
  external Consumer({Key? key, dynamic builder, Widget? child});
}

// --- scoped_model package ---------------------------------------------

@JavaName('com.codename1.flutter.scopedmodel.Model')
class Model {
  external Model();
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void notifyListeners();
}

@JavaName('com.codename1.flutter.scopedmodel.ScopedModel')
class ScopedModel extends Widget {
  external ScopedModel({Key? key, Object model, Widget child});
  external static T of(BuildContext context, {bool rebuildOnChange});
}

@JavaName('com.codename1.flutter.scopedmodel.ScopedModelDescendant')
class ScopedModelDescendant<T> extends StatelessWidget {
  external ScopedModelDescendant({Key? key, dynamic builder, bool? rebuildOnChange, Widget? child});
}

// --- Locale -----------------------------------------------------------

@JavaName('com.codename1.flutter.Locale')
class Locale {
  external Locale(String languageCode, [String? countryCode]);
  external String get languageCode;
  external String? get countryCode;
}

// --- theming value classes --------------------------------------------

@JavaName('com.codename1.flutter.material.IconThemeData')
class IconThemeData {
  external IconThemeData({Color? color, double? size, double? opacity, double? fill,
      double? weight, double? grade, double? opticalSize, dynamic shadows,
      bool? applyTextScaling});
  external Color? get color;
  external double? get size;
  external double? get opacity;
  external IconThemeData copyWith({Color? color, double? size, double? opacity, double? fill,
      double? weight, double? grade, double? opticalSize, dynamic shadows,
      bool? applyTextScaling});
}

@JavaName('com.codename1.flutter.material.AppBarTheme')
class AppBarTheme {
  external AppBarTheme({Color? backgroundColor, Color? foregroundColor, Color? color,
      Color? shadowColor, Color? surfaceTintColor, double? elevation,
      double? scrolledUnderElevation, IconThemeData? iconTheme, IconThemeData? actionsIconTheme,
      TextStyle? titleTextStyle, TextStyle? toolbarTextStyle, bool? centerTitle,
      double? titleSpacing, double? toolbarHeight, dynamic systemOverlayStyle,
      dynamic shape});
  external Color? get backgroundColor;
  external double? get elevation;
  external IconThemeData? get iconTheme;
}

@JavaName('com.codename1.flutter.material.ChipThemeData')
class ChipThemeData {
  external ChipThemeData({Color? backgroundColor, Color? disabledColor, Color? selectedColor,
      Color? secondarySelectedColor, Color? deleteIconColor, Color? shadowColor,
      EdgeInsets? padding, EdgeInsets? labelPadding, dynamic shape, TextStyle? labelStyle,
      TextStyle? secondaryLabelStyle, Brightness? brightness, double? elevation,
      double? pressElevation});
  external Color? get backgroundColor;
  external Color? get selectedColor;
  external Color? get secondarySelectedColor;
  external Color? get disabledColor;
  external Brightness? get brightness;
}

@JavaName('com.codename1.flutter.material.CheckboxThemeData')
class CheckboxThemeData {
  external CheckboxThemeData({dynamic fillColor, dynamic checkColor, dynamic overlayColor,
      dynamic materialTapTargetSize, dynamic shape, dynamic side, dynamic visualDensity,
      dynamic mouseCursor, dynamic splashRadius});
}

@JavaName('com.codename1.flutter.material.BottomAppBarThemeData')
class BottomAppBarThemeData {
  external BottomAppBarThemeData({Color? color, Color? surfaceTintColor, Color? shadowColor,
      double? elevation, double? height, EdgeInsets? padding, dynamic shape});
  external Color? get color;
  external double? get elevation;
}

@JavaName('com.codename1.flutter.material.CardTheme')
class CardTheme {
  external CardTheme({Color? color, Color? shadowColor, Color? surfaceTintColor,
      double? elevation, EdgeInsets? margin, dynamic shape, dynamic clipBehavior});
  external Color? get color;
  external double? get elevation;
}

@JavaName('com.codename1.flutter.material.CardThemeData')
class CardThemeData {
  external CardThemeData({Color? color, Color? shadowColor, Color? surfaceTintColor,
      double? elevation, EdgeInsets? margin, dynamic shape, dynamic clipBehavior});
  external Color? get color;
  external double? get elevation;
}

@JavaName('com.codename1.flutter.material.DividerThemeData')
class DividerThemeData {
  external DividerThemeData({double? thickness, Color? color, double? space, double? indent,
      double? endIndent});
  external double? get thickness;
  external Color? get color;
  external double? get space;
}
