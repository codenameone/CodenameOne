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
                // Wait for the live Google Maps SDK to actually paint its tiles
                // (NativeMap.isMapReady() reflects the map's 'tilesloaded' event)
                // before capturing, rather than guessing a fixed delay -- a slow
                // CI runner otherwise snapped a blank grey frame before any tile
                // loaded (the flake that previously failed this test). Poll up to
                // ~22s (the load headroom the old blind wait needed on the
                // slowest runner; still under the 30s native-test timeout). The
                // readiness check is a cheap Java flag (set once by an in-page
                // JS->Java bridge), so polling is free; we capture as soon as the
                // map paints. If the tiles never load (blocked network / rejected
                // key) fail loudly instead of baselining a blank map.
                waitForMapReady(self, map, 44, () ->
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

    /// Poll until the live web map reports its tiles painted (map.isMapReady()),
    /// then run onReady. Captures as soon as the real map has rendered, so a fast
    /// runner finishes quickly; the attempt budget (500ms * attempts) is the
    /// worst-case deadline. On timeout we deliberately do NOT capture -- a blank
    /// or blocked map should fail loudly (the harness records missing_actual)
    /// rather than be baselined.
    private void waitForMapReady(final Form form, final NativeMap map,
                                 final int attemptsLeft, final Runnable onReady) {
        boolean ready;
        try {
            ready = map.isMapReady();
        } catch (Throwable t) {
            ready = false;
        }
        if (ready) {
            // One short extra beat so the last tiles' compositing settles before
            // the capture (the same peer-compositing lag extraSettle covers).
            UITimer.timer(1000, false, form, onReady);
            return;
        }
        if (attemptsLeft <= 0) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=FAILED reason=tiles-never-loaded");
            done();
            return;
        }
        UITimer.timer(500, false, form, () ->
                waitForMapReady(form, map, attemptsLeft - 1, onReady));
    }

    /// The map is a native peer (BrowserComponent) view. On the iOS Metal
    /// backend the screenshot has been seen to capture solid black -- the
    /// peer's web-view layer not composited into the captured frame in time
    /// (the same reason the desktop/Mac-native runner is skipped above, though
    /// iOS does composite it most of the time). Force a repaint and a short
    /// extra present window before the capture so the composited frame includes
    /// the loaded map. Opt-in hook; default is 0 for every other test.
    @Override
    protected long extraSettleBeforeCaptureMillis() {
        return 1200;
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
