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
package com.codenameone.devruntime;

import com.codename1.io.Util;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.components.SpanLabel;

import java.io.InputStream;

/// The Codename One version this app is pinned to, and the warning shown when
/// a computer pushes a program compiled against a different one.
///
/// Running from a computer is supported only between matching versions: a
/// program compiled against another framework can call API this app does not
/// have, or rely on behaviour it does not share, and the failure then looks
/// like a bug in the program. So the version is shown in About, and a mismatch
/// is a question the person answers before the program starts -- never a
/// refusal, since a newer desktop is often fine for a simple program.
///
/// The version is written into `cn1-instant-version.properties` by the build
/// (a filtered resource in `common/pom.xml`), from the same `cn1.version` the
/// app compiles against, so it cannot drift from the framework actually inside.
final class DeviceRuntimeVersion {
    private static String cached;

    private DeviceRuntimeVersion() {
    }

    /// The pinned version, or "unknown" when the resource is missing (a build
    /// that skipped resource processing).
    static String get() {
        if (cached == null) {
            cached = read();
        }
        return cached;
    }

    private static String read() {
        InputStream in = null;
        try {
            in = Display.getInstance().getResourceAsStream(DeviceRuntimeVersion.class,
                    "/cn1-instant-version.properties");
            if (in == null) {
                return "unknown";
            }
            String text = Util.readToString(in);
            int at = text.indexOf("cn1.version=");
            if (at < 0) {
                return "unknown";
            }
            int end = text.indexOf('\n', at);
            String v = text.substring(at + "cn1.version=".length(), end < 0 ? text.length() : end).trim();
            // An unfiltered copy still says ${cn1.version}.
            return v.length() == 0 || v.indexOf('$') >= 0 ? "unknown" : v;
        } catch (Exception err) {
            return "unknown";
        } finally {
            Util.cleanup(in);
        }
    }

    /// True when the pushed program may start: the versions match, the sender
    /// did not say, or the person chose to run it anyway. Called from the
    /// thread serving the push, never the EDT; the computer waits without a
    /// deadline at this point.
    static boolean confirmRun(String desktopVersion) {
        String mine = get();
        if (desktopVersion == null || desktopVersion.length() == 0
                || "unknown".equals(mine) || mine.equals(desktopVersion)) {
            return true;
        }
        final boolean[] run = new boolean[1];
        final String message = "This program was built with Codename One " + desktopVersion
                + ", but this app runs Codename One " + mine + ". It may fail in ways it "
                + "would not on its own version. Use Codename One " + mine
                + " on your computer for reliable results.";
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                final Dialog d = new Dialog("Different version");
                d.setLayout(new BorderLayout());
                d.add(BorderLayout.CENTER, new SpanLabel(message));
                Button runBtn = new Button("Run anyway");
                Button cancel = new Button("Cancel");
                runBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        run[0] = true;
                        d.dispose();
                    }
                });
                cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        d.dispose();
                    }
                });
                Container actions = new Container(new GridLayout(2));
                actions.add(runBtn).add(cancel);
                d.add(BorderLayout.SOUTH, actions);
                // Unanswered counts as Cancel, so a phone left alone does not
                // hold the install lock for good.
                d.setTimeout(DeviceRuntimePairing.PROMPT_TIMEOUT_MS);
                d.show();
            }
        });
        return run[0];
    }
}
