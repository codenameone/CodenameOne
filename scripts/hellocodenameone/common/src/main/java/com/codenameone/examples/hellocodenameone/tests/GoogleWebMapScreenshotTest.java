package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Util;
import com.codename1.maps.LatLng;
import com.codename1.maps.NativeMap;
import com.codename1.maps.WebMapProvider;
import com.codename1.maps.spi.MapProviderRegistry;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

import java.io.InputStream;

/// Visual confirmation that the cross-platform web map provider
/// ({@link com.codename1.maps.WebMapProvider}, Google Maps JavaScript SDK in a
/// {@link com.codename1.ui.BrowserComponent}) renders. This is the path that
/// lets *any* platform show Google Maps -- including the ones with no native
/// Google SDK -- so it doubles as the cross-platform fallback smoke test.
///
/// Like {@link NativeMapProviderScreenshotTest} it frames the Italian peninsula
/// at a regional zoom (stable geography, strong land/water contrast, no
/// street-level churn) and uses a lenient `.tolerance` so live tile/label noise
/// does not fail CI while a blocked/blank map still does.
///
/// The Google JavaScript API needs a key. CI writes the `GOOGLE_MAPS_API_KEY`
/// secret to `google-maps-key.txt` on the classpath before the build; when that
/// resource is absent (local builds, forks, no secret) the test skips rather
/// than baseline an error overlay.
public class GoogleWebMapScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (com.codename1.ui.CN.isWatch()) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        String key = readKey();
        if (key == null || key.length() == 0) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=no-api-key");
            done();
            return true;
        }
        // Register the web Google provider and force it for this map only; the
        // provider was resolved by the NativeMap constructor synchronously, so
        // restoring the default order right after leaves later tests (e.g. the
        // native Apple provider test) untouched.
        MapProviderRegistry.register(WebMapProvider.google(key));
        MapProviderRegistry.setProviderOrder(new String[]{"google"});
        NativeMap map = new NativeMap(new LatLng(41.0, 13.0), 5);
        MapProviderRegistry.setProviderOrder(null);
        if (!map.isNativeMap()) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=no-web-peer");
            done();
            return true;
        }
        Form form = createForm("Google Web Map", new BorderLayout(), "GoogleWebMap");
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }

    private String readKey() {
        try {
            InputStream is = Display.getInstance().getResourceAsStream(
                    getClass(), "/google-maps-key.txt");
            if (is == null) {
                return null;
            }
            try {
                byte[] data = Util.readInputStream(is);
                return new String(data, "UTF-8").trim();
            } finally {
                is.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        // The page loads the Google Maps JS from the network and then paints
        // tiles asynchronously; give it well over the native-map settle so the
        // imagery is present before the capture.
        UITimer.timer(8000, false, parent, run);
    }
}
