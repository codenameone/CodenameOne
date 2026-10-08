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
package com.codename1.desktopcompat.java.awt;

import java.util.ArrayList;

/// Traverses the components of a container in the order they were added,
/// depth first.
///
/// A component takes part when it is visible, enabled, focusable and
/// answers `true` from `cn1FocusTraversable()`: it is shown by a Codename
/// One widget that takes the focus, or has key or focus listeners, or key
/// bindings of its own. Traversal wraps around at both ends; focus cycle
/// roots below the container are not treated specially.
public class ContainerOrderFocusTraversalPolicy extends FocusTraversalPolicy {

    private boolean implicitDownCycleTraversal = true;

    public ContainerOrderFocusTraversalPolicy() {
    }

    private void collect(Container c, ArrayList<Component> out) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponent(i);
            if (!k.isVisible()) {
                continue;
            }
            if (accept(k)) {
                out.add(k);
            }
            if (k instanceof Container) {
                collect((Container) k, out);
            }
        }
    }

    private ArrayList<Component> order(Container c) {
        if (c == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }
        ArrayList<Component> out = new ArrayList<Component>();
        collect(c, out);
        return out;
    }

    @Override
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        ArrayList<Component> l = order(aContainer);
        if (l.isEmpty()) {
            return null;
        }
        int i = l.indexOf(aComponent);
        return l.get(i < 0 || i == l.size() - 1 ? 0 : i + 1);
    }

    @Override
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        ArrayList<Component> l = order(aContainer);
        if (l.isEmpty()) {
            return null;
        }
        int i = l.indexOf(aComponent);
        return l.get(i <= 0 ? l.size() - 1 : i - 1);
    }

    @Override
    public Component getFirstComponent(Container aContainer) {
        ArrayList<Component> l = order(aContainer);
        return l.isEmpty() ? null : l.get(0);
    }

    @Override
    public Component getLastComponent(Container aContainer) {
        ArrayList<Component> l = order(aContainer);
        return l.isEmpty() ? null : l.get(l.size() - 1);
    }

    @Override
    public Component getDefaultComponent(Container aContainer) {
        return getFirstComponent(aContainer);
    }

    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }

    public boolean getImplicitDownCycleTraversal() {
        return implicitDownCycleTraversal;
    }

    protected boolean accept(Component aComponent) {
        return aComponent.isVisible() && aComponent.isDisplayable() && aComponent.isEnabled()
                && aComponent.isFocusable() && aComponent.cn1FocusTraversable();
    }
}
