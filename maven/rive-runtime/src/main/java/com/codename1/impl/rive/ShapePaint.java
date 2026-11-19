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

/// A fill or stroke of a shape. Its single child (solid color or gradient)
/// writes into the [RivePaint] the renderer receives.
public abstract class ShapePaint extends ShapePaintBase {
    protected final RivePaint renderPaint = new RivePaint();
    protected ShapePaintMutator mutator;
    protected Shape shape;
    protected TextStylePaint textStyle;
    private StrokeEffect[] effects;

    /// Registers a trim or dash child; effects apply in the order added.
    public void addStrokeEffect(StrokeEffect e) {
        int n = effects == null ? 0 : effects.length;
        StrokeEffect[] a = new StrokeEffect[n + 1];
        if (n > 0) {
            System.arraycopy(effects, 0, a, 0, n);
        }
        a[n] = e;
        effects = a;
    }

    /// The path this paint really draws for `source`: `source` itself, or
    /// what its stroke effects make of it.
    public RivePath effectPath(RivePath source) {
        if (effects == null) {
            return source;
        }
        RivePath p = source;
        boolean fill = this instanceof Fill;
        for (StrokeEffect e : effects) {
            p = e.effectPath(p, fill);
        }
        return p;
    }

    public RivePaint getRenderPaint() {
        return renderPaint;
    }

    public Shape getShape() {
        return shape;
    }

    void setMutator(ShapePaintMutator m) {
        mutator = m;
    }

    @Override
    public int onAddedClean(Artboard context) {
        if (parent instanceof Shape) {
            shape = (Shape) parent;
            shape.addPaint(this);
        } else if (parent instanceof Artboard) {
            ((Artboard) parent).addBackgroundPaint(this);
        } else if (parent instanceof LayoutComponent) {
            ((LayoutComponent) parent).addLayoutPaint(this);
        } else if (parent instanceof TextStylePaint) {
            textStyle = (TextStylePaint) parent;
            textStyle.addPaint(this);
        }
        return STATUS_OK;
    }

    /// Refreshes the render paint from the mutator at the shape's opacity.
    public void updatePaint(float opacity) {
        if (mBlendModeValue != 127) {
            renderPaint.blendMode = mBlendModeValue;
        } else if (shape != null) {
            renderPaint.blendMode = shape.getBlendModeValue();
        } else if (textStyle != null) {
            renderPaint.blendMode = textStyle.getTextBlendMode();
        } else {
            renderPaint.blendMode = mBlendModeValue;
        }
        if (mutator != null) {
            mutator.applyTo(renderPaint, opacity);
        }
        renderPaint.version++;
    }

    @Override
    public void update(int value) {
        if (hasDirt(value, DIRT_PAINT)) {
            if (shape != null) {
                updatePaint(shape.getRenderOpacity());
            } else if (textStyle != null) {
                updatePaint(textStyle.getRenderOpacity());
            }
        }
    }

    @Override
    protected void isVisibleChanged() {
        if (shape != null) {
            shape.addDirt(DIRT_PAINT);
        }
    }

    @Override
    protected void blendModeValueChanged() {
        addDirt(DIRT_PAINT);
    }
}
