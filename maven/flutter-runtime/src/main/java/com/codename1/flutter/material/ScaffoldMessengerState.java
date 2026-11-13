/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.components.ToastBar;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Text;
import com.codename1.io.Log;
import com.codename1.ui.Display;

/**
 * Shows {@link SnackBar}s through CN1's {@link ToastBar}. The SnackBar's
 * content widget is consumed as a message string: a {@code Text} child
 * supplies its data; any other widget falls back to {@code toString()} with
 * a log warning (mirroring button-label consumption). Headless (no Display)
 * the message is only recorded, which keeps the consumption logic testable.
 */
public class ScaffoldMessengerState {

    private String lastMessage;
    private long lastDurationMillis;
    // The ToastBar.Status showSnackBar() last showed, if any -- retained so
    // hideCurrentSnackBar/removeCurrentSnackBar and a later showSnackBar can
    // clear() it. Without this, clearing only lastMessage left the Status
    // itself in ToastBar's own status list, where it stayed visible/pending
    // until its own expiry timer eventually fired.
    private ToastBar.Status activeStatus;
    // The controller of the visible SnackBar, completed with the reason it closed.
    private ScaffoldFeatureController activeController;
    // The visible MaterialBanner: there is no banner slot in this runtime, so it is
    // shown as a ToastBar status that stays until hidden, and its controller.
    private ToastBar.Status bannerStatus;
    private ScaffoldFeatureController bannerController;

    ScaffoldMessengerState() {
    }

    /**
     * {@code hideCurrentSnackBar}: dismisses the visible SnackBar. Real
     * Flutter (scaffold.dart) runs the SnackBar's exit animation and then
     * completes it with {@code SnackBarClosedReason.hide}; this runtime has
     * no exit animation, so the ToastBar status is cleared immediately --
     * {@link #removeCurrentSnackBar} behaves the same way here.
     */
    public void hideCurrentSnackBar(Object reason) {
        lastMessage = null;
        clearActiveStatus();
        finishActive(SnackBarClosedReason.hide);
    }

    /**
     * {@code removeCurrentSnackBar}: real Flutter's variant of
     * {@link #hideCurrentSnackBar} that skips the exit animation. This
     * runtime never animates a SnackBar out, so the two are identical here.
     */
    public void removeCurrentSnackBar(Object reason) {
        lastMessage = null;
        clearActiveStatus();
        finishActive(SnackBarClosedReason.remove);
    }

    /** Flutter's {@code clearSnackBars}: removes the visible snack bar and any queued. */
    public void clearSnackBars() {
        removeCurrentSnackBar(null);
    }

    /// Shows {@code snackBar} and returns its controller, whose {@code closed} future
    /// completes with a {@link SnackBarClosedReason}: {@code timeout} when its
    /// duration runs out, {@code hide}/{@code remove} when dismissed explicitly, and
    /// {@code remove} when a later SnackBar replaces it (this runtime does not queue).
    public ScaffoldFeatureController showSnackBar(SnackBar snackBar) {
        final ScaffoldFeatureController controller = new ScaffoldFeatureController();
        if (snackBar == null) {
            controller.finish(SnackBarClosedReason.remove);
            return controller;
        }
        // Real Flutter queues a SnackBar shown while another is up, and only
        // starts it once the current one finishes hiding (scaffold.dart's
        // showSnackBar/_handleSnackBarStatusChanged). This runtime shows at
        // most one SnackBar at a time with no queue, so a new showSnackBar
        // instead replaces the current one outright -- which still needs the
        // superseded Status cleared, the same as an explicit hide/remove,
        // or its ToastBar entry and expiry timer would outlive it.
        clearActiveStatus();
        finishActive(SnackBarClosedReason.remove);
        String msg = consumeMessage(snackBar.getContent());
        long ms = snackBar.durationMillis();
        lastMessage = msg;
        lastDurationMillis = ms;
        activeController = controller;
        controller.onClose(new Runnable() {
            @Override
            public void run() {
                if (activeController == controller) {
                    hideCurrentSnackBar(null);
                }
            }
        });
        if (!Display.isInitialized()) {
            return controller;
        }
        ToastBar.Status status = ToastBar.getInstance().createStatus();
        status.setMessage(msg);
        status.setExpires((int) ms);
        activeStatus = status;
        status.show();
        new dart.async.Timer(dart.core.Duration.of(0, 0, 0, 0, ms, 0), new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (activeController == controller) {
                    activeStatus = null;
                    lastMessage = null;
                    finishActive(SnackBarClosedReason.timeout);
                }
            }
        });
        return controller;
    }

    private void finishActive(SnackBarClosedReason reason) {
        ScaffoldFeatureController c = activeController;
        activeController = null;
        if (c != null) {
            c.finish(reason);
        }
    }

    /// Flutter's {@code showMaterialBanner}. A banner stays until it is hidden; with
    /// no banner slot in this runtime its message is shown as a status that does
    /// not expire. Replaces any visible banner.
    public ScaffoldFeatureController showMaterialBanner(MaterialBanner banner) {
        removeCurrentMaterialBanner(null);
        final ScaffoldFeatureController controller = new ScaffoldFeatureController();
        bannerController = controller;
        controller.onClose(new Runnable() {
            @Override
            public void run() {
                if (bannerController == controller) {
                    hideCurrentMaterialBanner(null);
                }
            }
        });
        if (banner != null && Display.isInitialized()) {
            ToastBar.Status status = ToastBar.getInstance().createStatus();
            status.setMessage(consumeMessage(banner.getContent()));
            bannerStatus = status;
            status.show();
        }
        return controller;
    }

    /// Flutter's {@code hideCurrentMaterialBanner}.
    public void hideCurrentMaterialBanner(Object reason) {
        closeBanner(SnackBarClosedReason.hide);
    }

    /// Flutter's {@code removeCurrentMaterialBanner}.
    public void removeCurrentMaterialBanner(Object reason) {
        closeBanner(SnackBarClosedReason.remove);
    }

    /// Flutter's {@code clearMaterialBanners}.
    public void clearMaterialBanners() {
        removeCurrentMaterialBanner(null);
    }

    private void closeBanner(SnackBarClosedReason reason) {
        if (bannerStatus != null) {
            bannerStatus.clear();
            bannerStatus = null;
        }
        ScaffoldFeatureController c = bannerController;
        bannerController = null;
        if (c != null) {
            c.finish(reason);
        }
    }

    private void clearActiveStatus() {
        if (activeStatus != null) {
            activeStatus.clear();
            activeStatus = null;
        }
    }

    private static String consumeMessage(Widget content) {
        if (content == null) {
            return "";
        }
        if (content instanceof Text) {
            String d = ((Text) content).getData();
            return d == null ? "" : d;
        }
        try {
            Log.p("Flutter runtime: SnackBar content " + content.getClass().getSimpleName()
                    + " is not a Text; using its toString() as the message");
        } catch (Throwable t) {
            // headless: Log has no storage backend
        }
        return String.valueOf(content);
    }

    /**
     * The message most recently passed to {@link #showSnackBar} (test hook).
     */
    public String lastMessage() {
        return lastMessage;
    }

    /**
     * The duration (ms) most recently passed to {@link #showSnackBar}
     * (test hook).
     */
    public long lastDurationMillis() {
        return lastDurationMillis;
    }
}
