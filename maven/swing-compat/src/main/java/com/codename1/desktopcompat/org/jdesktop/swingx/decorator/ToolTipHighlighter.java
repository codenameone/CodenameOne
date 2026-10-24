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
package com.codename1.desktopcompat.org.jdesktop.swingx.decorator;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;

/// Sets the tool tip text of the cell's component to the cell's text, or
/// to what a [StringValue] makes of the cell's value.
///
/// This layer records tool tip texts and shows none, so the effect is
/// what `getToolTipText()` of the renderer component answers.
public class ToolTipHighlighter extends AbstractHighlighter {

    private StringValue toolTipValue;

    public ToolTipHighlighter() {
        this((HighlightPredicate) null);
    }

    public ToolTipHighlighter(StringValue toolTipValue) {
        this(null, toolTipValue);
    }

    public ToolTipHighlighter(HighlightPredicate predicate) {
        this(predicate, null);
    }

    public ToolTipHighlighter(HighlightPredicate predicate, StringValue toolTipValue) {
        super(predicate);
        this.toolTipValue = toolTipValue;
    }

    public StringValue getToolTipValue() {
        return toolTipValue;
    }

    public void setToolTipValue(StringValue toolTipValue) {
        if (areEqual(toolTipValue, this.toolTipValue)) {
            return;
        }
        this.toolTipValue = toolTipValue;
        fireStateChanged();
    }

    @Override
    protected boolean canHighlight(Component component, ComponentAdapter adapter) {
        return component instanceof JComponent;
    }

    @Override
    protected Component doHighlight(Component component, ComponentAdapter adapter) {
        if (component instanceof JComponent) {
            String text = toolTipValue != null ? toolTipValue.getString(adapter.getValue()) : adapter.getString();
            ((JComponent) component).setToolTipText(text);
        }
        return component;
    }
}
