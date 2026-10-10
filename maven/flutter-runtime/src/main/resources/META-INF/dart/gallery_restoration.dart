// Codename One Flutter runtime API stubs — state restoration (M2, new_gallery).
//
// Signature-only declarations for Flutter's state-restoration framework as seen
// from Dart. The hand-written Java runtime lives under com.codename1.flutter.*
// (see @JavaName on each declaration). Restoration is a no-op-but-API-complete
// implementation: Restorable* properties hold their value in a field and
// registerForRestoration merely wires the property; nothing is persisted.
//
// This file is loaded alongside flutter_material.dart by StubRegistry.

// --- the RestorationMixin -------------------------------------------------

// Mixed into a State subclass (`class _FooState extends State<Foo> with
// RestorationMixin`). The emitter maps this to a Java interface with default
// methods; bare calls to registerForRestoration resolve to the default method.
@JavaName('com.codename1.flutter.RestorationMixin')
mixin RestorationMixin {
  external String? get restorationId;
  external RestorationBucket? get bucket;
  external void restoreState(RestorationBucket? oldBucket, bool initialRestore);
  external void registerForRestoration(RestorableProperty property, String restorationId);
  external void unregisterFromRestoration(RestorableProperty property);
  external void didToggleBucket(RestorationBucket? oldBucket);
}

// --- the restoration bucket (opaque token) --------------------------------

@JavaName('com.codename1.flutter.RestorationBucket')
class RestorationBucket {}

// --- RestorableProperty and its value-holding subtypes --------------------

// The abstract base. User code subclasses this directly (e.g. to restore a
// Set<int>), overriding createDefaultValue / fromPrimitives / toPrimitives /
// initWithValue and calling notifyListeners(). It extends ChangeNotifier in
// Flutter; the listener plumbing is folded in here for the restoration scope.
@JavaName('com.codename1.flutter.RestorableProperty')
abstract class RestorableProperty<T> {
  external RestorableProperty();
  external T createDefaultValue();
  external void initWithValue(T value);
  external Object toPrimitives();
  external T fromPrimitives(Object? data);
  external bool get isRegistered;
  external void notifyListeners();
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void dispose();
}

// Abstract value-holding bases. Flutter layers these between RestorableProperty
// and the concrete holders; user code in new_gallery subclasses them directly
// (studies/reply/app.dart, studies/shrine/app.dart) and reads the inherited
// `value` getter, so they must exist for member/identifier resolution.

// A restorable that stores a single value with a read/write `value` accessor —
// Flutter's `RestorableValue<T>`.
@JavaName('com.codename1.flutter.RestorableValue')
abstract class RestorableValue<T> extends RestorableProperty<T> {
  external T get value;
  external set value(T newValue);
}

// A restorable whose value is a Listenable that is itself restored (rather than
// re-created) — Flutter's `RestorableListenable<T extends Listenable>`. The
// value getter is read-only; subclasses override createDefaultValue /
// fromPrimitives / toPrimitives.
@JavaName('com.codename1.flutter.RestorableListenable')
abstract class RestorableListenable<T> extends RestorableProperty<T> {
  external T get value;
}

// A RestorableListenable specialised for ChangeNotifier values that also
// disposes the held notifier — Flutter's `RestorableChangeNotifier<T>`.
@JavaName('com.codename1.flutter.RestorableChangeNotifier')
abstract class RestorableChangeNotifier<T> extends RestorableListenable<T> {}

// Concrete value holders. Each exposes a typed `value` getter/setter that the
// emitter routes to the overloaded Java accessors value()/value(v).

@JavaName('com.codename1.flutter.RestorableBool')
class RestorableBool extends RestorableProperty<bool> {
  external RestorableBool(bool defaultValue);
  external bool get value;
  external set value(bool v);
}

@JavaName('com.codename1.flutter.RestorableBoolN')
class RestorableBoolN extends RestorableProperty<bool> {
  external RestorableBoolN(bool? defaultValue);
  external bool? get value;
  external set value(bool? v);
}

@JavaName('com.codename1.flutter.RestorableInt')
class RestorableInt extends RestorableProperty<int> {
  external RestorableInt(int defaultValue);
  external int get value;
  external set value(int v);
}

@JavaName('com.codename1.flutter.RestorableIntN')
class RestorableIntN extends RestorableProperty<int> {
  external RestorableIntN(int? defaultValue);
  external int? get value;
  external set value(int? v);
}

@JavaName('com.codename1.flutter.RestorableDouble')
class RestorableDouble extends RestorableProperty<double> {
  external RestorableDouble(double defaultValue);
  external double get value;
  external set value(double v);
}

@JavaName('com.codename1.flutter.RestorableDoubleN')
class RestorableDoubleN extends RestorableProperty<double> {
  external RestorableDoubleN(double? defaultValue);
  external double? get value;
  external set value(double? v);
}

@JavaName('com.codename1.flutter.RestorableString')
class RestorableString extends RestorableProperty<String> {
  external RestorableString(String defaultValue);
  external String get value;
  external set value(String v);
}

@JavaName('com.codename1.flutter.RestorableStringN')
class RestorableStringN extends RestorableProperty<String> {
  external RestorableStringN(String? defaultValue);
  external String? get value;
  external set value(String? v);
}

// value type is DateTime; modelled as Object here because dart:core DateTime is
// owned by a different category. The .value member still resolves as Object.
@JavaName('com.codename1.flutter.RestorableDateTime')
class RestorableDateTime extends RestorableValue<DateTime> {
  external RestorableDateTime(DateTime defaultValue);
  external DateTime get value;
  external set value(DateTime v);
}

@JavaName('com.codename1.flutter.RestorableTextEditingController')
class RestorableTextEditingController extends RestorableProperty<TextEditingController> {
  external RestorableTextEditingController({String? text});
  external TextEditingController get value;
}

// --- global keys and focus nodes ------------------------------------------

@JavaName('com.codename1.flutter.GlobalKey')
class GlobalKey<T> extends Key {
  external GlobalKey({String? debugLabel});
  external T? get currentState;
  external BuildContext? get currentContext;
  external Widget? get currentWidget;
}

@JavaName('com.codename1.flutter.FocusNode')
class FocusNode {
  external FocusNode({String? debugLabel, bool? skipTraversal, bool? canRequestFocus});
  external bool get hasFocus;
  external bool get hasPrimaryFocus;
  external void requestFocus([FocusNode? node]);
  external void unfocus();
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void dispose();
}

// --- restorable route future (route restoration) --------------------------

// The navigator handle passed to RestorableRouteFuture.onPresent. The Navigator
// static helpers (Navigator.of / Navigator.restorablePush) are owned by the
// navigation category; only the NavigatorState surface the callbacks touch is
// declared here.
@JavaName('com.codename1.flutter.navigation.NavigatorState')
abstract class NavigatorState {
  external String restorablePush(Object routeBuilder, {Object? arguments});
  external String restorablePushNamed(String routeName, {Object? arguments});
  external void pop([Object? result]);
  // Imperative navigation used across new_gallery — Flutter's `NavigatorState`
  // push / pushNamed / popUntil / canPop / maybePop.
  external Future<T> push<T>(Route<T> route);
  external Future<T> pushNamed<T>(String routeName, {Object? arguments});
  external Future<T> pushReplacement<T>(Route<T> newRoute, {Object? result});
  external Future<T> pushReplacementNamed<T>(String routeName, {Object? arguments, Object? result});
  external void popUntil(Object predicate);
  external bool canPop();
  external Future<bool> maybePop([Object? result]);
}

@JavaName('com.codename1.flutter.navigation.RestorableRouteFuture')
class RestorableRouteFuture<T> extends RestorableProperty<Object> {
  external RestorableRouteFuture({RoutePresentationCallback onPresent, DynamicCallback? onComplete});
  external void present([Object? arguments]);
  external bool get isPresent;
  external String? get route;
}
