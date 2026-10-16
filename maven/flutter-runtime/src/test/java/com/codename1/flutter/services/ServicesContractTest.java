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
package com.codename1.flutter.services;

import com.codename1.flutter.material.TextEditingController;

import dart.async.Await;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Three platform-API contracts the runtime got wrong: Clipboard.getData with nothing to
 * paste, launchUrl on a URL the port declines, and a controller written its own text.
 */
class ServicesContractTest {

    @Test
    void anEmptyClipboardCompletesWithNull() {
        // Headless there is no clipboard at all: Flutter answers null, not a ClipboardData
        // whose text is null (which slips past the usual `data == null` guard).
        assertNull(Await.await$(Clipboard.getData("text/plain")));
    }

    @Test
    void aUrlThePortDeclinesIsNotReportedLaunched() {
        final List<String> opened = new ArrayList<String>();
        boolean ok = UrlLauncher.launch("myapp://x", u -> Boolean.FALSE, opened::add);
        assertFalse(ok, "the port said it cannot open it; the caller's fallback must run");
        assertTrue(opened.isEmpty(), "and nothing is handed to execute()");

        assertTrue(UrlLauncher.launch("https://x", u -> Boolean.TRUE, opened::add));
        assertTrue(UrlLauncher.launch("https://y", u -> null, opened::add),
                "unknown: the launch is attempted");
        assertEquals(2, opened.size());
        assertFalse(UrlLauncher.launch("https://z", u -> null, u -> {
            throw new IllegalStateException("no handler");
        }), "a launch that throws failed");
    }

    @Test
    void writingTheControllersOwnTextBackDoesNotNotify() {
        final TextEditingController c = new TextEditingController();
        c.setText("same");
        final int[] calls = {0};
        c.addListener(() -> {
            calls[0]++;
            if (calls[0] > 50) {
                throw new IllegalStateException("recursing");
            }
            c.setText(c.text());
        });
        c.setText("same");
        assertEquals(0, calls[0], "an equal value is not a change");
        c.setText("other");
        assertEquals(1, calls[0], "a real change notifies once; the write-back is a no-op");
    }
}
