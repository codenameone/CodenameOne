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

    ScaffoldMessengerState() {
    }

    /**
     * {@code hideCurrentSnackBar}: dismisses the visible SnackBar. This runtime
     * shows SnackBars through the auto-expiring {@link ToastBar}, so there is no
     * retained handle to hide; the call clears the recorded message.
     */
    public void hideCurrentSnackBar(Object reason) {
        lastMessage = null;
    }

    public void showSnackBar(SnackBar snackBar) {
        if (snackBar == null) {
            return;
        }
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
        status.show();
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
