// package:google_maps_flutter, hosting Codename One's MapView: native maps where
// the platform has them, vector tiles elsewhere. Markers, the camera, taps and
// camera moves work; polylines, polygons, circles and styles are accepted and not
// drawn, and markers keep the default pin.

@JavaName('com.codename1.flutter.plugins.googlemaps.LatLng')
class LatLng {
  external LatLng(double latitude, double longitude);
  external double get latitude;
  external double get longitude;
}

@JavaName('com.codename1.flutter.plugins.googlemaps.CameraPosition')
class CameraPosition {
  external CameraPosition({required LatLng target, double zoom, double bearing, double tilt});
  external LatLng get target;
  external double get zoom;
  external double get bearing;
  external double get tilt;
}

@JavaName('com.codename1.flutter.plugins.googlemaps.CameraUpdate')
class CameraUpdate {
  external static CameraUpdate newLatLng(LatLng latLng);
  external static CameraUpdate newLatLngZoom(LatLng latLng, double zoom);
  external static CameraUpdate newCameraPosition(CameraPosition cameraPosition);
  external static CameraUpdate zoomIn();
  external static CameraUpdate zoomOut();
  external static CameraUpdate zoomTo(double zoom);
  external static CameraUpdate zoomBy(double amount, [Offset? focus]);
}

@JavaName('com.codename1.flutter.plugins.googlemaps.MarkerId')
class MarkerId {
  external MarkerId(String value);
  external String get value;
}

@JavaName('com.codename1.flutter.plugins.googlemaps.InfoWindow')
class InfoWindow {
  external InfoWindow({String? title, String? snippet, void Function()? onTap});
  external String? get title;
  external String? get snippet;
}

@JavaName('com.codename1.flutter.plugins.googlemaps.BitmapDescriptor')
class BitmapDescriptor {
  external static BitmapDescriptor get defaultMarker;
  external static BitmapDescriptor defaultMarkerWithHue(double hue);
  external static double get hueRed;
  external static double get hueOrange;
  external static double get hueYellow;
  external static double get hueGreen;
  external static double get hueCyan;
  external static double get hueAzure;
  external static double get hueBlue;
  external static double get hueViolet;
  external static double get hueMagenta;
  external static double get hueRose;
}

@JavaName('com.codename1.flutter.plugins.googlemaps.Marker')
class Marker {
  external Marker({required MarkerId markerId, LatLng position, InfoWindow infoWindow, bool draggable,
      bool visible, void Function()? onTap, BitmapDescriptor icon, double alpha, double rotation,
      double zIndex, void Function(LatLng value)? onDragEnd});
  external MarkerId get markerId;
  external LatLng get position;
  external InfoWindow get infoWindow;
  external bool get draggable;
  external bool get visible;
}

@JavaName('com.codename1.flutter.plugins.googlemaps.MapType')
enum MapType { none, normal, satellite, terrain, hybrid }

@JavaName('com.codename1.flutter.plugins.googlemaps.GoogleMapController')
class GoogleMapController {
  external Future<void> animateCamera(CameraUpdate cameraUpdate, {Duration? duration});
  external Future<void> moveCamera(CameraUpdate cameraUpdate);
  external Future<double> getZoomLevel();
  external Future<void> showMarkerInfoWindow(MarkerId markerId);
  external Future<void> hideMarkerInfoWindow(MarkerId markerId);
  external Future<void> setMapStyle(String? mapStyle);
  external void dispose();
}

@JavaName('com.codename1.flutter.plugins.googlemaps.GoogleMap')
class GoogleMap extends Widget {
  external GoogleMap({Key? key, required CameraPosition initialCameraPosition, Set<Marker> markers,
      MapType mapType, void Function(GoogleMapController controller)? onMapCreated,
      void Function(LatLng argument)? onTap, void Function(LatLng argument)? onLongPress,
      void Function(CameraPosition position)? onCameraMove, void Function()? onCameraIdle,
      Object? polylines, Object? polygons, Object? circles, bool myLocationEnabled,
      bool myLocationButtonEnabled, bool zoomControlsEnabled, bool zoomGesturesEnabled,
      bool scrollGesturesEnabled, bool rotateGesturesEnabled, bool tiltGesturesEnabled,
      bool compassEnabled, bool mapToolbarEnabled, bool trafficEnabled, bool buildingsEnabled,
      bool liteModeEnabled, EdgeInsets padding, Object? minMaxZoomPreference,
      Object? gestureRecognizers, String? style});
}
