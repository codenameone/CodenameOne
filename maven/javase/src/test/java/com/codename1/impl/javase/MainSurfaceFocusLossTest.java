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
package com.codename1.impl.javase;

import com.codename1.testing.junit.CodenameOneTest;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Guards that the simulator's main frame reports its keyboard focus to
 * {@code Desktop} as window zero.
 *
 * <p>Core already disarms the main surface's held keys when it is told focus left it,
 * and a core test covers that branch by calling {@code windowFocusChanged(0, false)}
 * directly. This port never made that call: the main frame's {@code windowActivated}
 * and {@code windowDeactivated} were empty bodies, and nothing else here passes window
 * zero. The core branch was therefore dead in the simulator and a key held on the main
 * form went on repeating after focus moved away.</p>
 *
 * <p>So this drives the real {@link WindowListener} on the real frame rather than
 * calling {@code Desktop} itself -- calling {@code Desktop} is what the core test
 * already does, and it is precisely the step that was missing.</p>
 *
 * <p>Needs a display, because the listener lives on a real frame.</p>
 */
@CodenameOneTest
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
class MainSurfaceFocusLossTest {

    @Test
    void deactivatingTheMainFrameDisarmsAKeyHeldOnTheMainForm() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "the listener lives on a real frame");
        JFrame frame = cn1Frame();
        assertNotNull(frame, "the desktop port must own a JFrame");

        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                Form main = new Form("main", new BorderLayout());
                main.show();
            }
        });

        // Arm the repeat. This is the state the fix has to clear, so assert it is
        // really armed first -- otherwise the test passes against anything.
        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                Display.getInstance().keyPressed(70);
            }
        });
        assertTrue(keyRepeatArmed(), "the press has to arm the repeat, which is the state under test");

        dispatchToFrameListeners(frame, WindowEvent.WINDOW_DEACTIVATED);
        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
            }
        });

        assertFalse(keyRepeatArmed(),
                "the main frame losing focus has to reach Desktop as window zero; with "
                        + "an empty windowDeactivated nothing told the main surface, and "
                        + "the key repeated for as long as the form stayed open");

        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                Display.getInstance().keyReleased(70);
            }
        });
    }

    /// Reflected rather than read through an accessor, matching how the core test for
    /// this same state does it: the main surface keeps the flag in its own field.
    private static boolean keyRepeatArmed() throws Exception {
        Field f = Display.class.getDeclaredField("keyRepeatCharged");
        f.setAccessible(true);
        return f.getBoolean(Display.getInstance());
    }

    private static void dispatchToFrameListeners(final JFrame frame, final int id) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                WindowEvent e = new WindowEvent(frame, id);
                for (WindowListener l : frame.getWindowListeners()) {
                    if (id == WindowEvent.WINDOW_DEACTIVATED) {
                        l.windowDeactivated(e);
                    } else {
                        l.windowActivated(e);
                    }
                }
            }
        });
    }

    private static JFrame cn1Frame() {
        for (Frame f : Frame.getFrames()) {
            if (f instanceof JFrame && f.isVisible()) {
                return (JFrame) f;
            }
        }
        return null;
    }

    private static void runOnCn1AndWait(final Runnable r) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } finally {
                    latch.countDown();
                }
            }
        });
        latch.await();
    }
}
