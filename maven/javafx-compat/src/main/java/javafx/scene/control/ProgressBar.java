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
package javafx.scene.control;

import com.codename1.ui.Component;

import javafx.geometry.Insets;

/// A bar that fills with progress, shown as a Codename One `Slider` the
/// user cannot move.
///
/// A negative progress is indeterminate and runs the native slider's
/// endless animation; from 0 to 1 the bar is filled in thousandths.
public class ProgressBar extends ProgressIndicator {

    private static final int STEPS = 1000;
    private static final double DEFAULT_WIDTH = 100;

    /// Creates an indeterminate progress bar.
    public ProgressBar() {
        this(INDETERMINATE_PROGRESS);
    }

    /// Creates a progress bar at a progress.
    public ProgressBar(double progress) {
        super(progress);
        getStyleClass().setAll("progress-bar");
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.Slider s = new com.codename1.ui.Slider();
        s.setMinValue(0);
        s.setMaxValue(STEPS);
        s.setEditable(false);
        return s;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (!(c instanceof com.codename1.ui.Slider)) {
            return;
        }
        com.codename1.ui.Slider s = (com.codename1.ui.Slider) c;
        boolean unknown = isIndeterminate();
        if (s.isInfinite() != unknown) {
            s.setInfinite(unknown);
        }
        if (!unknown) {
            double p = Math.min(1, getProgress());
            s.setProgress((int) Math.round(p * STEPS));
        }
    }

    @Override
    boolean drawsDisc() {
        return false;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return Math.max(super.computePrefWidth(height), in.getLeft() + DEFAULT_WIDTH + in.getRight());
    }
}
