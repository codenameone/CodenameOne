/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import com.codename1.analytics.*;
import com.codename1.appreview.*;
import com.codename1.ads.*;
import com.codename1.util.*;
import java.util.*;


class AdvertisingJava009Snippet {

    Form form;

    // tag::advertising-java-009[]
    void startAds() {
        AdConfig cfg = new AdConfig()
                .testMode(true)
                .addTestDevice("YOUR_TEST_DEVICE_ID")
                .tagForChildDirectedTreatment(AdConfig.TAG_FALSE)
                .maxAdContentRating(AdConfig.RATING_G);

        AdManager.initialize(cfg, ready -> {
            // false when no provider was installed, or the network's own
            // initialize failed. Stop here: with no provider AdConsent reports
            // consent as not required and canRequestAds() answers true, so
            // carrying on would walk into loadAds() with nothing behind it.
            if (!ready) {
                showAdFreeUi();
                return;
            }

            // Consent has to be settled before the first load, not before
            // initialize: requestConsent presents the GDPR form and, on iOS,
            // the App Tracking Transparency prompt, and both need the SDK up.
            AdConsent.requestConsent(status -> {
                if (AdConsent.canRequestAds()) {
                    loadAds();
                } else {
                    // STATUS_REQUIRED with consent withheld. Personalized ads
                    // are off the table; show the app without them rather
                    // than blocking on a prompt the user already declined.
                    showAdFreeUi();
                }
            });
        });
    }

    void loadAds() {
    }

    void showAdFreeUi() {
    }
    // end::advertising-java-009[]
}
