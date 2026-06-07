/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codenameone.inputvalidation.gestures;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;

/// Validates that pointerDragged is dispatched continuously between
/// pointerPressed and pointerReleased. Covers (a) the iOS 26 hover-recognizer
/// regression in PR #5003 (which cancelled the drag mid-stream) and (b) the
/// empty-array NPE fix in 2fef7187 (`pointerDragged guard against empty pointer arrays`).
/// The detector requires at least DRAG_MIN_SAMPLES intermediate samples so a
/// recognizer that fires only the first or last point still fails.
public final class DragStep implements GestureStep {
    private static final int DRAG_MIN_SAMPLES = 3;

    @Override
    public String name() {
        return "drag";
    }

    @Override
    public void install(Container target, Callback callback) {
        final DragSurface surface = new DragSurface();
        surface.setName("cn1iv-drag-target");
        Label hint = new Label("Drag horizontally across this area");
        Container col = new Container(new BorderLayout());
        col.add(BorderLayout.NORTH, hint);
        col.add(BorderLayout.CENTER, surface);
        target.add(BorderLayout.CENTER, col);

        final int[] samples = {0};
        final int[] firstXY = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        final int[] lastXY = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        final boolean[] fired = {false};

        ActionListener dragListener = evt -> {
            if (firstXY[0] == Integer.MIN_VALUE) {
                firstXY[0] = evt.getX();
                firstXY[1] = evt.getY();
            }
            lastXY[0] = evt.getX();
            lastXY[1] = evt.getY();
            samples[0]++;
            surface.update(evt.getX(), evt.getY(), samples[0]);
        };
        ActionListener releaseListener = evt -> {
            if (fired[0]) {
                return;
            }
            if (samples[0] >= DRAG_MIN_SAMPLES) {
                fired[0] = true;
                callback.onDetected("samples=" + samples[0]
                        + ",from=" + firstXY[0] + "x" + firstXY[1]
                        + ",to=" + lastXY[0] + "x" + lastXY[1]);
            }
        };

        // Form-level listeners catch every drag sample regardless of which child
        // happens to be under the finger.
        Form parent = CN.getCurrentForm();
        if (parent != null) {
            parent.addPointerDraggedListener(dragListener);
            parent.addPointerReleasedListener(releaseListener);
        }
    }

    private static final class DragSurface extends Component {
        private int lastX = -1;
        private int lastY = -1;
        private int samples;

        DragSurface() {
            getAllStyles().setBgColor(0x1f2937);
            getAllStyles().setBgTransparency(255);
            getAllStyles().setFgColor(0xfbbf24);
            getAllStyles().setMargin(16, 16, 16, 16);
            getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));
        }

        void update(int absX, int absY, int sampleCount) {
            this.lastX = absX - getAbsoluteX();
            this.lastY = absY - getAbsoluteY();
            this.samples = sampleCount;
            repaint();
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(
                    CN.convertToPixels(60f),
                    CN.convertToPixels(40f));
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(0x1f2937);
            g.fillRect(getX(), getY(), getWidth(), getHeight());
            if (this.lastX >= 0) {
                g.setColor(0xfbbf24);
                int r = CN.convertToPixels(3f);
                g.fillArc(getX() + this.lastX - r, getY() + this.lastY - r, r * 2, r * 2, 0, 360);
            }
            g.setColor(0xfbbf24);
            g.drawString("samples=" + this.samples, getX() + 8, getY() + 8);
        }
    }
}
