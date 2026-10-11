// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.
//
// The Dart side of the same channels FlutterInteropSnippets.java answers
// from Java -- package:flutter/services.dart, compiled unchanged.

import 'package:flutter/services.dart';

// tag::flutter-interop-dart-001[]
// Calls the Java handler registered with MethodChannel.setMethodCallHandler
// on the platform side (FlutterInteropSnippets.answerDartWithAJavaHandler).
final batteryChannel = MethodChannel('com.example.app/battery');

Future<int> getBatteryLevel() async {
  final level = await batteryChannel.invokeMethod('getBatteryLevel');
  return level as int;
}
// end::flutter-interop-dart-001[]

// tag::flutter-interop-dart-002[]
// Answers calls the Java side makes with MethodChannel.invokeMethod
// (FlutterInteropSnippets.invokeADartRegisteredHandler).
void installBatteryRefreshHandler() {
  batteryChannel.setMethodCallHandler((MethodCall call) async {
    if (call.method == 'refreshBatteryLevel') {
      return getBatteryLevel();
    }
    throw MissingPluginException();
  });
}
// end::flutter-interop-dart-002[]

// tag::flutter-interop-dart-003[]
// Listens to events the Java side pushes through an EventSink
// (FlutterInteropSnippets.pushEventsFromAJavaStreamHandler).
final sensorChannel = EventChannel('com.example.app/sensor');

StreamSubscription<dynamic> listenToSensorReadings(void Function(double value) onReading) {
  return sensorChannel.receiveBroadcastStream().listen((event) {
    onReading(event as double);
  });
}
// end::flutter-interop-dart-003[]
