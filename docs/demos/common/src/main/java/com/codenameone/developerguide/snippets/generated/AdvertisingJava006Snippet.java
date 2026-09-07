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


class AdvertisingJava006Snippet {


    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    
    void snippet() throws Exception {
        // tag::advertising-java-006[]
        RewardedAd ad = new RewardedAd("ca-app-pub-xxx/yyy");
        ad.setServerSideVerificationOptions(new ServerSideVerificationOptions(userId, "level=7"));
        // show() does nothing when no ad is loaded, so the offer starts disabled
        // and goes back to disabled the moment it is spent.
        watchForCoins.setEnabled(false);
        watchForCoins.addActionListener(e -> {
            watchForCoins.setEnabled(false);
            ad.show(reward -> {
                // Server-side verification is configured above, so the network
                // posts the reward to your server and that is what credits the
                // user. This callback runs before anything has verified it, so
                // it is presentation only -- crediting here would pay twice.
                showRewardPending(reward.getAmount());
            });
        });

        ad.setAdListener(new AdListener() {
            public void onLoaded() {
                retryDelay = 1000;
                loadFailures = 0;
                // A rewarded ad is an opt-in format, so a loaded ad only enables
                // the offer. Showing it here would put a full screen ad in front
                // of a user who never asked for one.
                watchForCoins.setEnabled(true);
            }

            public void onDismissed() {
                // A shown ad is spent and a session cannot be shown twice, so
                // load the next one. Without this the offer is enabled once and
                // never again.
                ad.load();
            }

            public void onShowFailed(AdError error) {
                // Nothing was consumed, so put the offer back rather than
                // leaving it disabled for the rest of the screen.
                ad.load();
            }

            public void onFailedToLoad(AdError error) {
                // getCode() is provider specific and getDomain() says whose it
                // is: Codename One's own errors carry no domain, an adapter's
                // carry the SDK's. The numbers collide -- AppLovin reports -1
                // for an unspecified error and CODE_UNSUPPORTED is also -1 --
                // so read the constants only for a framework error. Those two
                // are genuinely permanent: an unsupported platform or a bad ad
                // unit id never becomes valid, and retrying them turns the
                // graceful no-ads path into a permanent timer.
                if (error.getDomain() == null
                        && (error.getCode() == AdError.CODE_UNSUPPORTED
                            || error.getCode() == AdError.CODE_INVALID_REQUEST)) {
                    return;
                }
                // A provider's code means nothing here, so treat it as possibly
                // transient -- bounded, so an unrecognized permanent failure
                // cannot retry for the life of the screen either.
                if (++loadFailures > 5) {
                    return;
                }
                retryDelay = Math.min(retryDelay * 2, 60000);
                UITimer.timer(retryDelay, false, form, () -> ad.load());
            }
        });
        ad.load();
        // end::advertising-java-006[]
    }

    void grantCoins(int coins) { }
    void showRewardPending(int coins) { }
    Button watchForCoins = new Button("Watch for coins");
    String userId = "42";


    int retryDelay = 1000;
    int loadFailures;

}
