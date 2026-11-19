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

/// Treats the input as the template and replaces each `{name}` (with the
/// configured delimiters) by the named argument's bound value.
public class DataConverterStringTemplate extends DataConverterStringTemplateBase {
    private final ArrayList<StringTemplateArgument> arguments = new ArrayList<StringTemplateArgument>();

    /// Upstream's `DataConverterStringTemplate::clone`: the arguments are copied.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        ArrayList<StringTemplateArgument> from = ((DataConverterStringTemplate) source).arguments;
        for (int i = 0; i < from.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            Core copy = from.get(i).makeCopy();
            arguments.add(copy instanceof StringTemplateArgument ? (StringTemplateArgument) copy : from.get(i));
        }
    }

    public void addArgument(StringTemplateArgument a) {
        arguments.add(a);
    }

    @Override
    public int outputType() {
        return DataValue.STRING;
    }

    @Override
    public void bind(ViewModelInstance root) {
        super.bind(root);
        for (int i = 0; i < arguments.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            DataBind b = arguments.get(i).getDataBind();
            if (b != null) {
                b.bind(root);
            }
        }
    }

    @Override
    public boolean refreshBinds() {
        super.refreshBinds();
        return !arguments.isEmpty();
    }

    private final ArrayList<StringTemplateFormat.Segment> segments = new ArrayList<StringTemplateFormat.Segment>();
    private String parsedText;
    private String parsedOpen;
    private String parsedClose;
    private final StringBuilder output = new StringBuilder(); // NOPMD AvoidStringBufferField - reused every frame, so converting allocates no builder

    /// Upstream `DataConverterStringTemplate::convert`: the template is parsed
    /// once per text into literals and placeholders; a named placeholder takes
    /// the argument of that name, a positional or bare one the argument at
    /// its index, and one without an argument writes nothing.
    @Override
    public void convert(DataValue v, DataBind bind) {
        if (v.type != DataValue.STRING) {
            v.setString("");
            return;
        }
        String text = v.string;
        String open = getOpenDelimiter() == null ? "{" : getOpenDelimiter();
        String close = getCloseDelimiter() == null ? "}" : getCloseDelimiter();
        if (!text.equals(parsedText) || !open.equals(parsedOpen) || !close.equals(parsedClose)) {
            StringTemplateFormat.parse(text, open, close, segments);
            parsedText = text;
            parsedOpen = open;
            parsedClose = close;
        }
        output.setLength(0);
        for (int i = 0; i < segments.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            StringTemplateFormat.Segment seg = segments.get(i);
            StringTemplateArgument arg = null;
            if (seg.kind == StringTemplateFormat.LITERAL) {
                output.append(seg.text);
                continue;
            } else if (seg.kind == StringTemplateFormat.NAME) {
                arg = argument(seg.name);
            } else if (seg.index < arguments.size()) {
                arg = arguments.get(seg.index);
            }
            if (arg != null) {
                arg.appendValue(output, seg.spec);
            }
        }
        v.setString(output.toString());
    }

    private StringTemplateArgument argument(String name) {
        for (int i = 0; i < arguments.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (name.equals(arguments.get(i).getName())) {
                return arguments.get(i);
            }
        }
        return null;
    }
}
