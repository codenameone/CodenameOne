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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BrowserWindowTest extends UITestBase {

    @FormTest
    void nativeWindowDelegatesToImplementation() {
        Object nativeWindow = new Object();
        implementation.setNativeBrowserWindow(nativeWindow);

        BrowserWindow window = new BrowserWindow("https://start");

        ActionListener loadListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        };
        window.addLoadListener(loadListener);
        assertTrue(implementation.getNativeBrowserWindowOnLoadListener().contains(loadListener));

        window.removeLoadListener(loadListener);
        assertFalse(implementation.getNativeBrowserWindowOnLoadListener().contains(loadListener));

        window.setTitle("Docs");
        assertEquals("Docs", implementation.getNativeBrowserWindowTitle());

        window.setSize(640, 480);
        assertEquals(new Dimension(640, 480), implementation.getNativeBrowserWindowSize());

        ActionListener closeListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        };
        window.addCloseListener(closeListener);
        assertTrue(implementation.getNativeBrowserWindowCloseListener().contains(closeListener));

        assertFalse(implementation.isNativeBrowserWindowShowInvoked());
        window.show();
        assertTrue(implementation.isNativeBrowserWindowShowInvoked());

        assertFalse(implementation.isNativeBrowserWindowHideInvoked());
        assertFalse(implementation.isNativeBrowserWindowCleanupInvoked());
        window.close();
        assertTrue(implementation.isNativeBrowserWindowHideInvoked());
        assertTrue(implementation.isNativeBrowserWindowCleanupInvoked());

        window.removeCloseListener(closeListener);
        assertFalse(implementation.getNativeBrowserWindowCloseListener().contains(closeListener));
    }

    @FormTest
    public void testEvalRequest() {
        BrowserWindow.EvalRequest request = new BrowserWindow.EvalRequest();
        request.setJS("alert('Hello');");
        assertEquals("alert('Hello');", request.getJS());
    }

    @FormTest
    void fallbackWindowUsesBrowserForm() {
        implementation.setNativeBrowserWindow(null);

        Form backForm = Display.getInstance().getCurrent();
        assertNotNull(backForm);

        BrowserWindow window = new BrowserWindow("https://example.com/start");
        AtomicInteger closeCount = new AtomicInteger();
        window.addCloseListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeCount.incrementAndGet();
            }
        });

        window.show();
        Form browserForm = Display.getInstance().getCurrent();
        assertNotSame(backForm, browserForm);

        window.setTitle("Docs");
        assertEquals("Docs", browserForm.getTitle());

        window.close();
        assertEquals(1, closeCount.get());
        assertSame(backForm, Display.getInstance().getCurrent());

        window.close();
        assertEquals(1, closeCount.get());
    }
    @FormTest
    void aJavaScriptTimeoutStillFiresWhileItsWindowIsHidden() {
        // The timeout used to be driven by the painting of the browser's own surface, so
        // hiding or minimizing that window stopped the clock: the callback this method
        // documents arrived when the window came back, or never at all.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        BrowserComponent browser = new BrowserComponent();
        w.add(BorderLayout.CENTER, browser);
        w.show();
        DisplayTest.flushEdt();

        final int[] errors = new int[1];
        browser.execute(30, "1", new com.codename1.util.Callback<BrowserComponent.JSRef>() {
            public void onSucess(BrowserComponent.JSRef v) {
            }

            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                errors[0]++;
            }
        });
        DisplayTest.flushEdt();

        // The window goes away before the script answers.
        w.hide();
        DisplayTest.flushEdt();

        for (int iter = 0; iter < 300 && errors[0] == 0; iter++) {
            DisplayTest.flushEdt();
            try {
                Thread.sleep(2);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
        assertEquals(1, errors[0],
                "the timeout has to arrive even though its window is not on screen");

        w.dispose();
        DisplayTest.flushEdt();
    }
}
