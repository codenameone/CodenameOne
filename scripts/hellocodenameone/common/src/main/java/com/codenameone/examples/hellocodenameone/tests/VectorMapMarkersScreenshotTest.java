package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.MarkerOptions;
import com.codename1.maps.vector.DemoTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Exercises marker overlays (the default pin rendering) on the vector map at
/// several geographic positions around the center.
public class VectorMapMarkersScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (com.codename1.ui.CN.isWatch()) {
            // The watch form factor has no committed map goldens; the map
            // coverage runs on phone/tablet form factors instead.
            System.out.println(
                    "CN1SS:INFO:test=VectorMapMarkers status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        Form form = createForm("Vector Map Markers", new BorderLayout(), "VectorMapMarkers");
        MapView map = new MapView(new DemoTileSource(), MapStyle.light());
        map.moveCamera(new LatLng(0, 0), 5);
        map.addMarker(new MarkerOptions(new LatLng(0, 0)).title("Center"));
        map.addMarker(new MarkerOptions(new LatLng(1.0, 1.0)).title("NE"));
        map.addMarker(new MarkerOptions(new LatLng(-1.0, -1.0)).title("SW"));
        map.addMarker(new MarkerOptions(new LatLng(1.0, -1.0)).title("NW"));
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
