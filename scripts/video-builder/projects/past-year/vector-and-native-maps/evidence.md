# Evidence map

Source: `docs/website/content/blog/vector-and-native-maps.md`
Canonical: https://www.codenameone.com/blog/vector-and-native-maps/

## Thesis

Choosing between owned vector rendering and injected native map providers

## Supported beats

- **What was wrong with the old maps:** For years the answer to "how do I show a map" was one of two things. The deprecated tile-based MapComponent fetched raster tiles and painted them, which worked but was frozen in time.
- **The MapView vector engine:** MapView is the part I find most interesting. It is a pure-vector map. Nothing about it is a native peer. Every road, water body, and park boundary is drawn through Graphics using GeneralPath and Stroke, the same drawing pipeline that paints every other CN1 component.
- **NativeMap and the build-hint provider model:** Sometimes you want the platform's map, with its road data, its traffic, and its look. That is what NativeMap is for. It embeds the actual platform map, Apple MapKit or Google Maps, behind the same MapSurface API.
- **The tradeoff:** The vector engine is new, and the style format is a subset of a full MapLibre style, not the whole specification. If you bring a complex upstream style it may reference layer or expression features the subset does not implement yet.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5264
- https://example.com/tiles/{z}/{x}/{y}.pbf

## Independent problem evidence

- Mapbox Vector Tile Specification: https://github.com/mapbox/vector-tile-spec — The vector-tile specification encodes tiled geometry and attributes so clients can choose how roads, water, labels, and boundaries are drawn.
- Apple MapKit: https://developer.apple.com/documentation/mapkit — Apple's MapKit provides platform map data, annotations, overlays, and native interaction behavior for Apple targets.

## Product proof

- `docs/website/static/blog/vector-and-native-maps/maps-vector.png`
- `docs/website/static/blog/vector-and-native-maps/maps-dark.png`
- `docs/website/static/blog/vector-and-native-maps/maps-markers.png`
- `docs/website/static/blog/vector-and-native-maps/maps-native.png`
