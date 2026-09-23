// Codename One Flutter runtime API stubs — "dartCore" category (new_gallery).
//
// dart:core / dart:async / dart:math / dart:typed_data / dart:intl equivalents,
// GoogleFonts, Directionality/TextDirection, Clipboard, SystemChrome and the
// Material localization basics used by the Flutter new_gallery integration app.
//
// Same conventions as flutter_material.dart:
//  - positional constructor params -> Java constructor arguments
//  - named constructor params      -> canonical positional order (declared order)
//  - instance getters              -> no-arg method calls (name())
//  - static getters                -> static field access (Name.field)
//  - named "factory" constructors  -> `external static X foo(...)` = static method
//
// Library-prefix trick: dart:math is imported `as math` and dart:intl `as intl`.
// The transpiler has no import-prefix table, so `math.pi` / `intl.Intl` resolve
// the receiver identifier as a *type*. We therefore expose a stub class literally
// named `math` (-> dart.math.DartMath) and `intl` (-> IntlLib holding `Intl`),
// which makes the prefixed member access resolve as static/instance access.

// --- dart:core : Object / Comparable ---------------------------------------

// The root of the Dart class hierarchy — `Object`. new_gallery reaches it for
// the static hash combiners (`Object.hash(a, b)` in == overrides / hashCode)
// and for `other.runtimeType`. The class @JavaName maps the type + instance
// members onto java.lang.Object; the static `hash`/`hashAll` combinators are
// backed by java.util.Objects.hash.
@JavaName('java.lang.Object')
class Object {
  external Object();
  external static int hash(Object? a, Object? b, [Object? c, Object? d, Object? e, Object? f, Object? g, Object? h]);
  external static int hashAll(Iterable objects);
  external Object get runtimeType;
  external int get hashCode;
  external String toString();
}

// A totally-ordered type — Dart's `Comparable<T>`. new_gallery's data-table demo
// sorts with `Comparable.compare(a, b)` and types cell values as `Comparable<T>`.
// Instances/type map to java.lang.Comparable; the static `compare` combinator is
// backed by dart.core.DartComparable.compare.
@JavaName('java.lang.Comparable')
abstract class Comparable<T> {
  external static int compare(Comparable a, Comparable b);
  external int compareTo(T other);
}

// --- dart:core : DateTime --------------------------------------------------

@JavaName('dart.core.DateTime')
class DateTime {
  // Dart's own defaults, stated. Without them an omitted month or day is filled
  // with the type's zero, so DateTime(2024) reached the runtime as month 0, day 0
  // -- indistinguishable from DateTime(2024, 3, 0), which Dart defines as the last
  // day of February. The runtime then had to clamp both to 1, and the clamp broke
  // the normalization calendar code relies on.
  external DateTime(int year, [int month = 1, int day = 1, int hour = 0, int minute = 0, int second = 0, int millisecond = 0, int microsecond = 0]);
  external static DateTime now();
  external static DateTime utc(int year, [int month = 1, int day = 1, int hour = 0, int minute = 0, int second = 0, int millisecond = 0, int microsecond = 0]);
  external static DateTime fromMillisecondsSinceEpoch(int millisecondsSinceEpoch, {bool isUtc});
  external int get year;
  external int get month;
  external int get day;
  external int get hour;
  external int get minute;
  external int get second;
  external int get millisecond;
  external int get weekday;
  external int get millisecondsSinceEpoch;
  external int get microsecondsSinceEpoch;
  external DateTime add(Duration duration);
  external DateTime subtract(Duration duration);
  external Duration difference(DateTime other);
  external bool isBefore(DateTime other);
  external bool isAfter(DateTime other);
  external bool isAtSameMomentAs(DateTime other);
  external DateTime toLocal();
  external DateTime toUtc();
  external int compareTo(DateTime other);
}

@JavaName('dart.core.DateTimeRange')
class DateTimeRange {
  external DateTimeRange({DateTime start, DateTime end});
  external DateTime get start;
  external DateTime get end;
  external Duration get duration;
}

// --- dart:core : RegExp / Match --------------------------------------------
// The runtime dart.core.RegExp/RegExpMatch are backed by java.util.regex. Named
// ctor params (multiLine/caseSensitive/unicode/dotAll) lower to post-construction
// setters; the getter `pattern` reads the original source string.

@JavaName('dart.core.RegExp')
class RegExp {
  external RegExp(String source, {bool multiLine, bool caseSensitive, bool unicode, bool dotAll});
  external bool hasMatch(String input);
  external RegExpMatch? firstMatch(String input);
  external Iterable<RegExpMatch> allMatches(String input);
  external String? stringMatch(String input);
  external String get pattern;
}

@JavaName('dart.core.RegExpMatch')
class RegExpMatch {
  external String? group(int group);
  external int get groupCount;
  external int get start;
  external int get end;
  external String get input;
}

// --- dart:core : StringBuffer ----------------------------------------------

@JavaName('dart.core.StringBuffer')
class StringBuffer {
  external StringBuffer([Object content]);
  external int get length;
  external bool get isEmpty;
  external bool get isNotEmpty;
  external void write(Object object);
  external void writeln([Object object]);
  external void writeCharCode(int charCode);
  external void writeAll(Iterable objects, [String separator]);
  external void clear();
}

// --- dart:core : MapEntry --------------------------------------------------
// Element type of Map.entries; `key`/`value` getters read the pair.

@JavaName('dart.core.MapEntry')
class MapEntry<K, V> {
  external MapEntry(K key, V value);
  external K get key;
  external V get value;
}

// --- dart:async : Timer ----------------------------------------------------

@JavaName('dart.async.Timer')
class Timer {
  external Timer(Duration duration, VoidCallback callback);
  external void cancel();
  external bool get isActive;
}

// The eventual-value type — dart:async's `Future<T>`. `await`, the named
// constructors (Future.value / Future.delayed) and `.then` / `.whenComplete`
// are handled directly by the transpiler; `.catchError` (chained after `.then`
// on the demo page's clipboard copy) falls through to this stub.
@JavaName('dart.async.Future')
abstract class Future<T> {
  external Future then(Object onValue, {Object? onError});
  external Future catchError(Object onError, {Object? test});
  external Future whenComplete(Object action);
}

// --- dart:typed_data -------------------------------------------------------

@JavaName('dart.typed_data.Uint8List')
class Uint8List {
  external Uint8List(int length);
  external static Uint8List fromList(List<int> elements);
  external int get length;
}

@JavaName('dart.typed_data.ByteData')
class ByteData {
  external ByteData(int length);
  external int get lengthInBytes;
  external int getUint8(int byteOffset);
  external void setUint8(int byteOffset, int value);
  external int getInt32(int byteOffset);
  external void setInt32(int byteOffset, int value);
}

// --- dart:math -------------------------------------------------------------
// `math` is the import-prefix stub (import 'dart:math' as math;).

@JavaName('dart.math.DartMath')
abstract class math {
  external static double get pi;
  external static double get e;
  external static double min(double a, double b);
  external static double max(double a, double b);
  external static double pow(double x, double exponent);
  external static double sqrt(double x);
  external static double sin(double x);
  external static double cos(double x);
  external static double tan(double x);
  external static double asin(double x);
  external static double acos(double x);
  external static double atan(double x);
  external static double atan2(double a, double b);
  external static double exp(double x);
  external static double log(double x);
}

// Unprefixed dart:math top-level functions (import 'dart:math';).
@JavaName('dart.math.DartMath.min')
external double min(double a, double b);
@JavaName('dart.math.DartMath.max')
external double max(double a, double b);
@JavaName('dart.math.DartMath.sqrt')
external double sqrt(double x);
@JavaName('dart.math.DartMath.pow')
external double pow(double x, double exponent);
@JavaName('dart.math.DartMath.sin')
external double sin(double x);
@JavaName('dart.math.DartMath.cos')
external double cos(double x);
@JavaName('dart.math.DartMath.tan')
external double tan(double x);
@JavaName('dart.math.DartMath.asin')
external double asin(double x);
@JavaName('dart.math.DartMath.acos')
external double acos(double x);
@JavaName('dart.math.DartMath.atan')
external double atan(double x);
@JavaName('dart.math.DartMath.atan2')
external double atan2(double a, double b);
@JavaName('dart.math.DartMath.exp')
external double exp(double x);
@JavaName('dart.math.DartMath.log')
external double log(double x);

@JavaName('dart.math.DartMath.DartRandom')
class Random {
  external Random([int seed]);
  external int nextInt(int max);
  external double nextDouble();
  external bool nextBool();
}

// dart:core's `Iterator<E>` protocol. Given an explicit @JavaName so classes
// that `implements Iterator<E>` (e.g. the transformations demo's _BoardIterator)
// resolve it and the emitter imports `dart.collection.Iterator` rather than
// leaving a bare, unresolved `Iterator`. The dart:collection IterableMixin
// built-in returns this type from its `iterator` getter.
@JavaName('dart.collection.Iterator')
abstract class Iterator<E> {
  external bool moveNext();
  external E get current;
}

@JavaName('dart.math.DartPoint')
class Point<T> {
  external Point(double x, double y);
  external double get x;
  external double get y;
  external double distanceTo(Point<T> other);
}

// --- dart:intl -------------------------------------------------------------

// package:intl's `Intl`. Reached as `intl.Intl.xxx(...)`; the transpiler
// strips the `intl` import prefix and resolves `Intl` to this top-level type,
// so every member the app calls (canonicalizedLocale / pluralLogic / ...) must
// be STATIC — it is invoked as a static method on the class, never on an
// instance.
@JavaName('com.codename1.flutter.intl.Intl')
class Intl {
  external static String canonicalizedLocale(String aLocale);
  external static String pluralLogic(num howMany, {String locale, String zero, String one, String two, String few, String many, String other});
  external static String message(String messageText, {String desc, String locale, String name, Object args, String meaning});
  external static String plural(num howMany, {String locale, String zero, String one, String two, String few, String many, String other, String name, Object args});
  external static String select(Object choice, Object cases, {String locale, String name, Object args});
  external static String gender(String targetGender, {String female, String male, String other, String locale, String name, Object args});
  external static Object withLocale(String locale, Object function);
  external static String getCurrentLocale();
  external static String get defaultLocale;
}

// `intl` is the import-prefix stub (import 'package:intl/intl.dart' as intl;).
// Retained so a bare `intl.` prefix still resolves; `intl.Intl` itself now goes
// straight to the top-level `Intl` type above.
@JavaName('com.codename1.flutter.intl.IntlLib')
abstract class intl {
  external static Intl get Intl;
}

@JavaName('com.codename1.flutter.intl.DateFormat')
class DateFormat {
  external DateFormat([String pattern, String locale]);
  // Skeleton "field" constants used bare, e.g. DateFormat(DateFormat.WEEKDAY).
  external static String get WEEKDAY;
  external static String get MMM;
  external static DateFormat MMMd([String locale]);
  external static DateFormat jm([String locale]);
  external static DateFormat Hm([String locale]);
  external static DateFormat yMMM([String locale]);
  external static DateFormat yMMMMd([String locale]);
  external static DateFormat yMMMd([String locale]);
  external static DateFormat yMd([String locale]);
  external DateFormat add_jm();
  external DateFormat add_jms();
  external String format(DateTime date);
}

@JavaName('com.codename1.flutter.intl.NumberFormat')
class NumberFormat {
  external static NumberFormat currency({String locale, String symbol, int? decimalDigits, String name});
  external static NumberFormat decimalPercentPattern({String locale, int decimalDigits});
  external static NumberFormat simpleCurrency({String locale, String name, int? decimalDigits});
  external String format(dynamic number);
}

// --- fonts : GoogleFonts ---------------------------------------------------
// Named font accessors return a TextStyle; the *TextTheme accessors return a
// TextTheme. Extra named args (textStyle/fontStyle/letterSpacing) the app
// passes are dropped by the emitter since they are not declared here.

@JavaName('com.codename1.flutter.fonts.GoogleFonts')
abstract class GoogleFonts {
  external static GoogleFontsConfig get config;
  external static TextStyle eczar({double fontSize, FontWeight fontWeight, Color color, double? letterSpacing, double? height, TextStyle? textStyle, FontStyle? fontStyle, Object? decoration, double? wordSpacing});
  external static TextStyle libreFranklin({double fontSize, FontWeight fontWeight, Color color, double? letterSpacing, double? height, TextStyle? textStyle, FontStyle? fontStyle, Object? decoration, double? wordSpacing});
  external static TextStyle merriweather({double fontSize, FontWeight fontWeight, Color color, double? letterSpacing, double? height, TextStyle? textStyle, FontStyle? fontStyle, Object? decoration, double? wordSpacing});
  external static TextStyle montserrat({double fontSize, FontWeight fontWeight, Color color, double? letterSpacing, double? height, TextStyle? textStyle, FontStyle? fontStyle, Object? decoration, double? wordSpacing});
  external static TextStyle oswald({double fontSize, FontWeight fontWeight, Color color, double? letterSpacing, double? height, TextStyle? textStyle, FontStyle? fontStyle, Object? decoration, double? wordSpacing});
  external static TextStyle robotoCondensed({double fontSize, FontWeight fontWeight, Color color, double? letterSpacing, double? height, TextStyle? textStyle, FontStyle? fontStyle, Object? decoration, double? wordSpacing});
  external static TextStyle robotoMono({double fontSize, FontWeight fontWeight, Color color, double? letterSpacing, double? height, TextStyle? textStyle, FontStyle? fontStyle, Object? decoration, double? wordSpacing});
  external static TextStyle workSans({double fontSize, FontWeight fontWeight, Color color, double? letterSpacing, double? height, TextStyle? textStyle, FontStyle? fontStyle, Object? decoration, double? wordSpacing});
  external static TextTheme ralewayTextTheme([TextTheme textTheme]);
  external static TextTheme rubikTextTheme([TextTheme textTheme]);
  external static TextTheme workSansTextTheme([TextTheme textTheme]);
}

@JavaName('com.codename1.flutter.fonts.GoogleFontsConfig')
class GoogleFontsConfig {
  external bool get allowRuntimeFetching;
  external set allowRuntimeFetching(bool v);
}

// --- services : Clipboard / SystemChrome -----------------------------------

@JavaName('com.codename1.flutter.services.ClipboardData')
class ClipboardData {
  external ClipboardData({String text});
  external String get text;
}

@JavaName('com.codename1.flutter.services.Clipboard')
abstract class Clipboard {
  external static Future setData(ClipboardData data);
  external static Future getData(String format);
}

@JavaName('com.codename1.flutter.services.SystemUiOverlayStyle')
abstract class SystemUiOverlayStyle {
  external static SystemUiOverlayStyle get light;
  external static SystemUiOverlayStyle get dark;
}

@JavaName('com.codename1.flutter.services.SystemChrome')
abstract class SystemChrome {
  external static void setSystemUIOverlayStyle(SystemUiOverlayStyle style);
  external static void setPreferredOrientations(List<Object> orientations);
  external static void setEnabledSystemUIMode(Object mode);
}

// --- platform --------------------------------------------------------------

@JavaName('com.codename1.flutter.TargetPlatform')
enum TargetPlatform { android, fuchsia, iOS, linux, macOS, windows }

// --- widgets : Directionality ----------------------------------------------
// TextDirection enum is contributed by the coreWidgets stub set.

@JavaName('com.codename1.flutter.widgets.Directionality')
class Directionality extends Widget {
  external Directionality({Key? key, TextDirection textDirection, Widget child});
  // The ambient text direction — Flutter's `Directionality.of(context)`.
  external static TextDirection of(BuildContext context);
}

@JavaName('com.codename1.flutter.widgets.Debug.debugCheckHasDirectionality')
external bool debugCheckHasDirectionality(BuildContext context);

// --- l10n : MaterialLocalizations ------------------------------------------

@JavaName('com.codename1.flutter.l10n.LocalizationsDelegate')
class LocalizationsDelegate<T> {}

@JavaName('com.codename1.flutter.l10n.MaterialLocalizations')
abstract class MaterialLocalizations {
  external static MaterialLocalizations of(BuildContext context);
  external static LocalizationsDelegate get delegate;
  external String get backButtonTooltip;
  external String get closeButtonTooltip;
  external String get closeButtonLabel;
  external String get viewLicensesButtonLabel;
  external String get nextPageTooltip;
  external String get previousPageTooltip;
  external String get openAppDrawerTooltip;
}

@JavaName('com.codename1.flutter.l10n.GlobalMaterialLocalizations')
abstract class GlobalMaterialLocalizations {
  external static LocalizationsDelegate get delegate;
}

@JavaName('com.codename1.flutter.l10n.GlobalCupertinoLocalizations')
abstract class GlobalCupertinoLocalizations {
  external static LocalizationsDelegate get delegate;
}

@JavaName('com.codename1.flutter.l10n.GlobalWidgetsLocalizations')
abstract class GlobalWidgetsLocalizations {
  external static LocalizationsDelegate get delegate;
}

// RestorableDateTime is contributed by the restoration stub set.

// Upright or italic — Flutter's FontStyle. Fortnightly's masthead is the only place in the
// gallery that asks for italics, but a font call naming it must still resolve.
@JavaName('com.codename1.flutter.FontStyle')
enum FontStyle { normal, italic }
