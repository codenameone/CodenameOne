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

/// Rive `TextStyle`: size, line height, letter spacing and the font asset a
/// run of text is shaped with.
public class TextStyle extends TextStyleBase {
    private FontAsset fontAsset;
    private final java.util.ArrayList<Text> users = new java.util.ArrayList<Text>();

    /// The style's font, or null when it has none this runtime can draw.
    public RiveFont getFont() {
        return fontAsset == null ? null : fontAsset.getFont();
    }

    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        RiveFileContext file = context.getFile();
        FileAsset a = file == null ? null : file.getAsset(mFontAssetId);
        fontAsset = a instanceof FontAsset ? (FontAsset) a : null;
        return code;
    }

    /// Registers a text shaped with this style, so a change reshapes it.
    void addUser(Text t) {
        if (!users.contains(t)) {
            users.add(t);
        }
    }

    protected void shapeChanged() {
        for (int i = 0; i < users.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            users.get(i).addDirt(DIRT_TEXT_SHAPE);
        }
    }

    @Override
    protected void fontSizeChanged() {
        shapeChanged();
    }

    @Override
    protected void lineHeightChanged() {
        shapeChanged();
    }

    @Override
    protected void letterSpacingChanged() {
        shapeChanged();
    }
}
