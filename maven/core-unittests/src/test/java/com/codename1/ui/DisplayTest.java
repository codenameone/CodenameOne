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
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class DisplayTest extends UITestBase {

    public static void initInvokeAndBlockThreads() {
        RunnableWrapper.setMaxThreadCount(100);
    }

    public static void flushAnimations() {
        CN.getCurrentForm().getAnimationManager().flush();
    }

    /**
     * How long a queued runnable may take to reach the EDT before the suite calls it wedged.
     * Generous next to any real flush (they finish in milliseconds) and short next to a CI job
     * timeout, which is the point: a wedge should cost seconds, not the whole job.
     */
    private static final int EDT_FLUSH_TIMEOUT_MS = 30000;

    /**
     * Runs a flush on the EDT and waits for it -- with a deadline.
     *
     * <p>This used to call {@code callSeriallyAndWait}, which waits forever. Anything that
     * wedges the EDT therefore wedged the whole suite: {@code BleSensorReconnectTest} printed
     * its "Running" line, went silent, and the JDK 8 CI leg was cancelled 55 minutes later at
     * the job timeout, with no test named as the culprit and no evidence of what it was
     * waiting on. Every test in this suite reaches the EDT through here, so one deadlocked
     * session anywhere costs an hour of CI and reports nothing.
     *
     * <p>A wedge is now a fast, named failure carrying the stack of every live thread --
     * which is exactly the evidence needed to find the lock cycle behind it.
     */
    public static void flushEdt() {
        final Display display = Display.getInstance();
        if (display.isEdt()) {
            display.flushEdt();
            return;
        }

        // Still callSeriallyAndWait, deliberately: its RunnableWrapper is what carries the
        // invokeAndBlock nesting, and a plain callSerially plus a latch drops that -- which
        // showed up at once as StorageImageAsyncTest timing out against a display it could no
        // longer see. Only the deadline is new. The latch is what makes the EDT's write to it
        // visible here, and reports whether the runnable ever ran.
        final CountDownLatch done = new CountDownLatch(1);
        display.callSeriallyAndWait(new Runnable() {
            public void run() {
                try {
                    display.flushEdt();
                } finally {
                    done.countDown();
                }
            }
        }, EDT_FLUSH_TIMEOUT_MS);
        if (done.getCount() != 0) {
            throw new IllegalStateException("The EDT did not run a queued runnable within "
                    + EDT_FLUSH_TIMEOUT_MS + "ms -- it is wedged, most likely on a lock held by"
                    + " a thread that is itself waiting for the EDT.\n" + dumpAllThreads());
        }
    }

    /** Every live thread and where it is, for a wedge that has to be diagnosed from a CI log. */
    private static String dumpAllThreads() {
        StringBuilder sb = new StringBuilder("--- thread dump ---\n");
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> e : traces.entrySet()) {
            Thread t = e.getKey();
            sb.append('"').append(t.getName()).append("\" ").append(t.getState());
            if (t.isDaemon()) {
                sb.append(" daemon");
            }
            sb.append('\n');
            for (StackTraceElement frame : e.getValue()) {
                sb.append("\tat ").append(frame).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    @AfterEach
    void resetStatics() {
        Container.setBlockOverdraw(false);
        Component.setRevalidateOnStyleChange(true);
    }

    @Test
    void testConvertToPixelsHandlesVariousUnits() {
        Display display = Display.getInstance();

        assertEquals(Math.round(2f * Font.getDefaultFont().getHeight()), display.convertToPixels(2f, Style.UNIT_TYPE_REM));
        assertEquals(Math.round(25f / 100f * CN.getDisplayHeight()), display.convertToPixels(25f, Style.UNIT_TYPE_VH));
        assertEquals(Math.round(40f / 100f * CN.getDisplayWidth()), display.convertToPixels(40f, Style.UNIT_TYPE_VW));
        assertEquals(Math.round(10f / 100f * Math.min(CN.getDisplayWidth(), CN.getDisplayHeight())),
                display.convertToPixels(10f, Style.UNIT_TYPE_VMIN));
        assertEquals(Math.round(60f / 100f * Math.max(CN.getDisplayWidth(), CN.getDisplayHeight())),
                display.convertToPixels(60f, Style.UNIT_TYPE_VMAX));
        assertEquals(display.convertToPixels(2.5f), display.convertToPixels(2.5f, Style.UNIT_TYPE_DIPS));
        assertEquals(540, display.convertToPixels(50f, Style.UNIT_TYPE_SCREEN_PERCENTAGE, true));
        assertEquals(960, display.convertToPixels(50f, Style.UNIT_TYPE_SCREEN_PERCENTAGE, false));
        assertEquals(7, display.convertToPixels(7f, (byte) 99));
    }

    @Test
    void testSetPropertyHandlesSpecialKeys() {
        Display display = Display.getInstance();

        display.setProperty("AppArg", "launch");
        assertEquals("launch", display.getProperty("AppArg", ""));

        display.setProperty("blockOverdraw", "ignored");
        assertTrue(Container.isBlockOverdraw());

        display.setProperty("blockCopyPaste", "true");
        assertTrue(implementation.isBlockCopyAndPaste());

        display.setProperty("Component.revalidateOnStyleChange", "false");
        assertFalse(Component.isRevalidateOnStyleChange());

        display.setProperty("Component.revalidateOnStyleChange", "TRUE");
        assertTrue(Component.isRevalidateOnStyleChange());
    }

    @Test
    void testCallDetectionDelegatesToImplementation() {
        Display display = Display.getInstance();

        implementation.setCallState(true, true);
        assertTrue(display.isCallDetectionSupported());
        assertTrue(display.isInCall());

        implementation.setCallState(false, false);
        assertFalse(display.isCallDetectionSupported());
        assertFalse(display.isInCall());
    }

    @Test
    void testDebugRunnable() {
        Display display = Display.getInstance();
        boolean oldEnable = display.isEnableAsyncStackTraces();
        try {
            display.setEnableAsyncStackTraces(true);
            assertTrue(display.isEnableAsyncStackTraces());

            final boolean[] executed = {false};
            display.callSeriallyAndWait(new Runnable() {
                public void run() {
                    executed[0] = true;
                }
            });
            assertTrue(executed[0]);

            // Testing exception propagation behavior is tricky as it just logs.
            // But running callSerially with async traces enabled exercises DebugRunnable construction and run.

        } finally {
            display.setEnableAsyncStackTraces(oldEnable);
        }
    }

    @Test
    void testReadArrayStackArgumentUsesProvidedStack() throws Exception {
        Display display = Display.getInstance();
        Field stackField = Display.class.getDeclaredField("inputEventStackTmp");
        stackField.setAccessible(true);
        int[] originalStack = (int[]) stackField.get(display);
        stackField.set(display, new int[]{0, 999, 999});

        try {
            Method readArray = Display.class.getDeclaredMethod("readArrayStackArgument", int[].class, int.class);
            readArray.setAccessible(true);

            int[] sourceStack = new int[]{2, 10, 20};
            int[] decoded = (int[]) readArray.invoke(display, sourceStack, 0);
            assertArrayEquals(new int[]{10, 20}, decoded);
        } finally {
            stackField.set(display, originalStack);
        }
    }

    @FormTest
    void testInputEventStackRemainsBoundedForPointerBurst() throws Exception {
        Display display = Display.getInstance();
        Field stackField = Display.class.getDeclaredField("inputEventStack");
        stackField.setAccessible(true);
        Field stackTmpField = Display.class.getDeclaredField("inputEventStackTmp");
        stackTmpField.setAccessible(true);
        Field pointerField = Display.class.getDeclaredField("inputEventStackPointer");
        pointerField.setAccessible(true);
        int[] originalStack = (int[]) stackField.get(display);
        int[] originalStackTmp = (int[]) stackTmpField.get(display);
        int originalPointer = pointerField.getInt(display);

        try {
            stackField.set(display, new int[4]);
            stackTmpField.set(display, new int[4]);
            pointerField.setInt(display, 0);

            for (int i = 0; i < 20; i++) {
                display.pointerPressed(new int[]{10}, new int[]{10});
                display.pointerReleased(new int[]{10}, new int[]{10});
            }

            int[] bounded = (int[]) stackField.get(display);
            assertEquals(4, bounded.length);
        } finally {
            stackField.set(display, originalStack);
            stackTmpField.set(display, originalStackTmp);
            pointerField.setInt(display, originalPointer);
        }
    }

}
