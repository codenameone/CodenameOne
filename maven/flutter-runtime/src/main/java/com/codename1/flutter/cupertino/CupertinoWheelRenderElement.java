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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.spinner.InternalPickerWidget;

/**
 * Base for the Cupertino wheels, each of which is one of Codename One's 3D spinners.
 *
 * <p>They are the same control: a column of rows that scrolls under a fixed selection
 * band, with the rows away from the band foreshortened. Building a faithful one from
 * scratch -- item extent, magnification, squeeze, momentum, selection tracking -- would
 * have been a piece of work in its own right, and it already exists in
 * {@code com.codename1.ui.spinner}. The only thing missing was that those classes were
 * package private; they are public now.</p>
 *
 * <p>A wheel FILLS the box it is given. Flutter's does, and the gallery relies on it:
 * every picker in the demo is a wheel inside a SizedBox of a stated height.</p>
 */
abstract class CupertinoWheelRenderElement extends RenderElement {

    /** Flutter's default wheel height when nothing constrains it, in logical pixels. */
    private static final double DEFAULT_HEIGHT_LP = 216;

    private Object lastReported;

    CupertinoWheelRenderElement(Widget widget) {
        super(widget);
    }

    /** The spinner this wheel is. */
    protected abstract Container createWheel();

    /** Hands the current value to whatever the Dart asked to be told. */
    protected abstract void report(Object value);

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Container wheel = createWheel();
        if (wheel == null) {
            return null;
        }
        listenForChanges(wheel);
        if (wheel instanceof InternalPickerWidget) {
            lastReported = ((InternalPickerWidget) wheel).getValue();
        }
        return wheel;
    }

    /**
     * Reports the wheel's value as it turns.
     *
     * <p>A spinner is a set of scrollable columns and has no value event of its own, so
     * the scroll is what is listened to and the VALUE is what is compared -- it only
     * changes as a row crosses the selection band, which is exactly when Flutter's
     * {@code onSelectedItemChanged} fires. Without the comparison this would report on
     * every pixel of every drag.</p>
     */
    private void listenForChanges(Container wheel) {
        final Container root = wheel;
        ScrollListener l = new ScrollListener() {
            @Override
            public void scrollChanged(int scrollX, int scrollY, int oldX, int oldY) {
                if (!(root instanceof InternalPickerWidget)) {
                    return;
                }
                Object now = ((InternalPickerWidget) root).getValue();
                if (now == null ? lastReported == null : now.equals(lastReported)) {
                    return;
                }
                lastReported = now;
                report(now);
            }
        };
        attachScrollListeners(wheel, l);
    }

    private void attachScrollListeners(Container c, ScrollListener l) {
        if (c.isScrollableY()) {
            c.addScrollListener(l);
        }
        int count = c.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component child = c.getComponentAt(iter);
            if (child instanceof Container) {
                attachScrollListeners((Container) child, l);
            }
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double w = constraints.hasBoundedWidth()
                ? constraints.maxWidth() : com.codename1.flutter.rendering.Dp.px(320);
        double h = constraints.hasBoundedHeight()
                ? constraints.maxHeight()
                : com.codename1.flutter.rendering.Dp.px(DEFAULT_HEIGHT_LP);
        return constraints.constrain(new Size(w, h));
    }

    @Override
    public void position(int x, int y) {
        super.position(x, y);
        // The spinner lays ITSELF out. Everything else in this runtime is a leaf whose
        // bounds the Flutter pass writes directly, and writing only the outer bounds of a
        // control with columns inside it leaves every column at whatever size it had when
        // it was created -- which is none.
        Component c = component();
        if (c instanceof Container) {
            ((Container) c).layoutContainer();
        }
    }
}
