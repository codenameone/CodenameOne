package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.NativeMap;
import com.codename1.maps.WebMapProvider;
import com.codename1.maps.spi.MapProviderRegistry;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Visual confirmation that the cross-platform web map provider
/// ({@link com.codename1.maps.WebMapProvider} -> {@link com.codename1.ui.BrowserComponent})
/// renders into a peer and composites into the captured frame. This is the
/// path that lets *any* platform show a web map -- including the ones with no
/// native map SDK.
///
/// Determinism: the original version loaded the **live** Google Maps JavaScript
/// SDK over the network and pixel-compared the result against a golden. That is
/// fundamentally non-deterministic -- tiles/labels shift run-to-run, and on a
/// slow CI runner the live map had not painted a single tile within the timeout
/// and the test captured a blank page (a guaranteed mismatch). No tolerance can
/// make a live third-party map deterministic.
///
/// So this exercises the *identical* provider/peer path with a fixed,
/// self-contained offline HTML document (solid colour regions only -- no
/// network, no web fonts, no images, no animation), giving a byte-stable
/// capture. It still fails loudly on the real regression this guards: a blank
/// or un-composited web peer (the page renders distinct land/water/road blocks;
/// a blank capture differs across essentially the whole frame).
public class GoogleWebMapScreenshotTest extends BaseTest {

    /// Self-contained, network-free "map-like" page: a green land background, a
    /// blue water band, two white roads and a darker-green park. Axis-aligned
    /// solid fills keep anti-aliasing (and therefore cross-runner-GPU variance)
    /// to a handful of edge pixels. No text/fonts (their rendering varies by
    /// backend), no external resources, no requestAnimationFrame loop.
    private static final String OFFLINE_MAP_HTML =
            "<!DOCTYPE html><html><head>"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
            + "<style>"
            + "html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:#a8d5a8}"
            + ".b{position:absolute}"
            + ".water{left:0;top:56%;width:100%;height:44%;background:#7fb2e5}"
            + ".park{left:58%;top:8%;width:28%;height:26%;background:#6fbf73}"
            + ".road{background:#ffffff}"
            + ".rv{left:18%;top:0;width:5%;height:56%}"
            + ".rh{left:0;top:30%;width:100%;height:4%}"
            + "</style></head><body>"
            + "<div class=\"b water\"></div>"
            + "<div class=\"b park\"></div>"
            + "<div class=\"b road rv\"></div>"
            + "<div class=\"b road rh\"></div>"
            + "</body></html>";

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
        // Baseline only on the mobile ports that ship a golden for this test
        // (iOS + Android). It was implicitly limited to those before: it skipped
        // wherever the Google Maps API key was absent, i.e. every platform
        // except the iOS/Android CI jobs that inject the secret. Now that the
        // page is offline (no key needed) it would otherwise also run on the
        // Linux GTK / Windows / JS / JavaSE screenshot suites, which have no
        // golden -- streaming an ungolden'd capture that fails their gate
        // (missing_expected). Gate explicitly to the two ports we baseline.
        String platform = com.codename1.ui.CN.getPlatformName();
        if (!"ios".equals(platform) && !"and".equals(platform)) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=platform-" + platform);
            done();
            return true;
        }
        // Force the web provider for this map only with a fixed offline page. The
        // empty-key default would make the provider report unavailable, so pass a
        // dummy non-empty key; the offline template has no {key} token to expand.
        MapProviderRegistry.register(
                new WebMapProvider("web", "offline", OFFLINE_MAP_HTML));
        MapProviderRegistry.setProviderOrder(new String[]{"web"});
        final NativeMap map = new NativeMap(new LatLng(41.0, 13.0), 5);
        MapProviderRegistry.setProviderOrder(null);
        if (!map.isNativeMap()) {
            System.out.println(
                    "CN1SS:INFO:test=GoogleWebMap status=SKIPPED reason=no-web-peer");
            done();
            return true;
        }
        Form form = new Form("Google Web Map", new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                final Form self = this;
                // The offline page paints instantly (no network), but the native
                // web-view PEER still has to be composited into the captured
                // frame, and that is backend- and runner-speed-sensitive: the
                // legacy OpenGL ES backend captured solid black at a 3s settle on
                // a starved CI runner (the iOS Metal + Android backends composited
                // fine at 3s). The original live-map version waited 22s and
                // composited on GL, so give the peer comparable headroom here --
                // the fixed content means the captured pixels are identical
                // whatever the wait, so this only affects reliability, not the
                // golden. Stays under the 30s native-test timeout.
                UITimer.timer(20000, false, self, () ->
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

    /// The map is a native peer (BrowserComponent) view. On the iOS Metal
    /// backend the screenshot has been seen to capture solid black -- the
    /// peer's web-view layer not composited into the captured frame in time.
    /// Force a repaint and a short extra present window before the capture so the
    /// composited frame includes the rendered page. Opt-in hook; default is 0
    /// for every other test.
    @Override
    protected long extraSettleBeforeCaptureMillis() {
        return 1200;
    }
}
