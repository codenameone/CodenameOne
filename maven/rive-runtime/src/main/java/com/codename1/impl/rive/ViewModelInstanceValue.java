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

/// The value of one property in a view model instance. Data binds that read
/// it are its dependents; a change marks them dirty.
public class ViewModelInstanceValue extends ViewModelInstanceValueBase {
    private ViewModelProperty property;
    private final ArrayList<DataBind> dependents = new ArrayList<DataBind>();
    private int changeCount;

    public ViewModelProperty getViewModelProperty() {
        return property;
    }

    public void setViewModelProperty(ViewModelProperty p) {
        property = p;
    }

    /// The property's name, or null.
    public String getPropertyName() {
        return property == null ? null : property.getName();
    }

    public void addDependent(DataBind b) {
        if (!dependents.contains(b)) {
            dependents.add(b);
        }
    }

    public void removeDependent(DataBind b) {
        dependents.remove(b);
    }

    /// Counts changes, so a condition can tell the value changed.
    public int getChangeCount() {
        return changeCount;
    }

    /// Marks every dependent bind dirty.
    protected void valueChanged() {
        changeCount++;
        for (int i = 0; i < dependents.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            dependents.get(i).sourceChanged();
        }
    }

    /// Reads the value into `out`.
    public void read(DataValue out) {
        out.type = DataValue.NONE;
    }

    /// Writes `in` into the value, coercing it to this type.
    public void write(DataValue in) {
    }

    /// A copy of this value, or null for a kind that is not copied.
    public ViewModelInstanceValue copyValue() {
        return null;
    }

    /// Resolves what the copy refers to in `file`.
    public void complete(RiveFileContext file, int depth) {
    }

    protected final <T extends ViewModelInstanceValue> T copyBase(T c) {
        c.setViewModelPropertyId(getViewModelPropertyId());
        c.setViewModelProperty(property);
        return c;
    }
}
