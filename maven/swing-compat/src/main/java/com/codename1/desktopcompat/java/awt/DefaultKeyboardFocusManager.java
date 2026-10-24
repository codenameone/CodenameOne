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

/// The one keyboard focus manager of the layer.
public class DefaultKeyboardFocusManager extends KeyboardFocusManager {

    public DefaultKeyboardFocusManager() {
    }

    private static Container root(Component c) {
        Container top = null;
        for (Container p = c instanceof Container ? (Container) c : c.getParent(); p != null; p = p.getParent()) {
            top = p;
        }
        return top;
    }

    private static FocusTraversalPolicy policy(Container root) {
        FocusTraversalPolicy p = root.getFocusTraversalPolicy();
        return p != null ? p : getCurrentKeyboardFocusManager().getDefaultFocusTraversalPolicy();
    }

    @Override
    public void focusNextComponent(Component aComponent) {
        if (aComponent == null) {
            return;
        }
        Container root = root(aComponent);
        if (root != null) {
            Component next = policy(root).getComponentAfter(root, aComponent);
            if (next != null) {
                next.requestFocusInWindow();
            }
        }
    }

    @Override
    public void focusPreviousComponent(Component aComponent) {
        if (aComponent == null) {
            return;
        }
        Container root = root(aComponent);
        if (root != null) {
            Component next = policy(root).getComponentBefore(root, aComponent);
            if (next != null) {
                next.requestFocusInWindow();
            }
        }
    }
}
