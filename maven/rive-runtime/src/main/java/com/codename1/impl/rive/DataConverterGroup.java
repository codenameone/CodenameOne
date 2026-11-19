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

/// A chain of converters, applied in order (and in reverse order back).
public class DataConverterGroup extends DataConverterGroupBase {
    private final ArrayList<DataConverterGroupItem> items = new ArrayList<DataConverterGroupItem>();

    public void addItem(DataConverterGroupItem item) {
        items.add(item);
    }

    /// Upstream's `DataConverterGroup::clone`: every item is copied with a
    /// copy of its converter.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterGroup o = (DataConverterGroup) source;
        for (int i = 0; i < o.items.size(); i++) {
            DataConverterGroupItem item = o.items.get(i);
            Core copy = item.makeCopy();
            DataConverterGroupItem ci = copy instanceof DataConverterGroupItem ? (DataConverterGroupItem) copy : item;
            if (ci != item) { // NOPMD CompareObjectsWithEquals - identity is the intent
                ci.setConverter(DataBind.copyOf(o.at(i)));
            }
            items.add(ci);
        }
    }

    private DataConverter at(int i) {
        RiveFileContext f = getFile();
        DataConverterGroupItem item = items.get(i);
        DataConverter c = item.getConverter() != null ? item.getConverter()
                : f == null ? null : f.getConverter(item.getConverterId());
        return c == this ? null : c; // NOPMD CompareObjectsWithEquals - identity is the intent
    }

    @Override
    public int outputType() {
        for (int i = items.size() - 1; i >= 0; i--) {
            DataConverter c = at(i);
            if (c != null && c.outputType() != DataValue.NONE) {
                return c.outputType();
            }
        }
        return DataValue.NONE;
    }

    @Override
    public void bind(ViewModelInstance root) {
        super.bind(root);
        for (int i = 0; i < items.size(); i++) {
            DataConverter c = at(i);
            if (c != null) {
                c.bind(root);
            }
        }
    }

    @Override
    public boolean advance(float elapsedSeconds) {
        boolean dirty = super.advance(elapsedSeconds);
        for (int i = 0; i < items.size(); i++) {
            DataConverter c = at(i);
            if (c != null && c.advance(elapsedSeconds)) {
                dirty = true;
            }
        }
        return dirty;
    }

    @Override
    public boolean refreshBinds() {
        boolean changed = super.refreshBinds();
        for (int i = 0; i < items.size(); i++) {
            DataConverter c = at(i);
            if (c != null && c.refreshBinds()) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void convert(DataValue v, DataBind bind) {
        for (int i = 0; i < items.size(); i++) {
            DataConverter c = at(i);
            if (c != null) {
                c.convert(v, bind);
            }
        }
    }

    @Override
    public void reverseConvert(DataValue v, DataBind bind) {
        for (int i = items.size() - 1; i >= 0; i--) {
            DataConverter c = at(i);
            if (c != null) {
                c.reverseConvert(v, bind);
            }
        }
    }
}
