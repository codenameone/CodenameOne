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

/// Converts a bound value on its way to the target (and back for binds to
/// the source). Binds that target the converter itself (an operand, a
/// formula input) are refreshed from the same view model instance.
public class DataConverter extends DataConverterBase {
    private final ArrayList<DataBind> dataBinds = new ArrayList<DataBind>();
    private RiveFileContext file;

    public void setFile(RiveFileContext f) {
        file = f;
    }

    public RiveFileContext getFile() {
        return file;
    }

    public void addDataBind(DataBind b) {
        dataBinds.add(b);
    }

    public ArrayList<DataBind> getDataBinds() {
        return dataBinds;
    }

    /// Upstream's `DataConverter::copy`: each copy takes copies of the binds
    /// on its own properties, targeting the copy.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverter o = (DataConverter) source;
        file = o.file;
        for (int i = 0; i < o.dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            dataBinds.add(o.dataBinds.get(i).cloneWithTarget(this));
        }
    }

    /// Binds this converter's own binds in `root`.
    public void bind(ViewModelInstance root) {
        for (int i = 0; i < dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            dataBinds.get(i).bind(root);
        }
    }

    /// Applies this converter's own binds; answers whether any source had
    /// changed, which makes the bind using it dirty.
    public boolean refreshBinds() {
        boolean changed = false;
        for (int i = 0; i < dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            DataBind b = dataBinds.get(i);
            ViewModelInstanceValue s = b.getSource();
            int before = s == null ? 0 : s.getChangeCount();
            b.updateSourceToTarget();
            if (s != null && trackChange(b, before)) {
                changed = true;
            }
        }
        return changed;
    }

    private int[] seen = new int[0];

    private boolean trackChange(DataBind b, int count) {
        int i = dataBinds.indexOf(b);
        if (seen.length != dataBinds.size()) {
            seen = new int[dataBinds.size()];
            for (int k = 0; k < seen.length; k++) {
                seen[k] = -1;
            }
        }
        if (seen[i] != count) {
            seen[i] = count;
            return true;
        }
        return false;
    }

    /// Advances the converters of this converter's own binds (upstream
    /// `DataConverter::advance`); answers whether the bind using this
    /// converter must convert again.
    public boolean advance(float elapsedSeconds) {
        boolean dirty = false;
        for (int i = 0; i < dataBinds.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (dataBinds.get(i).advance(elapsedSeconds)) {
                dirty = true;
            }
        }
        return dirty;
    }

    /// The type this converter produces, or NONE when it keeps the input's.
    public int outputType() {
        return DataValue.NONE;
    }

    /// Converts `v` in place, source to target.
    public void convert(DataValue v, DataBind bind) {
    }

    /// Converts `v` in place, target to source.
    public void reverseConvert(DataValue v, DataBind bind) {
    }
}
