package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MarkerOptions;
import com.codename1.maps.NativeMap;
import com.codename1.maps.vector.BundledTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Verifies that {@link NativeMap} transparently falls back to the vector
/// {@link com.codename1.maps.MapView} when no native provider is wired in. The
/// fallback is configured with the bundled real San Francisco tiles so the
/// capture is deterministic, and a marker is added through the
/// {@code MapSurface} API to prove it routes to the fallback.
///
/// This is the complement of {@link NativeMapProviderScreenshotTest}: when a
/// native provider *is* active (e.g. an iOS build with
/// `ios.maps.provider=apple`) the fallback path is not exercised, so this test
/// skips and the provider test captures the native render instead.
public class NativeMapFallbackScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            // No committed watch golden; phone/tablet form factors cover this.
            System.out.println(
                    "CN1SS:INFO:test=NativeMapFallback status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        NativeMap map = new NativeMap(new LatLng(37.808, -122.412), 13,
                new BundledTileSource("/maptiles/{z}/{x}/{y}.mvt", true, 13, 13).setAttribution("(c) OSM"),
                MapStyle.light());
        if (map.isNativeMap()) {
            System.out.println(
                    "CN1SS:INFO:test=NativeMapFallback status=SKIPPED reason=native-provider-active");
            done();
            return true;
        }
        Form form = createForm("Native Map Fallback", new BorderLayout(), "NativeMapFallback");
        map.addMarker(new MarkerOptions(new LatLng(37.8087, -122.4098)).title("Pier 39"));
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
