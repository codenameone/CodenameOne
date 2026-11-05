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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Display;
import javafx.scene.Node;

/// When the scroll bars of a view show. On a desktop they always do, as
/// JavaFX's do; on a touch screen they show while the content moves and
/// go a moment after it stops, as a phone's scroll indicator does, rather
/// than sitting over the content for good.
public final class ScrollIndicator implements FrameClock.Pulse {

    private static final long SHOWN_NANOS = 700000000L;

    private final Node[] bars;
    private long until;
    private boolean waiting;
    private double last = Double.NaN;

    /// The bars this indicator shows and hides.
    public ScrollIndicator(Node... bars) {
        this.bars = bars;
    }

    private static boolean touch() {
        return Display.isInitialized() && !Display.getInstance().isDesktop();
    }

    /// Called by the view's layout with how far it is scrolled; answers
    /// whether its bars are to show now.
    public boolean at(double offset) {
        if (!touch()) {
            return true;
        }
        // The first layout places the view, it does not scroll it.
        if (!Double.isNaN(last) && Double.compare(last, offset) != 0) {
            until = FrameClock.nowNanos() + SHOWN_NANOS;
            if (!waiting) {
                waiting = true;
                FrameClock.add(this);
            }
        }
        last = offset;
        return waiting;
    }

    @Override
    public void pulse(long nowNanos) {
        if (nowNanos < until) {
            return;
        }
        waiting = false;
        FrameClock.remove(this);
        for (int i = 0; i < bars.length; i++) {
            bars[i].setVisible(false);
        }
    }
}
