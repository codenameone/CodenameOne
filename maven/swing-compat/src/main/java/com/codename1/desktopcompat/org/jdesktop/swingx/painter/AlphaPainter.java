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
package com.codename1.desktopcompat.org.jdesktop.swingx.painter;

import com.codename1.desktopcompat.java.awt.AlphaComposite;
import com.codename1.desktopcompat.java.awt.Composite;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;

/// A compound painter whose painters draw through one opacity.
///
/// The opacity multiplies whatever the graphics already has.
public class AlphaPainter<T> extends CompoundPainter<T> {

    private float alpha = 1.0f;

    public AlphaPainter() {
    }

    @Override
    protected void doPaint(Graphics2D g, T component, int width, int height) {
        if (alpha <= 0f) {
            return;
        }
        Graphics copy = g.create();
        try {
            if (copy instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) copy;
                if (alpha < 1f) {
                    Composite current = g2.getComposite();
                    float base = current instanceof AlphaComposite ? ((AlphaComposite) current).getAlpha() : 1f;
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, base * alpha));
                }
                super.doPaint(g2, component, width, height);
            }
        } finally {
            copy.dispose();
        }
    }

    public float getAlpha() {
        return alpha;
    }

    /// Sets the opacity; values outside 0 to 1 are brought into it.
    public void setAlpha(float alpha) {
        float old = this.alpha;
        this.alpha = alpha < 0f ? 0f : alpha > 1f ? 1f : alpha;
        setDirty(true);
        firePropertyChange("alpha", Float.valueOf(old), Float.valueOf(this.alpha));
    }
}
