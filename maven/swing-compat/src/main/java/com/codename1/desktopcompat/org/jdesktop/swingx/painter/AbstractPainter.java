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

import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.RenderingHints;
import com.codename1.desktopcompat.org.jdesktop.beans.AbstractBean;

/// The base of the painters: it carries the properties every painter has,
/// prepares the graphics and leaves the drawing to [#doPaint].
///
/// ## What differs from SwingX
///
///  - Nothing is cached. [#setCacheable(boolean)] is recorded,
///    [#shouldUseCache()] answers `false` and [#clearCache()] does nothing:
///    a painter draws every time it is asked to.
///  - There are no image filters, so the filter accessors are absent.
///  - The painter is not copied before it is handed the graphics. `paint`
///    draws with the graphics it is given, as the [Painter] contract
///    allows; callers that need the state afterwards pass a copy.
public abstract class AbstractPainter<T> extends AbstractBean implements Painter<T> {

    /// How an image is resampled when it is drawn scaled.
    public enum Interpolation {
        Bicubic,
        Bilinear,
        NearestNeighbor
    }

    private boolean visible = true;
    private boolean antialiasing = true;
    private boolean cacheable;
    private boolean dirty;
    private Interpolation interpolation = Interpolation.NearestNeighbor;

    public AbstractPainter() {
    }

    /// `cacheable` is recorded only; see the class comment.
    public AbstractPainter(boolean cacheable) {
        this.cacheable = cacheable;
    }

    public boolean isAntialiasing() {
        return antialiasing;
    }

    public void setAntialiasing(boolean value) {
        boolean old = antialiasing;
        antialiasing = value;
        if (old != value) {
            setDirty(true);
        }
        firePropertyChange("antialiasing", Boolean.valueOf(old), Boolean.valueOf(value));
    }

    public Interpolation getInterpolation() {
        return interpolation;
    }

    /// Sets the interpolation hint; `null` means nearest neighbor.
    public void setInterpolation(Interpolation value) {
        Interpolation old = interpolation;
        interpolation = value == null ? Interpolation.NearestNeighbor : value;
        if (old != interpolation) {
            setDirty(true);
        }
        firePropertyChange("interpolation", old, interpolation);
    }

    public boolean isVisible() {
        return visible;
    }

    /// An invisible painter paints nothing.
    public void setVisible(boolean visible) {
        boolean old = this.visible;
        this.visible = visible;
        if (old != visible) {
            setDirty(true);
        }
        firePropertyChange("visible", Boolean.valueOf(old), Boolean.valueOf(visible));
    }

    public boolean isCacheable() {
        return cacheable;
    }

    /// Recorded only: nothing is cached.
    public void setCacheable(boolean cacheable) {
        boolean old = this.cacheable;
        this.cacheable = cacheable;
        firePropertyChange("cacheable", Boolean.valueOf(old), Boolean.valueOf(cacheable));
    }

    /// Does nothing: there is no cache.
    public void clearCache() {
    }

    /// Called before every paint; a subclass checks its state against
    /// `object` here and calls [#setDirty(boolean)] when it changed.
    protected void validate(T object) {
    }

    /// Whether a property changed since the painter last painted.
    protected boolean isDirty() {
        return dirty;
    }

    protected void setDirty(boolean d) {
        boolean old = dirty;
        dirty = d;
        firePropertyChange("dirty", Boolean.valueOf(old), Boolean.valueOf(d));
    }

    /// Always `false`.
    protected boolean shouldUseCache() {
        return false;
    }

    /// Sets the rendering hints of the antialiasing and interpolation
    /// properties on the graphics about to be painted with.
    protected void configureGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antialiasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        Object hint;
        if (interpolation == Interpolation.Bicubic) {
            hint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
        } else if (interpolation == Interpolation.Bilinear) {
            hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
        } else {
            hint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
    }

    /// Draws into `(0, 0, width, height)`.
    protected abstract void doPaint(Graphics2D g, T object, int width, int height);

    @Override
    public final void paint(Graphics2D g, T object, int width, int height) {
        if (g == null) {
            throw new NullPointerException("The Graphics2D must be supplied");
        }
        if (!visible || width < 1 || height < 1) {
            return;
        }
        configureGraphics(g);
        validate(object);
        doPaint(g, object, width, height);
        if (isDirty()) {
            setDirty(false);
        }
    }
}
