/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.layouts;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;


/// Abstract class that can be used to arrange components in a container using
/// a predefined algorithm. This class may be implemented externally and is similar
/// in spirit to the AWT/Swing layout managers.
///
/// @author Chen Fishbein
public abstract class Layout {

    /// Utility method
    ///
    /// #### Parameters
    ///
    /// - `components`
    ///
    /// - `offset`
    private static int updateTabIndicesImpl(Component[] components, int offset) {
        int len = components.length;
        int idx = offset;
        for (int i = 0; i < len; i++) {
            Component cmp = components[i];
            if (cmp != null) {
                int prefIdx = cmp.getPreferredTabIndex();
                if (prefIdx == 0) {
                    cmp.setTabIndex(idx++);
                } else {
                    cmp.setTabIndex(prefIdx);
                }
                if (cmp instanceof Container) {
                    idx = ((Container) cmp).updateTabIndices(idx);
                }
            }
        }
        return idx;
    }

    /// Layout the given parent container children
    ///
    /// #### Parameters
    ///
    /// - `parent`: the given parent container
    public abstract void layoutContainer(Container parent);

    /// Returns the container preferred size
    ///
    /// #### Parameters
    ///
    /// - `parent`: the parent container
    ///
    /// #### Returns
    ///
    /// the container preferred size
    public abstract Dimension getPreferredSize(Container parent);

    /// Some layouts can optionally track the addition of elements with meta-data
    /// that allows the user to "hint" on object positioning.
    ///
    /// #### Parameters
    ///
    /// - `value`: optional meta data information, like alignment orientation
    ///
    /// - `comp`: the added component to the layout
    ///
    /// - `c`: the parent container
    public void addLayoutComponent(Object value, Component comp, Container c) {
        if (value != null) {
            throw new IllegalStateException("Layout doesn't support adding with arguments: " + getClass().getName());
        }
    }

    /// Removes the component from the layout this operation is only useful if the
    /// layout maintains references to components within it
    ///
    /// #### Parameters
    ///
    /// - `comp`: the removed component from layout
    public void removeLayoutComponent(Component comp) {
    }

    /// Returns the optional component constraint
    ///
    /// #### Parameters
    ///
    /// - `comp`: the component whose constraint should be returned
    ///
    /// #### Returns
    ///
    /// the optional component constraint
    public Object getComponentConstraint(Component comp) {
        return null;
    }

    public Object cloneConstraint(Object constraint) {
        return constraint;
    }

    /// This method returns true if the Layout allows Components to
    /// Overlap.
    ///
    /// #### Returns
    ///
    /// true if Components may intersect in this layout
    public boolean isOverlapSupported() {
        return false;
    }

    /// {@inheritDoc}
    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass();
    }

    /// {@inheritDoc}
    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }

    /// If this method returns true, the addLayoutComponent method will be called when replacing a
    /// layout for every component within the container
    ///
    /// #### Returns
    ///
    /// false by default
    public boolean isConstraintTracking() {
        return false;
    }

    /// Some layout managers can obscure their child components in some cases this
    /// returns true if the basic underpinnings are in place for that. This method
    /// doesn't take padding/margin etc. into account since that is checked by the
    /// caller
    ///
    /// #### Parameters
    ///
    /// - `parent`: parent container
    ///
    /// #### Returns
    ///
    /// true if there is a chance that this layout manager can fully obscure the background, when in doubt return false...
    public boolean obscuresPotential(Container parent) {
        return false;
    }

    /// If a layout specifies a different traversal order of its components than the
    /// component index, then it should override this method to return true, and
    /// it should also override `#getChildrenInTraversalOrder(com.codename1.ui.Container)`
    /// to set the tab indices of a container's children.
    ///
    /// #### Parameters
    ///
    /// - `parent`: The parent component.
    ///
    /// #### Returns
    ///
    /// True if this layout overrides tab traversal order.
    public boolean overridesTabIndices(Container parent) {
        return false;
    }

    /// Updates the tab traversal order
    ///
    /// #### Parameters
    ///
    /// - `parent`
    ///
    /// - `offset`
    public final int updateTabIndices(Container parent, int offset) {
        if (overridesTabIndices(parent)) {
            Component[] cmps = getChildrenInTraversalOrder(parent);
            return updateTabIndicesImpl(cmps, offset);
        }
        return offset;
    }

    /// Gets the children of the parent container in the order that they should
    /// be traversed when tabbing through a form.
    ///
    /// This should only be overridden if the Layout defines a different traversal order
    /// than the standard index order.
    ///
    /// Layouts that implement this method, should override the `#overridesTabIndices(com.codename1.ui.Container)`
    /// method to return true.
    ///
    /// #### Parameters
    ///
    /// - `parent`
    ///
    /// #### Returns
    ///
    /// Array of Components in the order
    protected Component[] getChildrenInTraversalOrder(Container parent) {
        return null;
    }

}
