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

import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.SwingConstants;

/// Owns the one component a renderer paints its cells with, and sets it
/// up for a cell: first the visuals every cell gets, then the content.
public abstract class ComponentProvider<T extends JComponent> {

    protected T rendererComponent;
    protected DefaultVisuals<T> defaultVisuals;
    protected int alignment;
    protected StringValue formatter;

    public ComponentProvider() {
        this(null, SwingConstants.LEADING);
    }

    public ComponentProvider(StringValue converter) {
        this(converter, SwingConstants.LEADING);
    }

    /// Makes a provider showing values through `converter`
    /// ([StringValues#TO_STRING] when `null`) with the given horizontal
    /// alignment.
    public ComponentProvider(StringValue converter, int alignment) {
        this.alignment = alignment;
        this.formatter = converter != null ? converter : StringValues.TO_STRING;
        rendererComponent = createRendererComponent();
        defaultVisuals = createDefaultVisuals();
    }

    /// The component, set up for the cell `context` describes; with a
    /// `null` context the component as it was left.
    public T getRendererComponent(CellContext context) {
        if (context != null) {
            configureVisuals(context);
            configureContent(context);
        }
        return rendererComponent;
    }

    public void setHorizontalAlignment(int alignment) {
        this.alignment = alignment;
    }

    public int getHorizontalAlignment() {
        return alignment;
    }

    public void setStringValue(StringValue formatter) {
        this.formatter = formatter != null ? formatter : StringValues.TO_STRING;
    }

    public StringValue getStringValue() {
        return formatter;
    }

    /// The text this provider shows for a value.
    public String getString(Object value) {
        return formatter.getString(value);
    }

    protected String getValueAsString(CellContext context) {
        return getString(context.getValue());
    }

    /// The icon for the context's value when the converter is an
    /// [IconValue], else `null`.
    protected Icon getValueAsIcon(CellContext context) {
        if (formatter instanceof IconValue) {
            return ((IconValue) formatter).getIcon(context.getValue());
        }
        return null;
    }

    protected void configureVisuals(CellContext context) {
        defaultVisuals.configureVisuals(rendererComponent, context);
    }

    protected void configureContent(CellContext context) {
        configureState(context);
        format(context);
    }

    /// Shows the context's value in the component.
    protected abstract void format(CellContext context);

    /// Sets the properties that are this provider's own, such as the
    /// alignment.
    protected abstract void configureState(CellContext context);

    protected abstract T createRendererComponent();

    protected DefaultVisuals<T> createDefaultVisuals() {
        return new DefaultVisuals<T>();
    }

    protected DefaultVisuals<T> getDefaultVisuals() {
        return defaultVisuals;
    }

    /// Does nothing: there are no UI delegates to refresh.
    public void updateUI() {
    }
}
