/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
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
