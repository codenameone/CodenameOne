package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MarkerOptions;
import com.codename1.maps.NativeMap;
import com.codename1.maps.vector.DemoTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Verifies that {@link NativeMap} transparently falls back to the vector
/// {@link com.codename1.maps.MapView} when no native provider is wired in
/// (always the case on the simulator). The fallback is configured with the
/// offline demo tileset so the capture is deterministic, and a marker is added
/// through the {@code MapSurface} API to prove it routes to the fallback.
public class NativeMapFallbackScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        Form form = createForm("Native Map Fallback", new BorderLayout(), "NativeMapFallback");
        NativeMap map = new NativeMap(new LatLng(0, 0), 4, new DemoTileSource(), MapStyle.light());
        map.addMarker(new MarkerOptions(new LatLng(0, 0)).title("Here"));
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
