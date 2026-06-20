package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.vector.DemoTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Renders the vector {@link MapView} with the built-in dark style so the
/// style engine (background + per-layer colors) has a baseline distinct from
/// the light basemap.
public class VectorMapDarkStyleScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (com.codename1.ui.CN.isWatch()) {
            // The watch form factor has no committed map goldens; the map
            // coverage runs on phone/tablet form factors instead.
            System.out.println(
                    "CN1SS:INFO:test=VectorMapDarkStyle status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        Form form = createForm("Vector Map Dark", new BorderLayout(), "VectorMapDarkStyle");
        MapView map = new MapView(new DemoTileSource(), MapStyle.dark());
        map.moveCamera(new LatLng(0, 0), 4);
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
