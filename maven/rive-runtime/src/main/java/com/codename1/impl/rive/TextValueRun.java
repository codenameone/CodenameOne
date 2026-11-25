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

/// Rive `TextValueRun`: a string drawn with one `TextStyle`.
public class TextValueRun extends TextValueRunBase {
    private TextStyle style;
    private Text text;

    public TextStyle getStyle() {
        return style;
    }

    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        if (code != STATUS_OK) {
            return code;
        }
        Core s = context.resolve(mStyleId);
        style = s instanceof TextStyle ? (TextStyle) s : null;
        if (parent instanceof Text) {
            text = (Text) parent;
        }
        return code;
    }

    @Override
    protected void textChanged() {
        if (text != null) {
            text.addDirt(DIRT_TEXT_SHAPE);
        }
    }

    @Override
    protected void styleIdChanged() {
        if (artboard != null) {
            Core s = artboard.resolve(mStyleId);
            style = s instanceof TextStyle ? (TextStyle) s : null;
        }
        textChanged();
    }
}
