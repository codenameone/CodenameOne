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

/// Rive `FontAsset`: an embedded font, decoded on first use. A referenced
/// (hosted) asset has no bytes and therefore no font; its text draws nothing.
public class FontAsset extends FontAssetBase {
    private RiveFont font;
    private boolean decoded;

    @Override
    public void decode(byte[] bytes) {
        super.decode(bytes);
        font = null;
        decoded = false;
    }

    /// The decoded font, or null when there are no bytes or they are not a
    /// TrueType font.
    public RiveFont getFont() {
        if (!decoded) {
            decoded = true;
            font = RiveFont.decode(contents);
        }
        return font;
    }
}
