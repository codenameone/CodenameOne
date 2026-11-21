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
package com.codename1.testing;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/// The helpers called the way a real test calls them: from the test thread,
/// with the EDT running. `TestUtilsTest` runs on the EDT and never reaches the
/// marshalling these exercise.
class TestUtilsEdtTest extends UITestBase {

    @AfterEach
    void restoreTimeout() {
        TestUtils.edtTimeout = 60000;
    }

    private Form showOnEdt(final Component... children) {
        final AtomicReference<Form> shown = new AtomicReference<Form>();
        Display.getInstance().callSeriallyAndWait(() -> {
            Form f = new Form("Edt", BoxLayout.y());
            for (Component c : children) {
                f.add(c);
            }
            f.show();
            shown.set(f);
        });
        return shown.get();
    }

    @Test
    void lookupsAndAssertsRunOffTheEdt() {
        assertFalse(Display.getInstance().isEdt());
        Label label = new Label("Hello");
        label.setName("greeting");
        showOnEdt(label);

        assertSame(label, TestUtils.findByName("greeting"));
        assertSame(label, TestUtils.findLabelText("Hello"));
        assertSame(label, TestUtils.getComponentByPath(new int[]{0}));
        assertNull(TestUtils.getComponentByPath(new int[]{5}), "a path past the children answers null");
        TestUtils.assertLabel("greeting", "Hello");
        TestUtils.setText("greeting", "Bye");
        TestUtils.assertLabel("greeting", "Bye");
    }

    @Test
    void walkRunsOnTheEdt() {
        final AtomicBoolean offEdtRead = new AtomicBoolean();
        Label probe = new Label("probe") {
            @Override
            public String getName() {
                if (!Display.getInstance().isEdt()) {
                    offEdtRead.set(true);
                }
                return "probe";
            }
        };
        showOnEdt(probe);
        assertSame(probe, TestUtils.findByName("probe"));
        assertFalse(offEdtRead.get(), "the tree was walked from the test thread");
    }

    @Test
    void failureOnTheEdtIsRethrownAtOnce() {
        showOnEdt(new Label("x"));
        long start = System.currentTimeMillis();
        RuntimeException e = assertThrows(RuntimeException.class, () -> TestUtils.clickMenuItem("missing"));
        assertTrue(e.getMessage().contains("Command not found"), e.getMessage());
        assertTrue(System.currentTimeMillis() - start < 5000, "waited for the timeout instead of rethrowing");
    }

    @Test
    void wrongComponentTypeFailsInsteadOfCasting() {
        Label notAButton = new Label("plain");
        notAButton.setName("plain");
        showOnEdt(notAButton);
        RuntimeException e = assertThrows(RuntimeException.class, () -> TestUtils.clickButtonByName("plain"));
        assertTrue(e.getMessage().contains("not a Button"), e.getMessage());
    }

    @Test
    void blockedEdtFailsTheLookupInsteadOfHanging() throws Exception {
        showOnEdt(new Label("x"));
        TestUtils.edtTimeout = 300;
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch blocked = new CountDownLatch(1);
        Display.getInstance().callSerially(() -> {
            blocked.countDown();
            try {
                // Bounded so a broken test cannot wedge the suite.
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(blocked.await(5, TimeUnit.SECONDS));
        try {
            RuntimeException e = assertThrows(RuntimeException.class, () -> TestUtils.findByName("x"));
            assertTrue(e.getMessage().contains("did not respond"), e.getMessage());
            e = assertThrows(RuntimeException.class, () -> TestUtils.keyPress(1));
            assertTrue(e.getMessage().contains("did not respond"), e.getMessage());
        } finally {
            release.countDown();
        }
    }

    @Test
    void clickThatBlocksInANestedLoopReturns() throws Exception {
        TestUtils.edtTimeout = 3000;
        final CountDownLatch leave = new CountDownLatch(1);
        final AtomicBoolean handlerReturned = new AtomicBoolean();
        Button b = new Button("Open");
        b.setName("open");
        // What a modal dialog does: block in the handler while the EDT keeps
        // serving events through invokeAndBlock.
        b.addActionListener(evt -> {
            Display.getInstance().invokeAndBlock(() -> {
                try {
                    leave.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
            handlerReturned.set(true);
        });
        showOnEdt(b);
        try {
            TestUtils.clickButtonByName("open");
            assertFalse(handlerReturned.get(), "the click should return while its handler is still blocked");
            // The EDT is still serving the test while the handler waits.
            assertSame(b, TestUtils.findByName("open"));
        } finally {
            leave.countDown();
        }
    }
}
