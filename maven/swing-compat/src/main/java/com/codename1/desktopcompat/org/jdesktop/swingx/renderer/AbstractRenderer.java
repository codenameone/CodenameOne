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
package com.codename1.desktopcompat.org.jdesktop.swingx.renderer;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.org.jdesktop.swingx.rollover.RolloverRenderer;

/// What the table, list and tree renderers share: the component provider
/// that does the work.
public abstract class AbstractRenderer implements RolloverRenderer, StringValue {

    protected ComponentProvider<?> componentController;

    /// Makes a renderer around a provider, or around
    /// [#createDefaultComponentProvider()] when it is `null`.
    public AbstractRenderer(ComponentProvider<?> provider) {
        componentController = provider != null ? provider : createDefaultComponentProvider();
    }

    public ComponentProvider<?> getComponentProvider() {
        return componentController;
    }

    protected abstract ComponentProvider<?> createDefaultComponentProvider();

    /// The text the renderer shows for a value.
    @Override
    public String getString(Object value) {
        return componentController.getString(value);
    }

    @Override
    public void doClick() {
        if (isEnabled() && componentController instanceof RolloverRenderer) {
            ((RolloverRenderer) componentController).doClick();
        }
    }

    @Override
    public boolean isEnabled() {
        return componentController instanceof RolloverRenderer
                && ((RolloverRenderer) componentController).isEnabled();
    }

    /// Does nothing: there are no UI delegates to refresh.
    public void updateUI() {
    }

    /// Fixes the background of unselected cells.
    public void setBackground(Color background) {
        componentController.getDefaultVisuals().setBackground(background);
    }

    /// Fixes the text color of unselected cells.
    public void setForeground(Color foreground) {
        componentController.getDefaultVisuals().setForeground(foreground);
    }
}
