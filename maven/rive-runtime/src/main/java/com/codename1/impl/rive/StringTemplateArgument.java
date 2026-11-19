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

/// A named argument of a string template, whose value comes from its bind.
public class StringTemplateArgument extends StringTemplateArgumentBase {
    private DataBind dataBind;
    private final DataValue scratch = new DataValue();

    /// Upstream's clone: the copy takes a copy of the bind on it.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataBind b = ((StringTemplateArgument) source).dataBind;
        dataBind = b == null ? null : b.cloneWithTarget(this);
    }

    public void addDataBind(DataBind b) {
        dataBind = b;
    }

    public DataBind getDataBind() {
        return dataBind;
    }

    /// The bound value as text.
    /// Appends this argument's value for the placeholder `spec`
    /// (upstream `appendValue`): a number goes through the printf-style
    /// formatter, anything else is appended as its string.
    public void appendValue(StringBuilder out, String spec) {
        if (dataBind == null || dataBind.getSource() == null) {
            return;
        }
        dataBind.getSource().read(scratch);
        if (scratch.type == DataValue.NUMBER || this instanceof StringTemplateArgumentNumber) {
            out.append(StringTemplateFormat.formatNumber(scratch.asNumber(), spec));
        } else {
            out.append(scratch.asString());
        }
    }

    public String text() {
        if (dataBind == null || dataBind.getSource() == null) {
            return "";
        }
        dataBind.getSource().read(scratch);
        return scratch.asString();
    }
}
