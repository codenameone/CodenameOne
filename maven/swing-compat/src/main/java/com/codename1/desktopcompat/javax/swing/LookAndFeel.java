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
/// The widgets of the layer are drawn by Codename One with its theme, and
/// no look and feel changes that: one that is set is remembered and handed
/// back, and is never asked to install anything.
public abstract class LookAndFeel {

    public LookAndFeel() {
    }

    public abstract String getName();

    public abstract String getID();

    public abstract String getDescription();

    public abstract boolean isNativeLookAndFeel();

    public abstract boolean isSupportedLookAndFeel();

    /// Never called by the layer.
    public void initialize() {
    }

    /// Never called by the layer.
    public void uninitialize() {
    }

    /// The defaults of this look and feel; `null` unless a subclass has
    /// some. The layer does not read them.
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
