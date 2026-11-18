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

/// An instance of a view model: one value per property. File level
/// instances are templates; what an artboard binds is a copy
/// ([#copyInstance(RiveFileContext, int)]), as upstream's
/// `File::createViewModelInstance` clones them.
public class ViewModelInstance extends ViewModelInstanceBase {
    private static final int MAX_NESTING = 32;
    private ViewModel viewModel;
    private final ArrayList<ViewModelInstanceValue> values = new ArrayList<ViewModelInstanceValue>();

    public void setViewModel(ViewModel vm) {
        viewModel = vm;
    }

    public ViewModel getViewModel() {
        return viewModel;
    }

    public void addValue(ViewModelInstanceValue v) {
        values.add(v);
        if (viewModel != null && v.getViewModelProperty() == null) {
            v.setViewModelProperty(viewModel.getProperty(v.getViewModelPropertyId()));
        }
    }

    public ArrayList<ViewModelInstanceValue> getValues() {
        return values;
    }

    /// The value of property id `id`, or null.
    public ViewModelInstanceValue propertyValue(int id) {
        for (int i = 0; i < values.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ViewModelInstanceValue v = values.get(i);
            if (v.getViewModelPropertyId() == id) {
                return v;
            }
        }
        return null;
    }

    /// The value of the property named `name`, or null.
    public ViewModelInstanceValue propertyValue(String name) {
        for (int i = 0; i < values.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ViewModelInstanceValue v = values.get(i);
            if (name != null && name.equals(v.getPropertyName())) {
                return v;
            }
        }
        return null;
    }

    /// A deep copy, with view model references and list items resolved
    /// against `file`.
    public ViewModelInstance copyInstance(RiveFileContext file, int depth) {
        ViewModelInstance c = new ViewModelInstance();
        c.setViewModelId(getViewModelId());
        c.setName(getName());
        c.viewModel = viewModel;
        for (int i = 0; i < values.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ViewModelInstanceValue v = values.get(i).copyValue();
            if (v == null) {
                continue;
            }
            c.addValue(v);
            if (depth < MAX_NESTING) {
                v.complete(file, depth + 1);
            }
        }
        return c;
    }
}
