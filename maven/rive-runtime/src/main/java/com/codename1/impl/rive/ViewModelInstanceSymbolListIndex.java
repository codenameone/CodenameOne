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

/// The index of a view model instance in the list that holds it: the value
/// a list row's artboard binds to show its own position. The list writes it
/// (`ViewModelInstanceList`) whenever items are added, removed or resolved.
/// Upstream reads it as an integer data value; Codename One's data values
/// carry numbers, so it reads as a whole number.
public class ViewModelInstanceSymbolListIndex extends ViewModelInstanceSymbolListIndexBase {
    @Override
    protected void propertyValueChanged() {
        valueChanged();
    }

    @Override
    public void read(DataValue out) {
        out.setNumber(getPropertyValue());
    }

    /// Upstream's `applyValue`: a bind writing into the symbol sets it.
    @Override
    public void write(DataValue in) {
        setPropertyValue((int) in.asNumber());
    }

    @Override
    public ViewModelInstanceValue copyValue() {
        ViewModelInstanceSymbolListIndex c = copyBase(new ViewModelInstanceSymbolListIndex());
        c.setPropertyValue(getPropertyValue());
        return c;
    }
}
