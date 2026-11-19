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

/// Something the artboard draws, in draw order, clipped by the clipping
/// shapes of its ancestors.
public abstract class Drawable extends DrawableBase {
    public static final int FLAG_HIDDEN = 1;

    protected final ArrayList<ClippingShape> clippingShapes = new ArrayList<ClippingShape>();

    public boolean isHidden() {
        return (mDrawableFlags & FLAG_HIDDEN) != 0 || isCollapsed();
    }

    /// Drawables not ported yet draw nothing.
    public void draw(RiveRenderer renderer) {
    }

    /// Applies the clips of this drawable; answers whether anything was pushed
    /// (the caller then restores).
    public boolean clip(RiveRenderer renderer) {
        if (clippingShapes.isEmpty()) {
            return false;
        }
        renderer.save();
        for (int i = 0; i < clippingShapes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ClippingShape c = clippingShapes.get(i);
            if (c.isVisible()) {
                renderer.clipPath(c.getClipPath());
            }
        }
        return true;
    }

    @Override
    public int onAddedClean(Artboard context) {
        int code = super.onAddedClean(context);
        for (ContainerComponent p = parent; p != null; p = p.getParent()) {
            ArrayList<Component> kids = p.getChildren();
            for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                if (kids.get(i) instanceof ClippingShape) {
                    clippingShapes.add((ClippingShape) kids.get(i));
                }
            }
        }
        return code;
    }

    @Override
    protected void drawableFlagsChanged() {
        if (artboard != null) {
            artboard.addDirt(DIRT_COMPONENTS);
        }
    }
}
