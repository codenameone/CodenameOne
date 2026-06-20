package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.MarkerOptions;
import com.codename1.maps.vector.BundledTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Exercises marker overlays (the default Material map-pin rendering) on the
/// real San Francisco basemap at a few well-known waterfront landmarks.
public class VectorMapMarkersScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            // No committed watch golden; phone/tablet form factors cover this.
            System.out.println(
                    "CN1SS:INFO:test=VectorMapMarkers status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        Form form = createForm("Vector Map Markers", new BorderLayout(), "VectorMapMarkers");
        MapView map = new MapView(
                new BundledTileSource("/maptiles/mt_{z}_{x}_{y}.mvt", true, 13, 13).setAttribution("(c) OSM"),
                MapStyle.light());
        map.moveCamera(new LatLng(37.808, -122.412), 13);
        map.addMarker(new MarkerOptions(new LatLng(37.8087, -122.4098)).title("Pier 39"));
        map.addMarker(new MarkerOptions(new LatLng(37.8083, -122.4156)).title("Fisherman's Wharf"));
        map.addMarker(new MarkerOptions(new LatLng(37.8024, -122.4058)).title("Coit Tower"));
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
