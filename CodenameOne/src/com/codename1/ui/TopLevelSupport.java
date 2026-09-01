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

import com.codename1.ui.geom.Rectangle;
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

    /// Resolves the container that a command activated inside the given component
    /// should be reported to.
    ///
    /// Not the same as `#rootOf(Component)`. A `Dialog` shown inside a window's
    /// layered pane is a parented `Form`, so the top level walk goes past it to the
    /// `Window` -- whose command listeners are somebody else's. The dialog never
    /// heard that its own button had been pressed, which left `lastCommandPressed`
    /// null and the modal wait running forever.
    ///
    /// For every hierarchy that has no hosted dialog in it this returns the very
    /// container `rootOf` would, because `Form` and `Window` both answer
    /// `Container#isCommandHost()` with the unparented test that `getTopLevelContainer`
    /// already uses.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component the command was activated from, may be null
    ///
    /// #### Returns
    ///
    /// the container to notify, or null when the component is detached
    static Container commandHostOf(Component cmp) {
        Component c = cmp;
        while (c != null) {
            if (c instanceof Container && ((Container) c).isCommandHost()) {
                return (Container) c;
            }
            c = c.getParent();
        }
        return null;
    }

    /// Registers a component for animation with the top level it lives in, using the
    /// internal registration that skips the public bookkeeping.
    ///
    /// `registerAnimatedInternal` is package private on both `Form` and `Window` and so
    /// cannot sit on the public `TopLevelContainer` interface -- an interface member
    /// would force it public. It is declared on `Container`, the nearest common
    /// supertype, so the call below dispatches virtually. Callers inside this package
    /// go through here instead of `getComponentForm()`, which is null by design inside
    /// a `Window`.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component whose top level is registered against
    ///
    /// - `a`: the animation to register
    static void registerAnimatedInternal(Component c, Animation a) {
        registerAnimatedInternal(c == null ? null : c.getTopLevelContainer(), a);
    }

    /// The counterpart to `#registerAnimatedInternal(Component, Animation)`.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component whose top level is deregistered from
    ///
    /// - `a`: the animation to deregister
    static void deregisterAnimatedInternal(Component c, Animation a) {
        deregisterAnimatedInternal(c == null ? null : c.getTopLevelContainer(), a);
    }

    /// Registers an animation with an already resolved top level.
    ///
    /// The `Component` overload covers the common case; this one is for callers that
    /// hold the top level in a local, or that register something other than themselves.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to register with, may be null
    ///
    /// - `a`: the animation to register
    static void registerAnimatedInternal(TopLevelContainer top, Animation a) {
        if (top != null) {
            top.asContainer().registerAnimatedInternal(a);
        }
    }

    /// The counterpart to `#registerAnimatedInternal(TopLevelContainer, Animation)`.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to deregister from, may be null
    ///
    /// - `a`: the animation to deregister
    static void deregisterAnimatedInternal(TopLevelContainer top, Animation a) {
        if (top != null) {
            top.asContainer().deregisterAnimatedInternal(a);
        }
    }

    /// The form wide layered pane of the given top level if one exists, without
    /// creating one.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to ask, may be null
    ///
    /// #### Returns
    ///
    /// the layered pane, or null when there is none or no top level
    static Container formLayeredPaneIfExists(TopLevelContainer top) {
        return top == null ? null : top.asContainer().getFormLayeredPaneIfExists();
    }

    /// The content area layered pane of the given top level if one exists, without
    /// creating one.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to ask, may be null
    ///
    /// #### Returns
    ///
    /// the layered pane, or null when there is none or no top level
    static Container layeredPaneIfExists(TopLevelContainer top) {
        return top == null ? null : top.asContainer().getLayeredPaneIfExists();
    }

    /// The height the top level's soft button bar costs, zero for a window.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to measure, may be null
    ///
    /// #### Returns
    ///
    /// the soft button area height
    static int softButtonAreaHeight(TopLevelContainer top) {
        return top == null ? 0 : top.asContainer().softButtonAreaHeight();
    }

    /// The width to measure against for something that will be shown on the given top
    /// level.
    ///
    /// A window is measured by its own size. A form is measured by the display, not by
    /// its own width, because that is the expression every caller evaluated before
    /// windows existed and a form is not always laid out when it is asked.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level, may be null
    ///
    /// #### Returns
    ///
    /// the width to size against
    static int hostWidth(TopLevelContainer top) {
        if (top != null && top.asContainer().isNativeWindow()) {
            return top.asContainer().getWidth();
        }
        return Display.getInstance().getDisplayWidth();
    }

    /// The counterpart to `#hostWidth(TopLevelContainer)`.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level, may be null
    ///
    /// #### Returns
    ///
    /// the height to size against
    static int hostHeight(TopLevelContainer top) {
        if (top != null && top.asContainer().isNativeWindow()) {
            return top.asContainer().getHeight();
        }
        return Display.getInstance().getDisplayHeight();
    }

    /// The top level the user is currently working in: the focused window when one has
    /// focus, otherwise the current form.
    ///
    /// This is what a caller with no component to resolve from should use --
    /// `ToastBar.showMessage`, `InfiniteProgress.showInfiniteBlocking`, the static
    /// `Dialog.show` helpers. Those used to name the current form unconditionally, so
    /// in a multi window application they put their overlay on the main window while
    /// the user was looking at another one.
    ///
    /// #### Returns
    ///
    /// the top level to act on, or null when the application has neither
    static TopLevelContainer current() {
        // Every port that has windows reports focus for them, Mac Catalyst included:
        // its scene delegate calls CN1MacWindowDeliverFocus from sceneDidBecomeActive
        // and sceneWillResignActive, which reaches windowFocusCallback and so
        // Desktop.getFocusedWindow(). Checked because a review read the callback as
        // declared but never invoked, which would have left every overlay routed
        // through here on the main form; the call sites are in
        // CodenameOne_GLSceneDelegate.m, not in the two files that only declare it.
        if (Display.impl != null && Display.impl.getWindowManager() != null) {
            Window focused = Desktop.getInstance().getFocusedWindow();
            if (focused != null && focused.isWindowShowing()) {
                return focused;
            }
        }
        return Display.getInstance().getCurrent();
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

    /// Adds a component to the top level's own layout, outside the content pane. Both
    /// `Form` and `Window` keep that structural add package private rather than on
    /// `TopLevelContainer`, since widening it would hand every caller a way to place
    /// components beside the content pane; `Container` declares the hook they override.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to add to
    ///
    /// - `constraints`: the layout constraint
    ///
    /// - `cmp`: the component to add
    static void addComponentToTopLevel(TopLevelContainer top, Object constraints, Component cmp) {
        if (top != null) {
            top.asContainer().addComponentToTopLevel(constraints, cmp);
        }
    }

    /// The counterpart to
    /// `#addComponentToTopLevel(TopLevelContainer, Object, Component)`.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to remove from
    ///
    /// - `cmp`: the component to remove
    static void removeComponentFromTopLevel(TopLevelContainer top, Component cmp) {
        if (top != null) {
            top.asContainer().removeComponentFromTopLevel(cmp);
        }
    }

    // ---------------------------------------------------------------------------
    // Directional focus traversal, shared by Form and Window.
    //
    // The scan is generic: it walks a root container by absolute coordinates and
    // knows nothing about which kind of top level it came from. It lived on Form,
    // where Window could not reach it -- so every arrow key in a window resolved
    // through Container's inert stubs and moved focus nowhere at all. Moved here
    // rather than copied, so the two cannot drift.
    // ---------------------------------------------------------------------------

    /// Returns true if the given dest component is in the column of the source component
    static boolean isInSameColumn(Component source, Component dest) {
        // workaround for NPE
        if (source == null || dest == null) {
            return false;
        }
        return Rectangle.intersects(source.getAbsoluteX(), 0,
                source.getWidth(), Integer.MAX_VALUE, dest.getAbsoluteX(), dest.getAbsoluteY(),
                dest.getWidth(), dest.getHeight());
    }

    /// Returns true if the given dest component is in the row of the source component
    static boolean isInSameRow(Component source, Component dest) {
        return Rectangle.intersects(0, source.getAbsoluteY(),
                Integer.MAX_VALUE, source.getHeight(), dest.getAbsoluteX(), dest.getAbsoluteY(),
                dest.getWidth(), dest.getHeight());
    }

    static Component findNextFocusHorizontal(Component focused, Component bestCandidate, Container root, boolean right) {
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component current = root.getComponentAt(iter);
            if (current.isFocusable()) {
                if (isInSameRow(focused, current)) {
                    int currentX = current.getAbsoluteX();
                    int focusedX = focused.getAbsoluteX();
                    if (right) {
                        if (focusedX < currentX) {
                            if (bestCandidate != null) {
                                if (bestCandidate.getAbsoluteX() < currentX) {
                                    continue;
                                }
                            }
                            bestCandidate = current;
                        }
                    } else {
                        if (focusedX > currentX) {
                            if (bestCandidate != null) {
                                if (bestCandidate.getAbsoluteX() > currentX) {
                                    continue;
                                }
                            }
                            bestCandidate = current;
                        }
                    }
                }
            }
            if (current instanceof Container && !(((Container) current).isBlockFocus())) {
                bestCandidate = findNextFocusHorizontal(focused, bestCandidate, (Container) current, right);
            }
        }
        return bestCandidate;
    }

    static Component findNextFocusVertical(Component focused, Component bestCandidate, Container root, boolean down) {
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component current = root.getComponentAt(iter);
            if (current.isFocusable()) {
                int currentY = current.getAbsoluteY();
                int focusedY = 0;
                if (focused != null) {
                    focusedY = focused.getAbsoluteY();
                }
                if (down) {
                    if (focusedY < currentY) {
                        if (bestCandidate != null) {
                            boolean exitingInSame = isInSameColumn(focused, bestCandidate);
                            if (bestCandidate.getAbsoluteY() < currentY) {
                                if (exitingInSame) {
                                    continue;
                                }
                                if (isInSameRow(current, bestCandidate) && !isInSameColumn(focused, current)) {
                                    continue;
                                }
                            }
                            if (exitingInSame && isInSameRow(current, bestCandidate)) {
                                continue;
                            }
                        }
                        bestCandidate = current;
                    }
                } else {
                    if (focusedY > currentY) {
                        if (bestCandidate != null) {
                            boolean exitingInSame = isInSameColumn(focused, bestCandidate);
                            if (bestCandidate.getAbsoluteY() > currentY) {
                                if (exitingInSame) {
                                    continue;
                                }
                                if (isInSameRow(current, bestCandidate) && !isInSameColumn(focused, current)) {
                                    continue;
                                }
                            }
                            if (exitingInSame && isInSameRow(current, bestCandidate)) {
                                continue;
                            }
                        }
                        bestCandidate = current;
                    }
                }
            }
            if (current instanceof Container && !(((Container) current).isBlockFocus())) {
                bestCandidate = findNextFocusVertical(focused, bestCandidate, (Container) current, down);
            }
        }
        return bestCandidate;
    }
}
