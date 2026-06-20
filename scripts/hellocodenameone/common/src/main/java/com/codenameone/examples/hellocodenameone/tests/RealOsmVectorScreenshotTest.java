package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.vector.BundledTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Renders the pure-vector {@link MapView} against *real* OpenStreetMap vector
/// tiles (a bundled San Francisco fixture downloaded from the keyless
/// OpenFreeMap basemap), proving the engine maps real OSM data -- streets,
/// water, parks, buildings and place labels. Deterministic and offline (the
/// tiles are shipped as resources), so it produces a stable screenshot baseline.
public class RealOsmVectorScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        Form form = createForm("Real OSM Vector", new BorderLayout(), "RealOsmVector");
        MapView map = new MapView(
                new BundledTileSource("/maptiles/{z}/{x}/{y}.mvt", true, 13, 13).setAttribution("(c) OSM"),
                MapStyle.light());
        map.moveCamera(new LatLng(37.814, -122.413), 13);
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
