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

/// Port of `NestedArtboardLayout`: its nested artboard joins the parent
/// layout as a child node (sized by the instance width/height and scale
/// types), and the resolved position moves the instance.
public class NestedArtboardLayout extends NestedArtboardLayoutBase {
    private Artboard hosted;

    void setHostedLayout(Artboard a) {
        hosted = a;
    }

    private void layoutStyleChanged() {
        if (hosted != null) {
            markWorldTransformDirty();
        }
    }

    @Override
    protected void instanceWidthChanged() {
        layoutStyleChanged();
    }

    @Override
    protected void instanceHeightChanged() {
        layoutStyleChanged();
    }

    /// Port of `NestedArtboardLayout::applyLayoutPlacement`.
    @Override
    public void updateWorldTransform() {
        super.updateWorldTransform();
        Artboard a = hosted;
        if (a == null) {
            return;
        }
        float bx = a.getLayoutX() - a.getLayoutWidth() * a.getOriginX();
        float by = a.getLayoutY() - a.getLayoutHeight() * a.getOriginY();
        if (parent instanceof Artboard) {
            Artboard pa = (Artboard) parent;
            bx += pa.getLayoutWidth() * pa.getOriginX();
            by += pa.getLayoutHeight() * pa.getOriginY();
        }
        float[] w = worldTransform.v;
        if (parentTransform != null) {
            float[] p = parentTransform.getWorldTransform().v;
            w[4] += p[0] * bx + p[2] * by;
            w[5] += p[1] * bx + p[3] * by;
        } else {
            w[4] += bx;
            w[5] += by;
        }
    }
}
