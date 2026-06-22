/// The pure-vector map rendering engine that backs
/// [com.codename1.maps.MapView].
///
/// The engine decodes Mapbox Vector Tiles (MVT) with the framework protobuf
/// reader and draws them entirely through the Codename One [com.codename1.ui.Graphics]
/// API -- there is no native peer, so a [com.codename1.maps.MapView] composes
/// with regular lightweight UI on every platform including the simulator and
/// the browser. Tiles are supplied by a pluggable
/// [com.codename1.maps.vector.TileSource] (network MVT or raster, bundled
/// fixtures, or the keyless OpenFreeMap basemap) and styled with a
/// [com.codename1.maps.vector.MapStyle] (a subset of the MapLibre GL style
/// specification). The classes in this package are internal building blocks of
/// the engine; application code targets [com.codename1.maps.MapView] and the
/// public tile-source and style types.
package com.codename1.maps.vector;
