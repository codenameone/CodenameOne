package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.vector.BundledTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Renders the bundled real San Francisco OSM tiles with the built-in dark
/// style, exercising the style engine (background + per-layer colors) on real
/// data so it is distinct from the light basemap.
public class VectorMapDarkStyleScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            // No committed watch golden; phone/tablet form factors cover this.
            System.out.println(
                    "CN1SS:INFO:test=VectorMapDarkStyle status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        Form form = createForm("Vector Map Dark", new BorderLayout(), "VectorMapDarkStyle");
        MapView map = new MapView(
                new BundledTileSource("/maptiles/mt_{z}_{x}_{y}.mvt", true, 13, 13).setAttribution("(c) OSM"),
                MapStyle.dark());
        map.moveCamera(new LatLng(37.808, -122.412), 13);
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
