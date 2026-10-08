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
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.SwingConstants;

/// Shows a cell as a label: the converter's text and, when the converter
/// is an [IconValue], its icon.
public class LabelProvider extends ComponentProvider<JLabel> {

    public LabelProvider() {
        this(null);
    }

    public LabelProvider(StringValue converter) {
        this(converter, SwingConstants.LEADING);
    }

    public LabelProvider(int alignment) {
        this(null, alignment);
    }

    public LabelProvider(StringValue converter, int alignment) {
        super(converter, alignment);
    }

    @Override
    protected JLabel createRendererComponent() {
        return new JRendererLabel();
    }

    @Override
    protected void configureState(CellContext context) {
        if (rendererComponent.getHorizontalAlignment() != getHorizontalAlignment()) {
            rendererComponent.setHorizontalAlignment(getHorizontalAlignment());
        }
    }

    @Override
    protected void format(CellContext context) {
        Icon icon = getValueAsIcon(context);
        if (icon == IconValue.NULL_ICON) {
            icon = null;
        }
        if (rendererComponent.getIcon() != icon) {
            rendererComponent.setIcon(icon);
        }
        String text = getValueAsString(context);
        if (!text.equals(rendererComponent.getText())) {
            rendererComponent.setText(text);
        }
    }
}
