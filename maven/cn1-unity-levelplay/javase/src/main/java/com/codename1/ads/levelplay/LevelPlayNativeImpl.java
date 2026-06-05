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
package com.codename1.ads.levelplay;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Graphics;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

import java.util.HashMap;
import java.util.Map;

/// Simulator (JavaSE) implementation of [LevelPlayNative]. There is no real ad
/// network here; instead it renders labelled placeholders and drives the full
/// event lifecycle so the entire advertising flow (load, show, dismiss, reward,
/// consent) can be exercised in the simulator without a device or LevelPlay account.
public class LevelPlayNativeImpl implements LevelPlayNative {
    private static final class FullScreen {
        int format;
        String adUnitId;
        boolean loaded;
    }

    private final Map<Integer, FullScreen> ads = new HashMap<Integer, FullScreen>();

    @Override
    public void initialize(String testDeviceIds, boolean testMode, int tagForChildDirected,
                           int tagForUnderAge, int maxAdContentRating) {
        // No SDK to initialize in the simulator.
    }

    @Override
    public boolean createFullScreen(int handle, int format, String adUnitId) {
        FullScreen fs = new FullScreen();
        fs.format = format;
        fs.adUnitId = adUnitId;
        ads.put(Integer.valueOf(handle), fs);
        return true;
    }

    @Override
    public void setServerSideVerification(int handle, String userId, String customData) {
        // Nothing to verify in the simulator.
    }

    @Override
    public void loadFullScreen(final int handle, String keywords, String contentUrl, boolean nonPersonalized) {
        final FullScreen fs = ads.get(Integer.valueOf(handle));
        if (fs == null) {
            return;
        }
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                fs.loaded = true;
                LevelPlayCallback.fire(handle, LevelPlayCallback.LOADED, 0, null, null, 0);
            }
        });
    }

    @Override
    public boolean isFullScreenLoaded(int handle) {
        FullScreen fs = ads.get(Integer.valueOf(handle));
        return fs != null && fs.loaded;
    }

    @Override
    public void showFullScreen(final int handle) {
        final FullScreen fs = ads.get(Integer.valueOf(handle));
        if (fs == null || !fs.loaded) {
            LevelPlayCallback.fire(handle, LevelPlayCallback.SHOW_FAILED, LevelPlayErrorCodes.NOT_READY,
                    "No ad loaded", null, 0);
            return;
        }
        fs.loaded = false;
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                presentPlaceholder(handle, fs);
            }
        });
    }

    private void presentPlaceholder(final int handle, FullScreen fs) {
        final boolean rewarded = fs.format == LevelPlayProvider.FORMAT_REWARDED
                || fs.format == LevelPlayProvider.FORMAT_REWARDED_INTERSTITIAL;
        final Dialog dlg = new Dialog(formatLabel(fs.format));
        dlg.setLayout(new BorderLayout());
        SpanLabel body = new SpanLabel("Codename One simulator placeholder. Ad unit: "
                + (fs.adUnitId == null ? "(none)" : fs.adUnitId));
        dlg.add(BorderLayout.CENTER, body);
        Button close = new Button(rewarded ? "Close & grant reward" : "Close");
        dlg.add(BorderLayout.SOUTH, close);

        LevelPlayCallback.fire(handle, LevelPlayCallback.SHOWN, 0, null, null, 0);
        LevelPlayCallback.fire(handle, LevelPlayCallback.IMPRESSION, 0, null, null, 0);

        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (rewarded) {
                    LevelPlayCallback.fire(handle, LevelPlayCallback.REWARD, 0, null, "reward", 1);
                }
                dlg.dispose();
                LevelPlayCallback.fire(handle, LevelPlayCallback.DISMISSED, 0, null, null, 0);
            }
        });
        dlg.show();
    }

    private static String formatLabel(int format) {
        switch (format) {
            case LevelPlayProvider.FORMAT_REWARDED:
                return "Rewarded Ad (test)";
            case LevelPlayProvider.FORMAT_REWARDED_INTERSTITIAL:
                return "Rewarded Interstitial (test)";
            case LevelPlayProvider.FORMAT_APP_OPEN:
                return "App Open Ad (test)";
            default:
                return "Interstitial Ad (test)";
        }
    }

    @Override
    public void setAppOpenAutoShow(int handle, boolean enabled) {
        // No foreground hook in the simulator; the developer can call show() manually.
    }

    @Override
    public void disposeFullScreen(int handle) {
        ads.remove(Integer.valueOf(handle));
    }

    @Override
    public PeerComponent createBanner(final int handle, String adUnitId, int sizeType, int widthDp) {
        return new PlaceholderBanner(handle, sizeType);
    }

    @Override
    public void loadBanner(final int handle, String keywords, String contentUrl, boolean nonPersonalized) {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                LevelPlayCallback.fire(handle, LevelPlayCallback.LOADED, 0, null, null, 0);
                LevelPlayCallback.fire(handle, LevelPlayCallback.IMPRESSION, 0, null, null, 0);
            }
        });
    }

    @Override
    public void disposeBanner(int handle) {
        // Nothing to release for the placeholder.
    }

    @Override
    public void requestConsent(boolean underAgeOfConsent) {
        // No consent form in the simulator; report "not required" immediately.
        LevelPlayCallback.fire(0, LevelPlayCallback.CONSENT_COMPLETE, 2, null, null, 0);
    }

    @Override
    public int getConsentStatus() {
        return 2; // AdConsent.STATUS_NOT_REQUIRED
    }

    @Override
    public boolean canRequestAds() {
        return true;
    }

    @Override
    public void resetConsent() {
        // Nothing stored in the simulator.
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    /// A lightweight banner placeholder drawn directly by Codename One (no
    /// underlying native view) so banners render in the simulator.
    private static final class PlaceholderBanner extends PeerComponent {
        private final int sizeType;

        PlaceholderBanner(int handle, int sizeType) {
            super(null);
            this.sizeType = sizeType;
            setFocusable(false);
        }

        @Override
        public void paint(Graphics g) {
            Style s = getStyle();
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            g.setColor(0x303030);
            g.fillRect(x, y, w, h);
            g.setColor(0xffffff);
            String label = "LevelPlay Test Banner";
            int sw = g.getFont().stringWidth(label);
            int sh = g.getFont().getHeight();
            g.drawString(label, x + (w - sw) / 2, y + (h - sh) / 2);
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            int h = sizeType == com.codename1.ads.BannerAd.SIZE_MEDIUM_RECTANGLE
                    ? CN.convertToPixels(250) : CN.convertToPixels(50);
            int w = CN.convertToPixels(320);
            return new com.codename1.ui.geom.Dimension(w, h);
        }
    }
}
