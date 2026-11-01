// package:geocoding, over Google's Geocoding REST API with the key in the Display
// property google.geocoding.key; no key, no display or no match fails with
// NoResultFoundException.

@JavaName('com.codename1.flutter.plugins.geocoding.Location')
class Location {
  external Location({required double latitude, required double longitude, required DateTime timestamp});
  external double get latitude;
  external double get longitude;
  external DateTime get timestamp;
}

@JavaName('com.codename1.flutter.plugins.geocoding.Placemark')
class Placemark {
  external Placemark({String? name, String? street, String? isoCountryCode, String? country,
      String? postalCode, String? administrativeArea, String? subAdministrativeArea, String? locality,
      String? subLocality, String? thoroughfare, String? subThoroughfare});
  external String? get name;
  external String? get street;
  external String? get isoCountryCode;
  external String? get country;
  external String? get postalCode;
  external String? get administrativeArea;
  external String? get subAdministrativeArea;
  external String? get locality;
  external String? get subLocality;
  external String? get thoroughfare;
  external String? get subThoroughfare;
}

@JavaName('com.codename1.flutter.plugins.geocoding.NoResultFoundException')
class NoResultFoundException implements Exception {
  external NoResultFoundException();
}

@JavaName('com.codename1.flutter.plugins.geocoding.Geocoding.placemarkFromCoordinates')
external Future<List<Placemark>> placemarkFromCoordinates(double latitude, double longitude);

@JavaName('com.codename1.flutter.plugins.geocoding.Geocoding.locationFromAddress')
external Future<List<Location>> locationFromAddress(String address);

@JavaName('com.codename1.flutter.plugins.geocoding.Geocoding.setLocaleIdentifier')
external Future<void> setLocaleIdentifier(String localeIdentifier);

@JavaName('com.codename1.flutter.plugins.geocoding.Geocoding.isPresent')
external Future<bool> isPresent();
