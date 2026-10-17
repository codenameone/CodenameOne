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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.javax.swing.table.TableCellRenderer;

/// A table cell renderer that delegates to a [ComponentProvider]: a
/// label showing the value's string unless another provider is given.
public class DefaultTableRenderer extends AbstractRenderer implements TableCellRenderer {

    private final TableCellContext cellContext = new TableCellContext();

    public DefaultTableRenderer() {
        this((ComponentProvider<?>) null);
    }

    public DefaultTableRenderer(ComponentProvider<?> componentProvider) {
        super(componentProvider);
    }

    public DefaultTableRenderer(StringValue converter) {
        this(new LabelProvider(converter));
    }

    public DefaultTableRenderer(StringValue converter, int alignment) {
        this(new LabelProvider(converter, alignment));
    }

    public DefaultTableRenderer(StringValue stringValue, IconValue iconValue) {
        this(stringValue, iconValue, SwingConstants.LEADING);
    }

    public DefaultTableRenderer(StringValue stringValue, IconValue iconValue, int alignment) {
        this(new MappedValue(stringValue, iconValue), alignment);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        cellContext.installContext(table, value, row, column, isSelected, hasFocus, true, true);
        return componentController.getRendererComponent(cellContext);
    }

    @Override
    protected ComponentProvider<?> createDefaultComponentProvider() {
        return new LabelProvider();
    }
}
