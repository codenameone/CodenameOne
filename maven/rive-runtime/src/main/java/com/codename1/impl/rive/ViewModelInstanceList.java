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

/// A view model list: its items are instances of view models.
public class ViewModelInstanceList extends ViewModelInstanceListBase {
    private final ArrayList<ViewModelInstanceListItem> items = new ArrayList<ViewModelInstanceListItem>();
    private final ArrayList<ViewModelInstance> instances = new ArrayList<ViewModelInstance>();

    public void addItem(ViewModelInstanceListItem item) {
        items.add(item);
    }

    /// The resolved item instances of a bound copy.
    public ArrayList<ViewModelInstance> getInstances() {
        return instances;
    }

    @Override
    public void read(DataValue out) {
        out.setList(instances.isEmpty() ? items.size() : instances.size());
    }

    /// Appends `instance` to a bound list, as upstream's `addItem` does at run
    /// time: it gets its index, and rows bound to the list rebuild.
    public void addInstance(ViewModelInstance instance) {
        instances.add(instance);
        assignListIndices(instances.size() - 1);
        valueChanged();
    }

    /// Inserts `instance` at `index` of a bound list; the items after it move
    /// down one and are told their new indices.
    public void addInstance(int index, ViewModelInstance instance) {
        instances.add(index, instance);
        assignListIndices(index);
        valueChanged();
    }

    /// Makes `items` the list's instances, each told its index, and notifies
    /// the list's readers: a converter's output list (upstream `DataValueList`).
    void replaceInstances(ArrayList<ViewModelInstance> items) {
        instances.clear();
        instances.addAll(items);
        assignListIndices(0);
        valueChanged();
    }

    /// Removes the item at `index` of a bound list; the items after it move up.
    public void removeInstance(int index) {
        instances.remove(index);
        assignListIndices(index);
        valueChanged();
    }

    /// Upstream's `assignListIndices`: writes each item's position, from
    /// `start` on, into its index symbol when its view model declares one.
    private void assignListIndices(int start) {
        for (int i = start; i < instances.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ViewModelInstanceSymbolListIndex symbol = indexSymbol(instances.get(i));
            if (symbol != null) {
                symbol.setPropertyValue(i);
            }
        }
    }

    /// The item's index symbol (upstream's `SymbolType::itemIndex`), or null.
    static ViewModelInstanceSymbolListIndex indexSymbol(ViewModelInstance instance) {
        if (instance == null) {
            return null;
        }
        ArrayList<ViewModelInstanceValue> values = instance.getValues();
        for (int i = 0; i < values.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ViewModelInstanceValue v = values.get(i);
            if (v instanceof ViewModelInstanceSymbolListIndex) {
                return (ViewModelInstanceSymbolListIndex) v;
            }
        }
        return null;
    }

    @Override
    public ViewModelInstanceValue copyValue() {
        ViewModelInstanceList c = copyBase(new ViewModelInstanceList());
        c.items.addAll(items);
        return c;
    }

    @Override
    public void complete(RiveFileContext file, int depth) {
        instances.clear();
        for (int i = 0; i < items.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            ViewModelInstanceListItem it = items.get(i);
            ViewModel vm = file.getViewModel(it.getViewModelId());
            ViewModelInstance src = vm == null ? null : vm.getInstance(it.getViewModelInstanceId());
            if (src != null) {
                instances.add(src.copyInstance(file, depth));
            }
        }
        assignListIndices(0);
    }
}
