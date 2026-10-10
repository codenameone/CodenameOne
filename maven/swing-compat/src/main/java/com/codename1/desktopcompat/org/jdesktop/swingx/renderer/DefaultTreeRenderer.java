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
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.tree.DefaultMutableTreeNode;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellRenderer;

/// A tree cell renderer that delegates to a [ComponentProvider].
///
/// The default provider is a label with the node's icon and text in one
/// component, where SwingX wraps a second component next to the icon; the
/// wrapping provider and its panel are absent. A node that is a
/// [DefaultMutableTreeNode] is shown by its user object when the renderer
/// was told to unwrap, which is the default.
public class DefaultTreeRenderer extends AbstractRenderer implements TreeCellRenderer {

    private final TreeCellContext cellContext = new TreeCellContext();

    public DefaultTreeRenderer() {
        this((ComponentProvider<?>) null);
    }

    public DefaultTreeRenderer(ComponentProvider<?> componentProvider) {
        super(componentProvider);
    }

    public DefaultTreeRenderer(IconValue iv) {
        this(iv, null);
    }

    public DefaultTreeRenderer(StringValue sv) {
        this(null, sv);
    }

    public DefaultTreeRenderer(IconValue iv, StringValue sv) {
        this(iv, sv, true);
    }

    public DefaultTreeRenderer(IconValue iv, StringValue sv, boolean unwrapUserObject) {
        this(new NodeLabelProvider(iv, sv, unwrapUserObject));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        cellContext.installContext(tree, value, row, 0, selected, hasFocus, expanded, leaf);
        return componentController.getRendererComponent(cellContext);
    }

    @Override
    protected ComponentProvider<?> createDefaultComponentProvider() {
        return new NodeLabelProvider(null, null, true);
    }

    /// A label with the node's icon: the icon converter's, or the default
    /// icon of the node's kind.
    private static final class NodeLabelProvider extends LabelProvider {

        private final IconValue icons;
        private final boolean unwrap;

        NodeLabelProvider(IconValue icons, StringValue strings, boolean unwrap) {
            super(strings);
            this.icons = icons;
            this.unwrap = unwrap;
        }

        private Object plain(Object value) {
            if (unwrap && value instanceof DefaultMutableTreeNode) {
                return ((DefaultMutableTreeNode) value).getUserObject();
            }
            return value;
        }

        @Override
        public String getString(Object value) {
            return super.getString(plain(value));
        }

        @Override
        protected Icon getValueAsIcon(CellContext context) {
            Icon icon = icons != null ? icons.getIcon(plain(context.getValue())) : null;
            if (icon == null) {
                icon = super.getValueAsIcon(context);
            }
            return icon != null ? icon : context.getIcon();
        }
    }
}
