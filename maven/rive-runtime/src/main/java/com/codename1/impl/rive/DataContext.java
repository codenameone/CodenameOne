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

/// Port of rive-runtime's `DataContext`: the view model instance an artboard
/// binds against, and the context of the artboard hosting it. A path that does
/// not resolve against this context's instance is resolved against the parent,
/// so a nested artboard bound to a child instance still reaches its host's
/// properties.
public final class DataContext {
    private final ViewModelInstance main;
    private final DataContext parent;

    public DataContext(ViewModelInstance main, DataContext parent) {
        this.main = main;
        this.parent = parent;
    }

    /// The instance this context binds (upstream `mainViewModelInstance`).
    public ViewModelInstance getMain() {
        return main;
    }

    public DataContext getParent() {
        return parent;
    }

    /// The property value `path` names, here or in a parent context
    /// (upstream `getViewModelProperty`).
    public ViewModelInstanceValue resolve(int[] path) {
        for (DataContext c = this; c != null; c = c.parent) {
            ViewModelInstanceValue v = DataBindContext.resolvePath(c.main, path);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /// The instance `path` names, here or in a parent context: every id after
    /// the first must be a view model property (upstream `getViewModelInstance`).
    public ViewModelInstance resolveInstance(int[] path) {
        if (path == null || path.length == 0) {
            return null;
        }
        for (DataContext c = this; c != null; c = c.parent) {
            ViewModelInstance current = c.main;
            if (current == null || current.getViewModelId() != path[0]) {
                continue;
            }
            for (int i = 1; i < path.length && current != null; i++) {
                ViewModelInstanceValue v = current.propertyValue(path[i]);
                current = v instanceof ViewModelInstanceViewModel ? ((ViewModelInstanceViewModel) v).getReference() : null;
            }
            if (current != null) {
                return current;
            }
        }
        return null;
    }
}
