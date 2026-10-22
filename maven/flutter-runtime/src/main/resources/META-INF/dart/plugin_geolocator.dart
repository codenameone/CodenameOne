// package:geolocator, over LocationManager. Permission is the platform's own
// prompt, raised on first use; distanceBetween/bearingBetween are computed here.

@JavaName('com.codename1.flutter.plugins.geolocator.LocationPermission')
enum LocationPermission { denied, deniedForever, whileInUse, always, unableToDetermine }

@JavaName('com.codename1.flutter.plugins.geolocator.LocationAccuracy')
enum LocationAccuracy { lowest, low, medium, high, best, bestForNavigation, reduced }

@JavaName('com.codename1.flutter.plugins.geolocator.LocationSettings')
class LocationSettings {
  external LocationSettings({LocationAccuracy accuracy, int distanceFilter, Duration? timeLimit});
}

@JavaName('com.codename1.flutter.plugins.geolocator.LocationSettings')
class AndroidSettings extends LocationSettings {
  external AndroidSettings({LocationAccuracy accuracy, int distanceFilter, Duration? timeLimit});
}

@JavaName('com.codename1.flutter.plugins.geolocator.LocationSettings')
class AppleSettings extends LocationSettings {
  external AppleSettings({LocationAccuracy accuracy, int distanceFilter, Duration? timeLimit});
}

@JavaName('com.codename1.flutter.plugins.geolocator.Position')
class Position {
  external double get latitude;
  external double get longitude;
  external DateTime get timestamp;
  external double get accuracy;
  external double get altitude;
  external double get altitudeAccuracy;
  external double get heading;
  external double get headingAccuracy;
  external double get speed;
  external double get speedAccuracy;
  external bool get isMocked;
  external Map<String, dynamic> toJson();
}

@JavaName('com.codename1.flutter.plugins.geolocator.LocationServiceDisabledException')
class LocationServiceDisabledException implements Exception {
  external LocationServiceDisabledException();
}

@JavaName('com.codename1.flutter.plugins.geolocator.PermissionDeniedException')
class PermissionDeniedException implements Exception {
  external PermissionDeniedException(String? message);
}

@JavaName('com.codename1.flutter.plugins.geolocator.Geolocator')
class Geolocator {
  external static Future<bool> isLocationServiceEnabled();
  external static Future<LocationPermission> checkPermission();
  external static Future<LocationPermission> requestPermission();
  external static Future<Position> getCurrentPosition({LocationAccuracy desiredAccuracy,
      bool forceAndroidLocationManager, Duration? timeLimit, LocationSettings? locationSettings});
  external static Future<Position?> getLastKnownPosition({bool forceAndroidLocationManager});
  external static Stream<Position> getPositionStream({LocationSettings? locationSettings});
  external static Future<bool> openAppSettings();
  external static Future<bool> openLocationSettings();
  external static double distanceBetween(double startLatitude, double startLongitude,
      double endLatitude, double endLongitude);
  external static double bearingBetween(double startLatitude, double startLongitude,
      double endLatitude, double endLongitude);
}
