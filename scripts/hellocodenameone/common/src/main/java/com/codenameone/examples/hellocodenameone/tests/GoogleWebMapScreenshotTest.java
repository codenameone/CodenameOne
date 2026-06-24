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
/// Like the (now-removed) native Apple provider test it frames the Italian peninsula
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
        // tvOS ships no WebKit (BrowserComponent is compiled out of the tvOS
        // slice), so the web map has no peer to render into -- skip rather than
        // baseline a blank/black capture, exactly as on the watch form factor.
        if (com.codename1.ui.CN.isTV()) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=tv-no-webview");
            done();
            return true;
        }
        // The Mac native (Catalyst) desktop backend renders into a Core Graphics
        // bitmap that does not composite the native web view, so the capture is
        // solid black there. The web map renders + captures fine on the mobile
        // ports (iOS/Android), which hold the goldens; skip the desktop runner.
        if (com.codename1.ui.CN.isDesktop()) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=webview-not-captured-on-desktop");
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
        MapProviderRegistry.setProviderOrder(new String[]{"web"});
        final NativeMap map = new NativeMap(new LatLng(41.0, 13.0), 5);
        MapProviderRegistry.setProviderOrder(null);
        if (!map.isNativeMap()) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=no-web-peer");
            done();
            return true;
        }
        // Build the form by hand (rather than createForm) so we control what
        // happens AFTER the capture: the Google Maps JS keeps a continuous
        // requestAnimationFrame / tile-loading loop alive for the life of the
        // page. If left running it starves the iOS main thread for the rest of
        // the suite and desyncs later tests' capture timing (a downstream test
        // then screenshots a stale form). So once we have our shot we dispose
        // the web view, which blanks the page and stops that loop.
        Form form = new Form("Google Web Map", new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                final Form self = this;
                // Let the Google Maps JS load and paint tiles from the network
                // (well over the native-map settle), capture, then dispose.
                UITimer.timer(8000, false, self, () ->
                        captureWhenSettled(self, "GoogleWebMap", () -> {
                            map.dispose();
                            done();
                        }));
            }
        };
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
}
