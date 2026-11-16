/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.studio;

import com.codename1.components.InteractionDialog;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.layouts.Layout;

/// A popup, sheet or dialog in the form's own layer that puts the form's
/// native peers (the 3D view) in lightweight mode while it shows.
///
/// A native view sits above everything Codename One paints, so an overlay
/// laid over the 3D view was hidden or torn by it. While the overlay is up
/// the view is drawn as a snapshot; it goes native again on every kind of
/// dismissal, including a tap outside, which also ends in `dispose()`.
final class Overlay extends InteractionDialog {
    private Form owner;

    Overlay(Layout l) {
        super(l);
    }

    Overlay(String title, Layout l) {
        super(title, l);
    }

    @Override
    public void show(int top, int bottom, int left, int right) {
        owner = CN.getCurrentForm();
        peers(owner, true);
        super.show(top, bottom, left, right);
    }

    @Override
    public void dispose() {
        super.dispose();
        peers(owner, false);
        owner = null;
    }

    private static void peers(Container c, boolean lightweight) {
        if (c == null) {
            return;
        }
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof PeerComponent) {
                ((PeerComponent) k).setLightweight(lightweight);
            } else if (k instanceof Container) {
                peers((Container) k, lightweight);
            }
        }
    }
}
