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

/// A view model enum value: an index into the property's enum.
public class ViewModelInstanceEnum extends ViewModelInstanceEnumBase {
    private DataEnum dataEnum;

    public DataEnum getDataEnum() {
        return dataEnum;
    }

    @Override
    protected void propertyValueChanged() {
        valueChanged();
    }

    @Override
    public void read(DataValue out) {
        out.setEnum(dataEnum, getPropertyValue());
    }

    @Override
    public void write(DataValue in) {
        if (in.type == DataValue.STRING) {
            setValueByName(in.string);
        } else {
            setPropertyValue((int) in.asNumber());
        }
    }

    /// Selects the enum value whose key or value is `name`; false if none.
    public boolean setValueByName(String name) {
        int i = dataEnum == null ? -1 : dataEnum.indexOf(name);
        if (i < 0) {
            return false;
        }
        setPropertyValue(i);
        return true;
    }

    @Override
    public ViewModelInstanceValue copyValue() {
        ViewModelInstanceEnum c = copyBase(new ViewModelInstanceEnum());
        c.setPropertyValue(getPropertyValue());
        c.dataEnum = dataEnum;
        return c;
    }

    @Override
    public void complete(RiveFileContext file, int depth) {
        ViewModelProperty p = getViewModelProperty();
        if (dataEnum == null && p instanceof ViewModelPropertyEnumCustom) {
            dataEnum = file.getEnum(((ViewModelPropertyEnumCustom) p).getEnumId());
        }
    }
}
