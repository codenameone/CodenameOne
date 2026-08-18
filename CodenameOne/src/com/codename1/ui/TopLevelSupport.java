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
package com.codename1.ui;

import com.codename1.ui.animations.Animation;

/// Package private helpers shared by `Form` and `Window`.
///
/// Java 5 has no default methods, so behaviour common to the two top levels lives
/// here as statics rather than on `TopLevelContainer`.
///
/// @author Shai Almog
final class TopLevelSupport {

    /// Client property recording a layer's depth within a layered pane.
    static final String Z_INDEX_PROP = "cn1$_zIndex";

    /// Client property recording the class a layer belongs to.
    static final String CLASS_PROP = "cn1$_cls";

    private TopLevelSupport() {
    }

    /// Returns the layer belonging to the given class, creating it at the top or the
    /// bottom of the stack if it does not exist yet.
    ///
    /// #### Parameters
    ///
    /// - `layeredPaneImpl`: the container holding the layers
    ///
    /// - `c`: the class owning the layer, or null for the global layer
    ///
    /// - `top`: true to create the layer above the existing ones
    ///
    /// #### Returns
    ///
    /// the layer for the given class
    static Container layeredPane(Container layeredPaneImpl, Class c, boolean top) {
        Container existing = findLayer(layeredPaneImpl, c);
        if (existing != null) {
            return existing;
        }
        // getChildrenAsList(true) rather than iterating the container directly: the
        // latter will not find components while an animation is in progress, and we
        // would then add a duplicate layer.
        java.util.List<Component> children = layeredPaneImpl.getChildrenAsList(true);
        Container cnt = new Container();
        int zIndex = 0;
        int componentCount = children.size();
        if (top) {
            if (componentCount > 0) {
                Integer z = (Integer) children.get(componentCount - 1).getClientProperty(Z_INDEX_PROP);
                if (z != null) {
                    zIndex = z.intValue();
                }
            }
            layeredPaneImpl.add(cnt);
        } else {
            if (componentCount > 0) {
                Integer z = (Integer) children.get(0).getClientProperty(Z_INDEX_PROP);
                if (z != null) {
                    zIndex = z.intValue();
                }
            }
            layeredPaneImpl.addComponent(0, cnt);
        }
        cnt.putClientProperty(CLASS_PROP, c != null ? c.getName() : null);
        cnt.putClientProperty(Z_INDEX_PROP, zIndex);
        return cnt;
    }

    /// Returns the layer belonging to the given class, creating it at an explicit
    /// depth if it does not exist yet.
    ///
    /// #### Parameters
    ///
    /// - `layeredPaneImpl`: the container holding the layers
    ///
    /// - `c`: the class owning the layer, or null for the global layer
    ///
    /// - `zIndex`: the depth at which to create the layer, higher sits in front
    ///
    /// #### Returns
    ///
    /// the layer for the given class
    static Container layeredPane(Container layeredPaneImpl, Class c, int zIndex) {
        Container existing = findLayer(layeredPaneImpl, c);
        if (existing != null) {
            return existing;
        }
        java.util.List<Component> children = layeredPaneImpl.getChildrenAsList(true);
        Container cnt = new Container();
        cnt.putClientProperty(Z_INDEX_PROP, zIndex);
        int len = children.size();
        int insertIndex = -1;
        for (int i = 0; i < len; i++) {
            Component cmp = children.get(i);
            Integer cmpZIndex = (Integer) cmp.getClientProperty(Z_INDEX_PROP);
            int cmpZ = cmpZIndex == null ? 0 : cmpZIndex.intValue();
            if (cmpZ >= zIndex) {
                insertIndex = i;
                break;
            }
        }
        if (insertIndex == -1) {
            layeredPaneImpl.add(cnt);
        } else {
            layeredPaneImpl.addComponent(insertIndex, cnt);
        }
        cnt.putClientProperty(CLASS_PROP, c != null ? c.getName() : null);
        return cnt;
    }

    private static Container findLayer(Container layeredPaneImpl, Class c) {
        java.util.List<Component> children = layeredPaneImpl.getChildrenAsList(true);
        if (c == null) {
            for (Component cmp : children) {
                if (cmp != null && cmp.getClientProperty(CLASS_PROP) == null) {
                    return (Container) cmp;
                }
            }
            return null;
        }
        String n = c.getName();
        for (Component cmp : children) {
            if (cmp != null && n.equals(cmp.getClientProperty(CLASS_PROP))) {
                return (Container) cmp;
            }
        }
        return null;
    }

    /// Resolves the top level containing the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to resolve from, may be null
    ///
    /// #### Returns
    ///
    /// the enclosing top level, or null when the component is detached
    static TopLevelContainer of(Component cmp) {
        if (cmp == null) {
            return null;
        }
        return cmp.getTopLevelContainer();
    }

    /// Resolves the top level containing the given component and returns it as a
    /// `Container`, which is the form the package private top level hooks are
    /// declared in.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to resolve from, may be null
    ///
    /// #### Returns
    ///
    /// the enclosing top level as a container, or null when the component is detached
    static Container rootOf(Component cmp) {
        TopLevelContainer top = of(cmp);
        if (top == null) {
            return null;
        }
        return top.asContainer();
    }

    /// Registers a component for animation with the top level it lives in, using the
    /// internal registration that skips the public bookkeeping.
    ///
    /// `registerAnimatedInternal` is package private on both `Form` and `Window` and so
    /// cannot sit on the public `TopLevelContainer` interface -- an interface member
    /// would force it public. Callers inside this package go through here instead of
    /// `getComponentForm()`, which is null by design inside a `Window`.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component whose top level is registered against
    ///
    /// - `a`: the animation to register
    static void registerAnimatedInternal(Component c, Animation a) {
        TopLevelContainer top = c == null ? null : c.getTopLevelContainer();
        if (top instanceof Form) {
            ((Form) top).registerAnimatedInternal(a);
        } else if (top instanceof Window) {
            ((Window) top).registerAnimatedInternal(a);
        }
    }

    /// The counterpart to `#registerAnimatedInternal(Component, Animation)`.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component whose top level is deregistered from
    ///
    /// - `a`: the animation to deregister
    static void deregisterAnimatedInternal(Component c, Animation a) {
        TopLevelContainer top = c == null ? null : c.getTopLevelContainer();
        if (top instanceof Form) {
            ((Form) top).deregisterAnimatedInternal(a);
        } else if (top instanceof Window) {
            ((Window) top).deregisterAnimatedInternal(a);
        }
    }

    /// Throws when the running platform has no windowing system, so that misuse
    /// fails at the point of construction rather than at the first paint.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if this platform cannot open native windows
    static void requireMultiWindow() {
        if (Display.impl == null || Display.impl.getWindowManager() == null) {
            throw new UnsupportedOperationException(
                    "Multiple native windows are not supported on this platform. "
                    + "Guard with Desktop.isSupported() or CN.isMultiWindowSupported().");
        }
    }
}
