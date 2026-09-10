package com.codename1.ads.admob;

import android.test.ActivityInstrumentationTestCase2;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdSize;

public class BannerMeasurementTest extends ActivityInstrumentationTestCase2<ProbeActivity> {
    public BannerMeasurementTest() { super(ProbeActivity.class); }

    public void testBannerHasSdkDimensionsBeforePeerOrAdLoad() throws Throwable {
        final ProbeActivity activity = getActivity();
        runTestOnUiThread(new Runnable() {
            public void run() {
                AdMobNativeImpl bridge = new AdMobNativeImpl();
                for (int format = 0; format <= 4; format++) {
                    AdView banner = (AdView) bridge.createBanner(format,
                            "ca-app-pub-3940256099942544/6300978111", format, 320);
                    assertNull("Must be sized before it is attached to CN1", banner.getParent());
                    AdSize size = banner.getAdSize();
                    assertEquals(size.getWidthInPixels(activity), banner.getMeasuredWidth());
                    assertEquals(size.getHeightInPixels(activity), banner.getMeasuredHeight());
                    assertTrue("A SOUTH layout must reserve visible ad height", banner.getMeasuredHeight() > 1);
                    bridge.disposeBanner(format);
                }
            }
        });
    }
}
