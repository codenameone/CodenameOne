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

/// A view model property holding another view model instance: the value is
/// the index of the instance in the referenced view model.
public class ViewModelInstanceViewModel extends ViewModelInstanceViewModelBase {
    private ViewModelInstance reference;

    /// The referenced instance, or null.
    public ViewModelInstance getReference() {
        return reference;
    }

    public void setReference(ViewModelInstance i) {
        reference = i;
        valueChanged();
    }

    @Override
    public void read(DataValue out) {
        out.setViewModel(reference);
    }

    @Override
    public void write(DataValue in) {
        if (in.type == DataValue.VIEW_MODEL && in.viewModel != reference) { // NOPMD CompareObjectsWithEquals - identity is the intent
            setReference(in.viewModel);
        }
    }

    @Override
    public ViewModelInstanceValue copyValue() {
        ViewModelInstanceViewModel c = copyBase(new ViewModelInstanceViewModel());
        c.setPropertyValue(getPropertyValue());
        return c;
    }

    @Override
    public void complete(RiveFileContext file, int depth) {
        ViewModelProperty p = getViewModelProperty();
        if (!(p instanceof ViewModelPropertyViewModel)) {
            return;
        }
        ViewModel vm = file.getViewModel(((ViewModelPropertyViewModel) p).getViewModelReferenceId());
        ViewModelInstance src = vm == null ? null : vm.getInstance(getPropertyValue());
        if (src != null) {
            reference = src.copyInstance(file, depth);
        }
    }
}
