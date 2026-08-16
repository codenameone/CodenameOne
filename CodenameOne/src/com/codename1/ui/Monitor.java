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

import com.codename1.ui.geom.Rectangle;

/// One physical display attached to the desktop.
///
/// A `Window` sits on exactly one monitor at a time and takes its scale and density
/// from that monitor, so two windows of the same application can legitimately render
/// at different scales. Obtain instances through `Desktop#getMonitors()`.
///
/// Instances are snapshots. A monitor that is unplugged, moved or has its resolution
/// changed produces a fresh set, announced through
/// `Desktop#addMonitorListener(com.codename1.ui.events.ActionListener)`.
///
/// @author Shai Almog
public final class Monitor {

    private final int index;
    private final Rectangle bounds;
    private final Rectangle workArea;
    private final int density;
    private final double scale;
    private final int dotsPerInch;
    private final String name;
    private final boolean primary;

    Monitor(int index, Rectangle bounds, Rectangle workArea, int density, double scale,
            int dotsPerInch, String name, boolean primary) {
        this.index = index;
        this.bounds = bounds;
        this.workArea = workArea;
        this.density = density;
        this.scale = scale;
        this.dotsPerInch = dotsPerInch;
        this.name = name;
        this.primary = primary;
    }

    int getIndex() {
        return index;
    }

    /// Returns the monitor's full area in desktop coordinates.
    ///
    /// Desktop coordinates span every monitor, so a secondary display placed left of
    /// or above the primary one legitimately has a negative origin.
    ///
    /// #### Returns
    ///
    /// a copy of the monitor bounds
    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    /// Returns the part of the monitor that is actually usable by a window, with the
    /// task bar, dock and any reserved panels excluded.
    ///
    /// Prefer this over `#getBounds()` when placing or maximising a window.
    ///
    /// #### Returns
    ///
    /// a copy of the usable area
    public Rectangle getWorkArea() {
        return new Rectangle(workArea);
    }

    /// Returns the density bucket of this monitor, as one of the `Display` density
    /// constants.
    ///
    /// #### Returns
    ///
    /// the density constant
    public int getDensity() {
        return density;
    }

    /// Returns the backing scale of this monitor: one for a conventional display, two
    /// for a high resolution one, and fractional values on platforms that allow them.
    ///
    /// #### Returns
    ///
    /// the scale factor
    public double getScale() {
        return scale;
    }

    /// Returns the resolution of this monitor in dots per inch.
    ///
    /// #### Returns
    ///
    /// the dots per inch
    public int getDotsPerInch() {
        return dotsPerInch;
    }

    /// Returns a name for this monitor suitable for showing to a person.
    ///
    /// #### Returns
    ///
    /// the monitor name
    public String getName() {
        return name;
    }

    /// Indicates whether this is the primary monitor, the one the platform treats as
    /// the origin of the desktop.
    ///
    /// #### Returns
    ///
    /// true if this is the primary monitor
    public boolean isPrimary() {
        return primary;
    }

    /// {@inheritDoc}
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Monitor)) {
            return false;
        }
        Monitor o = (Monitor) other;
        return index == o.index && bounds.equals(o.bounds);
    }

    /// {@inheritDoc}
    @Override
    public int hashCode() {
        return index * 31 + bounds.hashCode();
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "Monitor[" + index + " " + name + " " + bounds + " scale=" + scale + "]";
    }
}
