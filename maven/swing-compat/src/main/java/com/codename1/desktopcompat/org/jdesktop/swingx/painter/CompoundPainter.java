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

import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.geom.AffineTransform;

/// Runs several painters one after the other, the first at the bottom.
///
/// Each painter gets a copy of the graphics, so what one does to its
/// state does not reach the next -- unless the clip is preserved, in
/// which case the clip one painter leaves is the clip the following ones
/// draw in. A transform set on the compound applies to all of them.
///
/// Nothing is cached; see [AbstractPainter].
public class CompoundPainter<T> extends AbstractPainter<T> {

    private static final Painter[] NONE = new Painter[0];

    private Painter[] painters = NONE;
    private AffineTransform transform;
    private boolean clipPreserved;
    private boolean checkForDirtyChildPainters = true;

    public CompoundPainter() {
    }

    public CompoundPainter(Painter... painters) {
        this.painters = copy(painters);
    }

    private static Painter[] copy(Painter[] in) {
        if (in == null || in.length == 0) {
            return NONE;
        }
        Painter[] out = new Painter[in.length];
        System.arraycopy(in, 0, out, 0, in.length);
        return out;
    }

    /// Replaces the painters; `null` or none leaves the compound empty.
    public void setPainters(Painter... painters) {
        Painter[] old = getPainters();
        this.painters = copy(painters);
        setDirty(true);
        firePropertyChange("painters", old, getPainters());
    }

    /// A copy of the painters, in painting order.
    public final Painter[] getPainters() {
        return copy(painters);
    }

    public boolean isClipPreserved() {
        return clipPreserved;
    }

    /// Whether the clip a painter leaves behind confines the painters
    /// after it.
    public void setClipPreserved(boolean shouldRestoreState) {
        boolean old = clipPreserved;
        clipPreserved = shouldRestoreState;
        setDirty(true);
        firePropertyChange("clipPreserved", Boolean.valueOf(old), Boolean.valueOf(shouldRestoreState));
    }

    /// A copy of the transform, or `null`.
    public AffineTransform getTransform() {
        return transform == null ? null : new AffineTransform(transform);
    }

    /// Sets a transform applied before the painters run; `null` is none.
    public void setTransform(AffineTransform transform) {
        AffineTransform old = getTransform();
        this.transform = transform == null ? null : new AffineTransform(transform);
        setDirty(true);
        firePropertyChange("transform", old, getTransform());
    }

    /// Validates every painter that is an [AbstractPainter] and becomes
    /// dirty when one of them is.
    @Override
    @SuppressWarnings("unchecked")
    protected void validate(T object) {
        boolean dirty = false;
        for (int i = 0; i < painters.length; i++) {
            Painter p = painters[i];
            if (p instanceof AbstractPainter) {
                AbstractPainter ap = (AbstractPainter) p;
                ap.validate(object);
                if (ap.isDirty()) {
                    dirty = true;
                }
            }
        }
        if (dirty) {
            setDirty(true);
        }
    }

    public boolean isCheckingDirtyChildPainters() {
        return checkForDirtyChildPainters;
    }

    /// Whether [#isDirty()] looks at the painters too.
    public void setCheckingDirtyChildPainters(boolean b) {
        boolean old = checkForDirtyChildPainters;
        checkForDirtyChildPainters = b;
        firePropertyChange("checkingDirtyChildPainters", Boolean.valueOf(old), Boolean.valueOf(b));
    }

    @Override
    protected boolean isDirty() {
        if (super.isDirty()) {
            return true;
        }
        if (checkForDirtyChildPainters) {
            for (int i = 0; i < painters.length; i++) {
                Painter p = painters[i];
                if (p instanceof AbstractPainter && ((AbstractPainter) p).isDirty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void setDirty(boolean d) {
        super.setDirty(d);
    }

    /// Does nothing here and in the painters: there is no cache.
    @Override
    public void clearCache() {
        if (painters == null) {
            return;
        }
        for (int i = 0; i < painters.length; i++) {
            Painter p = painters[i];
            if (p instanceof AbstractPainter) {
                ((AbstractPainter) p).clearCache();
            }
        }
    }

    /// Does nothing: there is no cache.
    public void clearLocalCache() {
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPaint(Graphics2D g, T component, int width, int height) {
        for (int i = 0; i < painters.length; i++) {
            Painter p = painters[i];
            if (p == null) {
                continue;
            }
            Graphics copy = g.create();
            try {
                if (copy instanceof Graphics2D) {
                    Graphics2D temp = (Graphics2D) copy;
                    p.paint(temp, component, width, height);
                    if (clipPreserved) {
                        g.setClip(temp.getClip());
                    }
                }
            } finally {
                copy.dispose();
            }
        }
    }

    @Override
    protected void configureGraphics(Graphics2D g) {
        super.configureGraphics(g);
        if (transform != null) {
            g.transform(transform);
        }
    }

    @Override
    protected boolean shouldUseCache() {
        return false;
    }
}
