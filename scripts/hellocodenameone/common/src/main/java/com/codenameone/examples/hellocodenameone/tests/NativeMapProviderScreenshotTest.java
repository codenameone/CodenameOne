package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.NativeMap;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Visual confirmation that a native map provider (Apple MapKit on iOS, Google
/// Maps on Android when wired) actually renders -- the kind of "silently blank"
/// failure a smoke build cannot catch.
///
/// To keep the baseline stable it views a deliberately low-variance scene: the
/// Italian peninsula and the Mediterranean at a regional zoom. The geography
/// does not change, there is strong land/water contrast (so a blank/grey tile
/// differs hugely from a real render), and at this zoom there is no traffic,
/// no street-level churn and minimal label movement. The map is left in its
/// default standard type with the user-location dot off, and the comparison
/// uses a lenient `.tolerance` so day-to-day tile/label noise does not fail CI
/// while a genuinely blocked map still does.
///
/// The test only emits a screenshot when a native provider is actually active
/// (`isNativeMap()`); on the simulator / builds with no `maps.provider` wired
/// in (or a missing key) it skips rather than baseline the vector fallback --
/// that path is covered by {@link NativeMapFallbackScreenshotTest}.
public class NativeMapProviderScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (com.codename1.ui.CN.isWatch()) {
            // No committed watch golden, and the watch has no native map (the
            // provider falls back to vector there). Phone/tablet cover this.
            System.out.println(
                    "CN1SS:INFO:test=NativeMapProvider status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        NativeMap map = new NativeMap(new LatLng(41.0, 13.0), 5);
        if (!map.isNativeMap()) {
            System.out.println(
                    "CN1SS:INFO:test=NativeMapProvider status=SKIPPED reason=no-native-provider");
            done();
            return true;
        }
        Form form = createForm("Native Map Provider", new BorderLayout(), "NativeMapProvider");
        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        // A live native map fetches its tiles asynchronously; give it longer than
        // the default settle so the imagery is present before the capture.
        com.codename1.ui.util.UITimer.timer(4000, false, parent, run);
    }
}
