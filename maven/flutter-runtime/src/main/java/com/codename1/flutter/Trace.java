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
package com.codename1.flutter;

/**
 * Whether the runtime's first-frame attribution counters are collecting.
 *
 * <p>The counters — per-class build time, per-class layout self time, component
 * creation cost, glyph rasterisation cost — are what turned "the app takes
 * 250ms to start" into a list of things to fix. But they are not free: layout
 * attribution alone costs two {@code nanoTime} reads, a
 * {@code getClass().getSimpleName()} (which allocates a String) and a map
 * lookup on EVERY box laid out, and a screen lays out a few thousand boxes per
 * frame. Left always-on they measure themselves into the number they report,
 * and every application that never asks for the trace pays for it.</p>
 *
 * <p>Resolved once, from {@code cn1.flutter.startupTrace}, so the check itself
 * is a static field read.</p>
 */
public final class Trace {

    private Trace() {
    }

    private static Boolean enabled;

    /** Whether attribution counters should collect. */
    public static boolean on() {
        Boolean e = enabled;
        if (e == null) {
            e = Boolean.FALSE;
            try {
                if (com.codename1.ui.Display.isInitialized()) {
                    e = Boolean.valueOf("true".equals(com.codename1.ui.Display.getInstance()
                            .getProperty("cn1.flutter.startupTrace", "false")));
                }
            } catch (Throwable t) {
                e = Boolean.FALSE;
            }
            enabled = e;
        }
        return e.booleanValue();
    }

    /** Test hook: forces the flag, or restores lazy resolution with null. */
    public static void force(Boolean value) {
        enabled = value;
    }
}
