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

/// Rive `DataConverterNumberToList`: turns a number into a list of that many
/// instances of its view model. Items are kept between conversions, so a
/// count that grows appends fresh default instances and one that shrinks
/// drops the last ones; the rest keep their state and their rows.
public class DataConverterNumberToList extends DataConverterNumberToListBase {
    private final ArrayList<ViewModelInstance> listItems = new ArrayList<ViewModelInstance>();
    private final ViewModelInstanceList output = new ViewModelInstanceList();

    @Override
    public int outputType() {
        return DataValue.LIST;
    }

    @Override
    public void convert(DataValue v, DataBind bind) {
        if (v.type == DataValue.LIST) {
            return;
        }
        if (v.type != DataValue.NUMBER) {
            v.type = DataValue.NONE;
            return;
        }
        int count = Math.max(0, (int) Math.floor(v.number));
        RiveFileContext file = getFile();
        ViewModel vm = file == null ? null : file.getViewModel(getViewModelId());
        if (vm != null) {
            while (listItems.size() < count) {
                ViewModelInstance template = vm.defaultInstance();
                ViewModelInstance copy = template == null ? null : template.copyInstance(file, 0);
                if (copy == null) {
                    break;
                }
                listItems.add(copy);
            }
            while (listItems.size() > count) {
                listItems.remove(listItems.size() - 1);
            }
        } else {
            listItems.clear();
        }
        output.replaceInstances(listItems);
        v.setList(output);
    }

    @Override
    protected void viewModelIdChanged() {
        // Clear the cached items if the view model changes.
        listItems.clear();
    }
}
