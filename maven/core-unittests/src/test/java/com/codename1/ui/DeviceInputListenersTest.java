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
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the Component level device-input listeners (context menu, mouse wheel, stylus) and
 * {@link ActionEvent#getPointerEvent()} by dispatching synthetic pointer events on the EDT.
 */
class DeviceInputListenersTest extends UITestBase {

    private static Button centerButton(String text) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Button b = new Button(text);
        b.setPreferredSize(new Dimension(200, 120));
        form.add(BorderLayout.CENTER, b);
        form.revalidate();
        return b;
    }

    private static int centerX(Button b) {
        return b.getAbsoluteX() + b.getWidth() / 2;
    }

    private static int centerY(Button b) {
        return b.getAbsoluteY() + b.getHeight() / 2;
    }

    @FormTest
    void contextMenuFiresOnSecondaryButton() {
        Button b = centerButton("ctx");
        final AtomicBoolean ctx = new AtomicBoolean(false);
        final AtomicBoolean buttonPressed = new AtomicBoolean(false);
        b.addContextMenuListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.set(true);
                evt.consume();
            }
        });
        b.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                buttonPressed.set(true);
            }
        });

        implementation.setPointerEventMetadata(PointerEvent.BUTTON_SECONDARY,
                PointerEvent.MASK_SECONDARY, PointerEvent.TYPE_MOUSE, 1f, 0, 0, 0, 0, false);
        Display.getInstance().getCurrent().pointerPressed(centerX(b), centerY(b));
        implementation.resetPointerEventMetadata();

        assertTrue(ctx.get(), "context menu listener should fire on a secondary button press");
        assertFalse(buttonPressed.get(), "a consumed context menu should suppress the normal press");
    }

    @FormTest
    void contextMenuFiresOnLongPress() {
        Button b = centerButton("ctx2");
        final AtomicBoolean ctx = new AtomicBoolean(false);
        b.addContextMenuListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.set(true);
                evt.consume();
            }
        });
        Display.getInstance().getCurrent().longPointerPress(centerX(b), centerY(b));
        assertTrue(ctx.get(), "long press should be surfaced as a context menu request");
    }

    @FormTest
    void noContextMenuOnPrimaryButton() {
        Button b = centerButton("primary");
        final AtomicBoolean ctx = new AtomicBoolean(false);
        b.addContextMenuListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.set(true);
            }
        });
        implementation.setPointerEventMetadata(PointerEvent.BUTTON_PRIMARY,
                PointerEvent.MASK_PRIMARY, PointerEvent.TYPE_MOUSE, 1f, 0, 0, 0, 0, false);
        Display.getInstance().getCurrent().pointerPressed(centerX(b), centerY(b));
        Display.getInstance().getCurrent().pointerReleased(centerX(b), centerY(b));
        implementation.resetPointerEventMetadata();
        assertFalse(ctx.get(), "primary button must not request a context menu");
    }

    @FormTest
    void stylusListenerFiresOnlyForStylus() {
        Button b = centerButton("pen");
        final AtomicReference<PointerEvent> got = new AtomicReference<PointerEvent>();
        b.addStylusListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                got.set(evt.getPointerEvent());
            }
        });

        // stylus -> fires, with the pressure carried through
        implementation.setPointerEventMetadata(PointerEvent.BUTTON_PRIMARY, PointerEvent.MASK_PRIMARY,
                PointerEvent.TYPE_STYLUS, 0.6f, 0, 0, 0, 0, false);
        Display.getInstance().getCurrent().pointerPressed(centerX(b), centerY(b));
        assertNotNull(got.get(), "stylus listener should fire for a stylus pointer");
        assertTrue(got.get().isStylus());
        assertEquals(0.6f, got.get().getPressure(), 0.0001f);

        // touch -> does not fire
        got.set(null);
        implementation.setPointerEventMetadata(PointerEvent.BUTTON_PRIMARY, PointerEvent.MASK_PRIMARY,
                PointerEvent.TYPE_TOUCH, 1f, 0, 0, 0, 0, false);
        Display.getInstance().getCurrent().pointerPressed(centerX(b), centerY(b));
        implementation.resetPointerEventMetadata();
        assertNull(got.get(), "stylus listener must not fire for a finger touch");
    }

    @FormTest
    void mouseWheelListenerReceivesDeltasAndConsumes() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Button b = new Button("wheel");
        b.setPreferredSize(new Dimension(200, 120));
        form.add(BorderLayout.CENTER, b);
        form.revalidate();

        final AtomicReference<WheelEvent> got = new AtomicReference<WheelEvent>();
        b.addMouseWheelListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                got.set((WheelEvent) evt);
                evt.consume();
            }
        });

        int x = b.getAbsoluteX() + b.getWidth() / 2;
        int y = b.getAbsoluteY() + b.getHeight() / 2;
        boolean consumed = Display.getInstance().fireMouseWheelEvent(x, y, 0, 30, true,
                PointerEvent.MODIFIER_CONTROL);

        assertTrue(consumed, "a consumed wheel event should report consumed");
        assertNotNull(got.get());
        assertEquals(30, got.get().getDeltaY());
        assertTrue(got.get().isPrecise());
        assertTrue(got.get().isControlDown());
    }

    @FormTest
    void actionEventExposesPointerEventForPointerTypesOnly() {
        ActionEvent pointer = new ActionEvent("src", ActionEvent.Type.PointerPressed, 3, 4);
        assertNotNull(pointer.getPointerEvent(), "pointer events expose the current pointer snapshot");

        ActionEvent theme = new ActionEvent("src", ActionEvent.Type.Theme);
        assertNull(theme.getPointerEvent(), "non pointer events have no pointer snapshot");

        // an explicitly attached snapshot is returned verbatim
        PointerEvent explicit = new PointerEvent(1, 2, PointerEvent.BUTTON_MIDDLE,
                PointerEvent.MASK_MIDDLE, PointerEvent.TYPE_MOUSE, 1f, 0, 0, 0, 0, false);
        ActionEvent custom = new ActionEvent("src", ActionEvent.Type.Theme);
        custom.setPointerEvent(explicit);
        assertSame(explicit, custom.getPointerEvent());
    }

    @FormTest
    void magnifyAndRotationGesturesDispatchToComponent() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        final AtomicReference<Float> scaleGot = new AtomicReference<Float>();
        final AtomicReference<Float> radiansGot = new AtomicReference<Float>();
        Container gestureCmp = new Container() {
            @Override
            protected boolean pinch(float scale) {
                scaleGot.set(scale);
                return true;
            }

            @Override
            protected boolean rotation(float radians) {
                radiansGot.set(radians);
                return true;
            }
        };
        gestureCmp.setPreferredSize(new Dimension(200, 120));
        form.add(BorderLayout.CENTER, gestureCmp);
        form.revalidate();

        int x = gestureCmp.getAbsoluteX() + gestureCmp.getWidth() / 2;
        int y = gestureCmp.getAbsoluteY() + gestureCmp.getHeight() / 2;

        Display.getInstance().fireMagnifyGesture(x, y, 1.5f);
        assertNotNull(scaleGot.get(), "magnify gesture should reach the component pinch callback");
        assertEquals(1.5f, scaleGot.get(), 0.0001f);

        Display.getInstance().fireRotationGesture(x, y, 0.25f);
        assertNotNull(radiansGot.get(), "rotation gesture should reach the component rotation callback");
        assertEquals(0.25f, radiansGot.get(), 0.0001f);
    }

    /// A producer with no gesture phases must leave nothing behind.
    ///
    /// Display is a singleton, and a producer can still report magnification
    /// through Desktop.windowMagnifyGesture alone -- no begin, no release. The
    /// native Windows and Linux ports now forward the touchpad's phases, but
    /// the JavaSE simulator does not: it synthesises a two-pointer drag, which
    /// Component ends through pointerDragged instead. A
    /// claim recorded for them would never be read and never cleared, so the
    /// component that consumed one pinch, and the whole form hierarchy behind
    /// it, would stay reachable from the singleton for the life of the process,
    /// surviving every navigation away from it.
    @FormTest
    void aPhaselessMagnifyGestureRetainsNothing() throws Exception {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Container gestureCmp = new Container() {
            @Override
            protected boolean pinch(float scale) {
                return true;
            }
        };
        gestureCmp.setPreferredSize(new Dimension(200, 120));
        form.add(BorderLayout.CENTER, gestureCmp);
        form.revalidate();

        Display.getInstance().fireMagnifyGesture(
                gestureCmp.getAbsoluteX() + gestureCmp.getWidth() / 2,
                gestureCmp.getAbsoluteY() + gestureCmp.getHeight() / 2, 1.5f);

        java.lang.reflect.Field held = Display.class.getDeclaredField("pinchGestureTarget");
        held.setAccessible(true);
        assertNull(held.get(Display.getInstance()),
                "a gesture with no reported begin must not leave a component held");
    }

    /// The phased path has to end the gesture on the component that took it.
    ///
    /// A touchpad emits no pointer events, so the two-pointer path in Component
    /// that normally calls pinchReleased() never runs for a trackpad pinch. If
    /// the port reports the phases and Display drops them, a component that
    /// zoomed stays in its pinching state after the fingers leave -- an
    /// ImageViewer never commits currentZoom. That was the state the native
    /// Windows, Linux and JavaScript ports were in: they delivered the scale
    /// updates and neither phase.
    @FormTest
    void aPhasedMagnifyGestureEndsOnTheComponentThatTookIt() throws Exception {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        final AtomicInteger released = new AtomicInteger();
        Container gestureCmp = new Container() {
            @Override
            protected boolean pinch(float scale) {
                return true;
            }

            @Override
            protected void pinchReleased(int x, int y) {
                released.incrementAndGet();
            }
        };
        gestureCmp.setPreferredSize(new Dimension(200, 120));
        form.add(BorderLayout.CENTER, gestureCmp);
        form.revalidate();

        int x = gestureCmp.getAbsoluteX() + gestureCmp.getWidth() / 2;
        int y = gestureCmp.getAbsoluteY() + gestureCmp.getHeight() / 2;

        Display.getInstance().firePinchBeginGesture();
        Display.getInstance().fireMagnifyGesture(x, y, 1.5f);
        assertEquals(0, released.get(), "the gesture is still in progress");

        Display.getInstance().firePinchReleaseGesture(x, y);
        assertEquals(1, released.get(),
                "the component that consumed the pinch must be told the gesture ended");

        java.lang.reflect.Field held = Display.class.getDeclaredField("pinchGestureTarget");
        held.setAccessible(true);
        assertNull(held.get(Display.getInstance()),
                "the release must clear the claim so the next gesture is not stranded");
    }

    /// A release with no component having taken the gesture must not throw.
    ///
    /// The ports call firePinchReleaseGesture unconditionally at the end of a
    /// touchpad gesture, including one that passed over nothing that zooms, so
    /// the no-target case is the common one rather than an edge.
    @FormTest
    void aReleaseWithNoTargetIsHarmless() {
        Display.getInstance().firePinchBeginGesture();
        Display.getInstance().firePinchReleaseGesture(10, 10);
    }

    /// pinch(scale) is the factor since the gesture BEGAN, not since the last
    /// event, and this pins that so the next port cannot quietly disagree.
    ///
    /// ImageViewer is the reference consumer: it computes currentZoom * scale
    /// and only moves currentZoom in pinchReleased(), so a port that forwards
    /// each event's own increment ends a whole gesture on its last tiny step.
    /// Three ports did exactly that -- the native Windows and Linux ones
    /// forwarded the incremental multiplier their platform reports, and the
    /// JavaScript one forwarded a single wheel notch -- while AppKit
    /// accumulated. The scale a component sees must grow across a gesture.
    @FormTest
    void magnifyScaleIsCumulativeAcrossTheGesture() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        final java.util.List<Float> seen = new java.util.ArrayList<Float>();
        Container gestureCmp = new Container() {
            @Override
            protected boolean pinch(float scale) {
                seen.add(Float.valueOf(scale));
                return true;
            }
        };
        gestureCmp.setPreferredSize(new Dimension(200, 120));
        form.add(BorderLayout.CENTER, gestureCmp);
        form.revalidate();

        int x = gestureCmp.getAbsoluteX() + gestureCmp.getWidth() / 2;
        int y = gestureCmp.getAbsoluteY() + gestureCmp.getHeight() / 2;

        Display.getInstance().firePinchBeginGesture();
        // What a port must send for three equal outward steps: the running
        // product, not 1.1 three times over.
        Display.getInstance().fireMagnifyGesture(x, y, 1.1f);
        Display.getInstance().fireMagnifyGesture(x, y, 1.21f);
        Display.getInstance().fireMagnifyGesture(x, y, 1.331f);
        Display.getInstance().firePinchReleaseGesture(x, y);

        assertEquals(3, seen.size(), "every update should reach the component");
        assertTrue(seen.get(1).floatValue() > seen.get(0).floatValue()
                        && seen.get(2).floatValue() > seen.get(1).floatValue(),
                "the factor must grow across the gesture rather than restart per event; got " + seen);
        assertEquals(1.331f, seen.get(2).floatValue(), 0.0001f,
                "the final value is the factor since the gesture began");
    }
}
