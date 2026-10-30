// package:flutter_timezone, over java.util.TimeZone.

@JavaName('com.codename1.flutter.plugins.timezone.FlutterTimezone')
class FlutterTimezone {
  external static Future<String> getLocalTimezone();
  external static Future<List<String>> getAvailableTimezones();
}
