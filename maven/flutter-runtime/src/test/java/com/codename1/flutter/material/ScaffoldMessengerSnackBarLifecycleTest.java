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
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.flutter.widgets.Text;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@code hideCurrentSnackBar}/{@code removeCurrentSnackBar} must clear the
 * {@link ToastBar.Status} {@code showSnackBar} created, not just the
 * test-facing {@code lastMessage} field -- otherwise the Status stays in
 * {@link ToastBar}'s own list (and visible) until its own expiry timer fires,
 * real Flutter's {@code hideCurrentSnackBar}/{@code removeCurrentSnackBar}
 * dismiss it immediately (scaffold.dart). A second {@code showSnackBar} while
 * one is still up must clear the superseded Status the same way, rather than
 * leaking it alongside the new one.
 *
 * <p>Runs under {@link RasterDisplay} so {@code Display.isInitialized()} is
 * true and {@code showSnackBar} actually creates a real {@link ToastBar.Status}
 * (the headless path, with no Display, never creates one at all -- see the
 * class comment on {@link ScaffoldMessengerState}). There is no current Form,
 * so the Status never actually paints; {@code ToastBar}'s own status list is
 * the only place this is observable, hence the reflection.</p>
 */
class ScaffoldMessengerSnackBarLifecycleTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    /** {@code ToastBar}'s own pending/visible status count (a process-wide singleton). */
    private static int statusCount() throws Exception {
        Field f = ToastBar.class.getDeclaredField("statuses");
        f.setAccessible(true);
        List<?> statuses = (List<?>) f.get(ToastBar.getInstance());
        return statuses.size();
    }

    @Test
    void hideCurrentSnackBarClearsTheToastBarStatusItCreated() throws Exception {
        int before = statusCount();
        ScaffoldMessengerState state = new ScaffoldMessengerState();
        SnackBar sb = new SnackBar();
        sb.content(new Text("Saved"));
        state.showSnackBar(sb);
        assertEquals(before + 1, statusCount(), "showSnackBar must create one ToastBar status");

        state.hideCurrentSnackBar(null);
        assertNull(state.lastMessage(), "hideCurrentSnackBar must clear the recorded message too");
        assertEquals(before, statusCount(),
                "hideCurrentSnackBar must clear the ToastBar status it created, not just lastMessage");
    }

    @Test
    void removeCurrentSnackBarClearsTheToastBarStatusItCreated() throws Exception {
        int before = statusCount();
        ScaffoldMessengerState state = new ScaffoldMessengerState();
        SnackBar sb = new SnackBar();
        sb.content(new Text("Saved"));
        state.showSnackBar(sb);
        assertEquals(before + 1, statusCount(), "showSnackBar must create one ToastBar status");

        state.removeCurrentSnackBar(null);
        assertEquals(before, statusCount(),
                "removeCurrentSnackBar must clear the ToastBar status it created, not just lastMessage");
    }

    @Test
    void aSecondShowSnackBarClearsThePreviousToastBarStatusInsteadOfLeakingIt() throws Exception {
        int before = statusCount();
        ScaffoldMessengerState state = new ScaffoldMessengerState();
        SnackBar first = new SnackBar();
        first.content(new Text("First"));
        state.showSnackBar(first);
        assertEquals(before + 1, statusCount(), "the first showSnackBar must create one ToastBar status");

        SnackBar second = new SnackBar();
        second.content(new Text("Second"));
        state.showSnackBar(second);
        assertEquals(before + 1, statusCount(),
                "a second showSnackBar must replace, not add to, the still-pending first status");
        assertEquals("Second", state.lastMessage());

        // Clean up the status the second showSnackBar created, so this
        // process-wide singleton starts the next test at its own baseline.
        state.hideCurrentSnackBar(null);
        assertEquals(before, statusCount());
    }
}
