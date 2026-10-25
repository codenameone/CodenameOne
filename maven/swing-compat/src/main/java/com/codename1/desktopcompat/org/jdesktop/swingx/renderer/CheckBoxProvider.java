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

import com.codename1.desktopcompat.javax.swing.AbstractButton;
import com.codename1.desktopcompat.javax.swing.SwingConstants;

/// Shows a cell as a check box, checked for `Boolean.TRUE` or for what the
/// converter says when it is a [BooleanValue]. The text is empty unless a
/// converter is given.
public class CheckBoxProvider extends ComponentProvider<AbstractButton> {

    private boolean borderPainted;

    public CheckBoxProvider() {
        this(null);
    }

    public CheckBoxProvider(StringValue stringValue) {
        this(stringValue, SwingConstants.CENTER);
    }

    public CheckBoxProvider(StringValue stringValue, int alignment) {
        super(stringValue == null ? StringValues.EMPTY : stringValue, alignment);
        borderPainted = true;
    }

    public boolean isBorderPainted() {
        return borderPainted;
    }

    public void setBorderPainted(boolean borderPainted) {
        this.borderPainted = borderPainted;
    }

    @Override
    protected void format(CellContext context) {
        boolean on = getValueAsBoolean(context);
        if (rendererComponent.isSelected() != on) {
            rendererComponent.setSelected(on);
        }
        String text = getValueAsString(context);
        if (!text.equals(rendererComponent.getText())) {
            rendererComponent.setText(text);
        }
    }

    protected boolean getValueAsBoolean(CellContext context) {
        if (formatter instanceof BooleanValue) {
            return ((BooleanValue) formatter).getBoolean(context.getValue());
        }
        return Boolean.TRUE.equals(context.getValue());
    }

    @Override
    protected void configureState(CellContext context) {
        if (rendererComponent.getHorizontalAlignment() != getHorizontalAlignment()) {
            rendererComponent.setHorizontalAlignment(getHorizontalAlignment());
        }
    }

    @Override
    protected AbstractButton createRendererComponent() {
        return new JRendererCheckBox();
    }
}
