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
    }

    /**
     * {@code removeCurrentSnackBar}: real Flutter's variant of
     * {@link #hideCurrentSnackBar} that skips the exit animation. This
     * runtime never animates a SnackBar out, so the two are identical here.
     */
    public void removeCurrentSnackBar(Object reason) {
        lastMessage = null;
        clearActiveStatus();
    }

    public void showSnackBar(SnackBar snackBar) {
        if (snackBar == null) {
            return;
        }
        // Real Flutter queues a SnackBar shown while another is up, and only
        // starts it once the current one finishes hiding (scaffold.dart's
        // showSnackBar/_handleSnackBarStatusChanged). This runtime shows at
        // most one SnackBar at a time with no queue, so a new showSnackBar
        // instead replaces the current one outright -- which still needs the
        // superseded Status cleared, the same as an explicit hide/remove,
        // or its ToastBar entry and expiry timer would outlive it.
        clearActiveStatus();
        String msg = consumeMessage(snackBar.getContent());
        long ms = snackBar.durationMillis();
        lastMessage = msg;
        lastDurationMillis = ms;
        if (!Display.isInitialized()) {
            return;
        }
        ToastBar.Status status = ToastBar.getInstance().createStatus();
        status.setMessage(msg);
        status.setExpires((int) ms);
        activeStatus = status;
        status.show();
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
