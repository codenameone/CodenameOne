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

/// A linear gradient in the shape's local space.
public class LinearGradient extends LinearGradientBase implements ShapePaintMutator {
    protected final ArrayList<GradientStop> stops = new ArrayList<GradientStop>();

    public void addStop(GradientStop s) {
        stops.add(s);
    }

    @Override
    public int onAddedClean(Artboard context) {
        if (parent instanceof ShapePaint) {
            ((ShapePaint) parent).setMutator(this);
        }
        return STATUS_OK;
    }

    @Override
    public void buildDependencies() {
        for (int i = 0; i < stops.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            stops.get(i).addDependent(this);
        }
    }

    static int withOpacity(int argb, float opacity) {
        int a = (int) (((argb >>> 24) & 0xff) * opacity + 0.5f);
        if (a > 255) {
            a = 255;
        } else if (a < 0) {
            a = 0;
        }
        return (a << 24) | (argb & 0xffffff);
    }

    protected int gradientKind() {
        return RivePaint.GRADIENT_LINEAR;
    }

    @Override
    public void applyTo(RivePaint paint, float opacity) {
        int n = stops.size();
        // stops sorted by position (insertion sort; a handful at most, no allocation)
        for (int i = 1; i < n; i++) {
            GradientStop s = stops.get(i);
            int j = i - 1;
            while (j >= 0 && stops.get(j).getPosition() > s.getPosition()) {
                stops.set(j + 1, stops.get(j));
                j--;
            }
            stops.set(j + 1, s);
        }
        if (paint.stopColors.length < n) {
            paint.stopColors = new int[n];
            paint.stopPositions = new float[n];
        }
        float o = opacity * mOpacity;
        for (int i = 0; i < n; i++) {
            GradientStop s = stops.get(i);
            paint.stopColors[i] = withOpacity(s.getColorValue(), o);
            paint.stopPositions[i] = s.getPosition();
        }
        paint.stopCount = n;
        paint.gradient = gradientKind();
        paint.startX = mStartX;
        paint.startY = mStartY;
        paint.endX = mEndX;
        paint.endY = mEndY;
    }

    private void dirtyPaint() {
        if (parent != null) {
            parent.addDirt(DIRT_PAINT);
        }
    }

    @Override
    protected void startXChanged() {
        dirtyPaint();
    }

    @Override
    protected void startYChanged() {
        dirtyPaint();
    }

    @Override
    protected void endXChanged() {
        dirtyPaint();
    }

    @Override
    protected void endYChanged() {
        dirtyPaint();
    }

    @Override
    protected void opacityChanged() {
        dirtyPaint();
    }
}
