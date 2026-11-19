/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import java.util.ArrayList;

/// An object that lives in an artboard: it has a parent, takes part in the
/// dependency graph and is updated when it is dirty. Mirrors rive-runtime's
/// `Component`.
public class Component extends ComponentBase {
    public static final int STATUS_OK = 0;
    public static final int STATUS_MISSING = 1;
    public static final int STATUS_INVALID = 2;

    public static final int DIRT_NONE = 0;
    public static final int DIRT_COLLAPSED = 1;
    public static final int DIRT_DEPENDENTS = 1 << 1;
    public static final int DIRT_COMPONENTS = 1 << 2;
    public static final int DIRT_DRAW_ORDER = 1 << 3;
    public static final int DIRT_PATH = 1 << 4;
    public static final int DIRT_SKIN = 1 << 4;
    public static final int DIRT_TEXT_SHAPE = 1 << 4;
    public static final int DIRT_VERTICES = 1 << 5;
    public static final int DIRT_TRANSFORM = 1 << 6;
    public static final int DIRT_WORLD_TRANSFORM = 1 << 7;
    public static final int DIRT_RENDER_OPACITY = 1 << 8;
    public static final int DIRT_PAINT = 1 << 9;
    public static final int DIRT_STOPS = 1 << 10;
    public static final int DIRT_LAYOUT_STYLE = 1 << 11;
    public static final int DIRT_BINDINGS = 1 << 12;
    public static final int DIRT_CLIPPING = 1 << 15;
    public static final int DIRT_FILTHY = 0xFFFE;

    protected Artboard artboard;
    protected ContainerComponent parent;
    private ArrayList<Component> dependents;
    int graphOrder;
    protected int dirt = DIRT_FILTHY;

    public Artboard getArtboard() {
        return artboard;
    }

    /// For runtime-only components that are not read from the file.
    final void setArtboard(Artboard a) {
        artboard = a;
    }

    public ContainerComponent getParent() {
        return parent;
    }

    /// Resolves the parent and joins its children. The artboard is its own root.
    public int onAddedDirty(Artboard context) {
        artboard = context;
        if (this == context) { // NOPMD CompareObjectsWithEquals - identity is the intent
            return STATUS_OK;
        }
        Core p = context.resolve(mParentId);
        if (!(p instanceof ContainerComponent)) {
            return STATUS_MISSING;
        }
        parent = (ContainerComponent) p;
        parent.addChild(this);
        return STATUS_OK;
    }

    /// Called once every object of the artboard has run [#onAddedDirty(Artboard)].
    public int onAddedClean(Artboard context) {
        return STATUS_OK;
    }

    /// Declares the components whose update this one must follow.
    public void buildDependencies() {
    }

    /// Recomputes whatever `value` says is stale.
    public void update(int value) {
    }

    public final void addDependent(Component dependent) {
        if (dependents == null) {
            dependents = new ArrayList<Component>();
        }
        if (!dependents.contains(dependent)) {
            dependents.add(dependent);
        }
    }

    public final ArrayList<Component> getDependents() {
        return dependents;
    }

    public final int getGraphOrder() {
        return graphOrder;
    }

    public final boolean hasDirt(int flag) {
        return (dirt & flag) != 0;
    }

    public static boolean hasDirt(int value, int flag) {
        return (value & flag) != 0;
    }

    public boolean isCollapsed() {
        return (dirt & DIRT_COLLAPSED) != 0;
    }

    /// Hook for subclasses reacting to a change of their own dirt.
    protected void onDirty(int value) {
    }

    public boolean addDirt(int value) {
        return addDirt(value, false);
    }

    /// Marks `value` stale; with `recurse` the dependents get the same dirt.
    public boolean addDirt(int value, boolean recurse) {
        if ((dirt & value) == value) {
            return false;
        }
        dirt |= value;
        onDirty(dirt);
        if (artboard != null) {
            artboard.onComponentDirty(this);
        }
        if (recurse && dependents != null) {
            for (int i = 0; i < dependents.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                dependents.get(i).addDirt(value, true);
            }
        }
        return true;
    }

    /// Hides or shows this component (and so everything that depends on it).
    public boolean collapse(boolean value) {
        if (isCollapsed() == value) {
            return false;
        }
        if (value) {
            dirt |= DIRT_COLLAPSED;
        } else {
            dirt &= ~DIRT_COLLAPSED;
        }
        onDirty(dirt);
        if (artboard != null) {
            artboard.onComponentDirty(this);
        }
        return true;
    }

    final int peekDirt() {
        return dirt;
    }

    final int takeDirt() {
        int d = dirt;
        dirt &= DIRT_COLLAPSED;
        return d;
    }
}
