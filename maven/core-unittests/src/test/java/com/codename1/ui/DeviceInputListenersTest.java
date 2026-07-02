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
}
