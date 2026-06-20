package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.vector.DemoTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Renders the pure-vector {@link MapView} against the offline demo tileset
/// with the light style. Deterministic (no network), so it produces a stable
/// screenshot baseline for the vector renderer.
public class VectorMapBasemapScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (com.codename1.ui.CN.isWatch()) {
            // The watch form factor has no committed map goldens; the map
            // coverage runs on phone/tablet form factors instead.
            System.out.println(
                    "CN1SS:INFO:test=VectorMapBasemap status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        Form form = createForm("Vector Map", new BorderLayout(), "VectorMapBasemap");
        MapView map = new MapView(new DemoTileSource(), MapStyle.light());
        map.moveCamera(new LatLng(0, 0), 4);
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
