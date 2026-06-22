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
public class RealOsmVectorScreenshotTest extends VectorMapScreenshotBaseTest {

    @Override
    public boolean runTest() {
        if (com.codename1.ui.CN.isWatch()) {
            // The watch form factor has no committed map goldens; the map
            // coverage runs on phone/tablet form factors instead.
            System.out.println(
                    "CN1SS:INFO:test=RealOsmVector status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        Form form = createForm("Real OSM Vector", new BorderLayout(), "RealOsmVector");
        MapView map = new MapView(
                new BundledTileSource("/maptiles/mt_{z}_{x}_{y}.mvt", true, 13, 13).setAttribution("(c) OSM"),
                MapStyle.light());
        map.moveCamera(new LatLng(37.814, -122.413), 13);
        form.add(BorderLayout.CENTER, map);
        mapUnderTest = map;
        form.show();
        return true;
    }
}
