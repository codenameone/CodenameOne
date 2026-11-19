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

/// A view model: its properties and the instances the file defines.
public class ViewModel extends ViewModelBase {
    private final ArrayList<ViewModelProperty> properties = new ArrayList<ViewModelProperty>();
    private final ArrayList<ViewModelInstance> instances = new ArrayList<ViewModelInstance>();

    public void addProperty(ViewModelProperty p) {
        properties.add(p);
    }

    public void addInstance(ViewModelInstance i) {
        instances.add(i);
    }

    /// The property with id (index) `id`, or null.
    public ViewModelProperty getProperty(int id) {
        return id >= 0 && id < properties.size() ? properties.get(id) : null;
    }

    public ArrayList<ViewModelProperty> getProperties() {
        return properties;
    }

    /// The instance at `index`, or null.
    public ViewModelInstance getInstance(int index) {
        return index >= 0 && index < instances.size() ? instances.get(index) : null;
    }

    /// The instance named `name`, or null.
    public ViewModelInstance getInstanceByName(String name) {
        for (int i = 0; i < instances.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ViewModelInstance vi = instances.get(i);
            if (name != null && name.equals(vi.getName())) {
                return vi;
            }
        }
        return null;
    }

    public int getInstanceCount() {
        return instances.size();
    }

    /// Upstream `ViewModel::defaultInstance`: the first instance.
    public ViewModelInstance defaultInstance() {
        return getInstance(0);
    }
}
