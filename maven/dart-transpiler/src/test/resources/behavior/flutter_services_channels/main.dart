// package:flutter/services.dart platform channels, compiled unchanged against
// the MethodChannel/EventChannel stubs and run against a HAND-WRITTEN Java
// platform side (Support.java, registered via support.stub.dart -- a
// fixture-only stub, not part of the product's runtime stub set).
//
// Covers both directions: Dart invoking a Java-registered handler (success,
// a platform error, and no handler at all), Java invoking a Dart-registered
// handler (a plain return value, an async one via a Future, a thrown
// PlatformException, and no handler at all), and an EventChannel stream
// delivered from the Java side with cancel -> onCancel.
import 'package:flutter/services.dart';

const methodChannelName = 'com.codenameone.test/method';
const noHandlerChannelName = 'com.codenameone.test/nohandler';
const eventChannelName = 'com.codenameone.test/events';

void main() async {
  // --- Dart invokes, Java answers ----------------------------------------
  Support.installJavaMethodHandler(methodChannelName);
  final channel = MethodChannel(methodChannelName);

  final echoed = await channel.invokeMethod('echo', 'hello');
  print('dart-invoke-success: $echoed');

  try {
    await channel.invokeMethod('fail', null);
    print('dart-invoke-error: did not throw');
  } on PlatformException catch (e) {
    print('dart-invoke-error: ${e.code} ${e.message} ${e.details}');
  }

  try {
    await MethodChannel(noHandlerChannelName).invokeMethod('x', null);
    print('dart-invoke-missing: did not throw');
  } on MissingPluginException catch (e) {
    print('dart-invoke-missing: ${e.message}');
  }

  // --- Java invokes, Dart answers -----------------------------------------
  channel.setMethodCallHandler((MethodCall call) {
    if (call.method == 'sync') {
      return 'sync:${call.arguments}';
    }
    if (call.method == 'boom') {
      throw PlatformException(code: 'E_DART', message: 'dart handler failed');
    }
    return null;
  });
  print('java-invoke-sync: ${Support.invokeJavaSideCall(methodChannelName, "sync", "v1")}');

  try {
    Support.invokeJavaSideCall(methodChannelName, 'boom', null);
    print('java-invoke-error: did not throw');
  } catch (e) {
    print('java-invoke-error: $e');
  }

  try {
    Support.invokeJavaSideCall(noHandlerChannelName, 'x', null);
    print('java-invoke-missing: did not throw');
  } catch (e) {
    print('java-invoke-missing: $e');
  }

  // A Dart handler that itself awaits -- an async reply, not a direct value.
  channel.setMethodCallHandler((MethodCall call) async {
    await Future.delayed(Duration(milliseconds: 1));
    return 'async:${call.arguments}';
  });
  print('java-invoke-async: ${Support.invokeJavaSideCall(methodChannelName, "any", "v2")}');

  channel.setMethodCallHandler(null);

  // --- EventChannel: Java pushes events, Dart listens --------------------
  Support.installJavaStreamHandler(eventChannelName);
  final events = <String>[];
  final sub = EventChannel(eventChannelName).receiveBroadcastStream().listen((event) {
    events.add('$event');
  });
  Support.fireEvent('tick1');
  Support.fireEvent('tick2');
  print('events: $events');
  await sub.cancel();
  print('cancelCount: ${Support.cancelCount()}');
}
