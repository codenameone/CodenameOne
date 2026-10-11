// Codename One Flutter runtime API stubs -- package:flutter/services.dart
// platform channels (MethodChannel/EventChannel).
//
// MethodCall, PlatformException and MissingPluginException are the SAME
// Java class on both sides of a channel (@JavaName points straight at
// com.codename1.flutter.platform, the hand-written porting target that
// mirrors Android's io.flutter.plugin.common package): there is no
// serialize/deserialize boundary in-process, so the object a Dart
// invokeMethod() builds is literally the one a Java MethodCallHandler
// receives, and vice versa. See com.codename1.flutter.FlutterChannelRegistry
// for how the two sides are matched by channel name, and
// com.codename1.flutter.platform.MethodChannel's class comment for the
// porting story (implement MethodChannel.MethodCallHandler exactly like a
// classic Flutter plugin would, just importing com.codename1.flutter.platform
// instead of io.flutter.plugin.common).
//
// Same conventions as flutter_material.dart's header -- named constructor
// parameters become void setter methods (PlatformException's), positional
// ones become Java constructor arguments (MethodCall's, MissingPluginException's).
// setMethodCallHandler's handler is declared dynamic here for Dart-side
// looseness, but the Java method it resolves to (services.MethodChannel)
// is concretely typed Funcs.Func1<MethodCall, Object> -- the transpiler
// emits every closure literal as a bare, untyped Java lambda, so the REAL
// Java parameter must be a genuine functional interface (never bare
// Object) for the generated call to compile at all.

@JavaName('com.codename1.flutter.platform.MethodCall')
class MethodCall {
  external MethodCall(String method, [Object? arguments]);
  external String get method;
  external dynamic get arguments;
}

@JavaName('com.codename1.flutter.platform.PlatformException')
class PlatformException {
  external PlatformException({required String code, String? message, Object? details, String? stacktrace});
  external String get code;
  external String? get message;
  external dynamic get details;
  external String? get stacktrace;
}

@JavaName('com.codename1.flutter.platform.MissingPluginException')
class MissingPluginException {
  external MissingPluginException([String? message]);
  external String? get message;
}

@JavaName('com.codename1.flutter.services.MethodChannel')
class MethodChannel {
  external MethodChannel(String name, [Object? codec]);
  external Future<dynamic> invokeMethod(String method, [Object? arguments]);
  external Future<List<dynamic>> invokeListMethod(String method, [Object? arguments]);
  external Future<Map<dynamic, dynamic>> invokeMapMethod(String method, [Object? arguments]);
  external void setMethodCallHandler(Object? handler);
}

@JavaName('com.codename1.flutter.services.EventChannel')
class EventChannel {
  external EventChannel(String name, [Object? codec]);
  external Stream<dynamic> receiveBroadcastStream([Object? arguments]);
}
