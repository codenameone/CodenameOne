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

    /// Attempts per wait (500ms each): 12s. A healthy load fires tilesloaded in
    /// 3-8s; the pathological case is a COLD WebKit on a starved runner where the
    /// page load alone ate ~21s of the old single 22s budget (observed on the
    /// metal leg: MainFrameLoadCompleted at T+21s -> tiles never loaded -> missing
    /// screenshot). Two 12s attempts with a fresh web view beat one 22s attempt:
    /// by the retry WebKit's processes and the SDK script cache are warm, so the
    /// reload takes seconds.
    private static final int WAIT_ATTEMPTS = 24;

    /// One-shot in-test retry consumed (see waitForMapReady timeout branch).
    private boolean retriedOnce;

    private String apiKey;
    private NativeMap currentMap;

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
        apiKey = readKey();
        if (apiKey == null || apiKey.length() == 0) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=no-api-key");
            done();
            return true;
        }
        final NativeMap map = buildWebMap();
        if (!map.isNativeMap()) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=no-web-peer");
            done();
            return true;
        }
        currentMap = map;
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
                // Wait for the live Google Maps SDK to actually paint its tiles
                // (NativeMap.isMapReady() reflects the map's 'tilesloaded' event)
                // before capturing, rather than guessing a fixed delay -- a slow
                // CI runner otherwise snapped a blank grey frame before any tile
                // loaded (the flake that previously failed this test). The
                // readiness check is a cheap Java flag (set once by an in-page
                // JS->Java bridge), so polling is free; we capture as soon as the
                // map paints. If the tiles never load, retry once with a FRESH
                // web view (see waitForMapReady) before failing loudly.
                waitForMapReady(this, WAIT_ATTEMPTS, () -> captureAndFinish(this));
            }
        };
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }

    /// Registers the web Google provider and builds a NativeMap forced onto it;
    /// the provider is resolved by the NativeMap constructor synchronously, so
    /// restoring the default order right after leaves later tests (e.g. the
    /// native Apple provider test) untouched. Reused by the in-test retry.
    private NativeMap buildWebMap() {
        MapProviderRegistry.register(WebMapProvider.google(apiKey));
        MapProviderRegistry.setProviderOrder(new String[]{"web"});
        NativeMap map = new NativeMap(new LatLng(41.0, 13.0), 5);
        MapProviderRegistry.setProviderOrder(null);
        return map;
    }

    private void captureAndFinish(Form form) {
        captureWhenSettled(form, "GoogleWebMap", () -> {
            currentMap.dispose();
            done();
        });
    }

    /// Poll until the live web map reports its tiles painted (map.isMapReady()),
    /// then run onReady. Captures as soon as the real map has rendered, so a fast
    /// runner finishes quickly; the attempt budget (500ms * attempts) is the
    /// per-attempt deadline.
    ///
    /// On the FIRST exhaustion, retry once with a completely fresh web view: the
    /// observed failure mode (metal leg) was a cold WebKit on a starved runner
    /// spending ~21s just reaching MainFrameLoadCompleted -- by the time the page
    /// existed the tile budget was gone. On the retry WebKit's processes and the
    /// SDK script cache are warm, so the reload renders in seconds. On the SECOND
    /// exhaustion we deliberately do NOT capture -- a blank or blocked map should
    /// fail loudly (missing_actual) rather than be baselined -- but we MUST still
    /// dispose the map: the Google Maps JS keeps a requestAnimationFrame /
    /// tile-loading loop alive, and leaving it running starves the main thread
    /// and desyncs LATER tests' captures. The onReady path disposes via its
    /// callback.
    private void waitForMapReady(final Form form, final int attemptsLeft, final Runnable onReady) {
        boolean ready;
        try {
            ready = currentMap.isMapReady();
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
            if (!retriedOnce) {
                retriedOnce = true;
                System.out.println("CN1SS:WARN:test=GoogleWebMap tiles not loaded after "
                        + (WAIT_ATTEMPTS * 500) + "ms; retrying once with a fresh web view");
                try {
                    currentMap.dispose();
                } catch (Throwable t) {
                    // best effort
                }
                form.removeAll();
                NativeMap fresh = buildWebMap();
                if (fresh.isNativeMap()) {
                    currentMap = fresh;
                    form.add(BorderLayout.CENTER, fresh);
                    form.revalidate();
                    waitForMapReady(form, WAIT_ATTEMPTS, onReady);
                    return;
                }
                // fresh view unavailable -> fall through to the loud failure
            }
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=FAILED reason=tiles-never-loaded");
            try {
                currentMap.dispose();
            } catch (Throwable t) {
                // best effort -- still report done so the suite advances
            }
            done();
            return;
        }
        UITimer.timer(500, false, form, () ->
                waitForMapReady(form, attemptsLeft - 1, onReady));
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
