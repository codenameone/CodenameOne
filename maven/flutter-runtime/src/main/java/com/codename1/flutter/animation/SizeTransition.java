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
package com.codename1.flutter.animation;

/**
 * Animates its own size along one axis, clipping its {@code child} — Flutter's
 * {@code SizeTransition}. The {@code sizeFactor} animation drives the visible
 * fraction (0..1) and {@code axisAlignment} anchors the reveal. This pass hosts
 * the child at full size; the animated clip is deferred (see
 * {@link AnimatedChildWidget}).
 */
public class SizeTransition extends AnimatedChildWidget {

    private Object axis;
    private Animation<Double> sizeFactor;
    private Double axisAlignment;

    public void axis(Object v) {
        this.axis = v;
    }

    public void axisAlignment(double v) {
        this.axisAlignment = v;
    }

    public Animation<Double> getSizeFactor() {
        return sizeFactor;
    }

    public void sizeFactor(Animation<Double> v) {
        this.sizeFactor = v;
        listenable(v);
    }

    /**
     * Flutter's own composition: a ClipRect over an Align whose factor along the axis is
     * the animation's value. Align already scales its box to a FRACTION of its child, and
     * the ClipRect hides the part that does not fit yet.
     *
     * <p>This used to hand the child through at full size -- the note said the clip was
     * "deferred" -- so nothing driven by one ever grew or shrank. The mail study's bottom
     * bar is a SizeTransition, which is why it appeared and vanished instead of sliding.</p>
     */
    @Override
    public com.codename1.flutter.Widget build(com.codename1.flutter.BuildContext context) {
        com.codename1.flutter.Widget child = getChild();
        if (child == null || sizeFactor == null) {
            return child;
        }
        double factor = Math.max(0, valueOf(sizeFactor, 1));
        if (factor >= 1) {
            // Fully revealed: hand the child straight through. Align shrink-wraps to its
            // child once given a factor, which throws away a tight height the parent
            // meant it to fill -- a scaffold stretches its bottom bar over the display's
            // bottom inset, and wrapping it unconditionally left that strip unpainted.
            // At rest this widget should change nothing, and now it does not.
            return child;
        }
        boolean horizontal = axis == com.codename1.flutter.Axis.horizontal;
        double along = axisAlignment != null ? axisAlignment.doubleValue() : 0;
        com.codename1.flutter.widgets.Align align = new com.codename1.flutter.widgets.Align();
        align.alignment(horizontal
                ? new com.codename1.flutter.Alignment(along, -1)
                : new com.codename1.flutter.Alignment(-1, along));
        if (horizontal) {
            align.widthFactor(Double.valueOf(factor));
        } else {
            align.heightFactor(Double.valueOf(factor));
        }
        align.child(child);
        com.codename1.flutter.widgets.ClipRect clip =
                new com.codename1.flutter.widgets.ClipRect();
        clip.child(align);
        return clip;
    }
}
