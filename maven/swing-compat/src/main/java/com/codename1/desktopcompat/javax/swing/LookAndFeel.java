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
package com.codename1.desktopcompat.javax.swing;

/// The type of a look and feel, so that code which names one, asks
/// [UIManager] for the current one or passes its own compiles and runs.
///
/// The widgets of the layer are drawn by Codename One with its theme, so a
/// look and feel installs no UI delegates. What it does choose is the
/// palette, light or dark, and its defaults: see [UIManager].
public abstract class LookAndFeel {

    public LookAndFeel() {
    }

    public abstract String getName();

    public abstract String getID();

    public abstract String getDescription();

    public abstract boolean isNativeLookAndFeel();

    public abstract boolean isSupportedLookAndFeel();

    /// Called by [UIManager] when this look and feel is set.
    public void initialize() {
    }

    /// Called by [UIManager] when another look and feel replaces this one.
    public void uninitialize() {
    }

    /// The defaults of this look and feel; `null` unless a subclass has
    /// some. [UIManager] copies them into the look and feel defaults when
    /// this look and feel is set.
    public UIDefaults getDefaults() {
        return null;
    }

    public boolean getSupportsWindowDecorations() {
        return false;
    }

    @Override
    public String toString() {
        return "[" + getDescription() + " - " + getClass().getName() + "]";
    }
}
