// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::maps-java-001[]
MapView map = new MapView();
map.moveCamera(new LatLng(37.7749, -122.4194), 12);
form.add(BorderLayout.CENTER, map);
// end::maps-java-001[]

// tag::maps-java-002[]
// Camera
map.setCameraPosition(new CameraPosition(new LatLng(48.8566, 2.3522), 11));
map.moveCamera(new LatLng(48.8566, 2.3522), 11);
map.setZoom(13);
map.fitBounds(new MapBounds(new LatLng(48.8, 2.2), new LatLng(48.9, 2.4)), 24);

// Markers
Marker m = map.addMarker(new MarkerOptions(new LatLng(48.8584, 2.2945))
        .icon(pinImage)
        .title("Eiffel Tower")
        .anchor(0.5f, 1.0f)
        .onClick(e -> showDetails()));
map.removeMarker(m);

// Shapes
map.addPolyline(new Polyline(routePoints).setStrokeColor(0xff5722).setStrokeWidth(6));
map.addPolygon(new Polygon(areaPoints).setFillColor(0x803f51b5).setStrokeColor(0x3f51b5));
map.addCircle(new Circle(new LatLng(48.85, 2.35), 500).setFillColor(0x804caf50));
map.clearMapObjects();

// Coordinate conversion and bounds
Point pixel = map.latLngToScreen(new LatLng(48.85, 2.35));
LatLng coord = map.screenToLatLng(120, 240);
MapBounds visible = map.getVisibleRegion();

// Events
map.addTapListener((surface, location, x, y) -> placeMarker(location));
map.addLongPressListener((surface, location, x, y) -> contextMenu(location));
map.addCameraChangeListener((surface, camera) -> persist(camera));
// end::maps-java-002[]

// tag::maps-java-003[]
// Keyless, zero-config vector basemap (OpenStreetMap data via OpenFreeMap):
MapView vector = new MapView(MvtTileSource.openFreeMap(), MapStyle.light());

// A keyed hosted provider (e.g. MapTiler). {z}/{x}/{y} are the XYZ tile
// coordinates and {key} is substituted from setApiKey(...):
MapView branded = new MapView(
        new MvtTileSource("https://api.maptiler.com/tiles/v3/{z}/{x}/{y}.pbf?key={key}", 0, 14)
                .setApiKey(apiKey),
        MapStyle.dark());

// A keyless raster (image-tile) basemap, if you don't need vector styling:
MapView raster = new MapView(RasterTileSource.openStreetMap());
// end::maps-java-003[]

// tag::maps-java-004[]
NativeMap map = new NativeMap(new LatLng(37.7749, -122.4194), 12);
map.addMarker(new MarkerOptions(new LatLng(37.7749, -122.4194)).title("San Francisco"));
form.add(BorderLayout.CENTER, map);

if (!map.isNativeMap()) {
    // Running on the simulator (or a build without a provider) -> vector fallback.
}
// end::maps-java-004[]

// tag::maps-java-005[]
NativeMap map = new NativeMap(new LatLng(0, 0), 4, fallbackTileSource, MapStyle.light());
// end::maps-java-005[]

// tag::maps-java-006[]
// Render Google Maps through its JavaScript SDK on any platform with a browser:
MapProviderRegistry.register(WebMapProvider.google("YOUR_MAPS_JS_API_KEY"));
NativeMap map = new NativeMap(new LatLng(41.0, 13.0), 5);
// end::maps-java-006[]

// tag::maps-java-007[]
MapProviderRegistry.setProviderOrder(new String[]{"google", "web", "vector"});
// end::maps-java-007[]
