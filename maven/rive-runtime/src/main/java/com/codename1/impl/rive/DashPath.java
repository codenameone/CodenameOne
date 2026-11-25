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

/// Port of `DashPath`: cuts a stroke's path into alternating dashes and
/// gaps from its [Dash] children, starting `offset` along the path. Fills
/// are left alone, as upstream.
public class DashPath extends DashPathBase implements StrokeEffect {
    private final ArrayList<Dash> dashes = new ArrayList<Dash>();
    private final Dash offsetDash = new Dash();
    private final RivePath out = new RivePath();
    private final RivePathMeasure measure = new RivePathMeasure();
    private RivePath lastSource;
    private int lastVersion = -1;
    private boolean valid;

    @Override
    public int onAddedClean(Artboard context) {
        if (parent instanceof GroupEffect) {
            // An effect of a group is shared through a TargetEffect; groups
            // are not ported, so it stays valid and inert.
            return STATUS_OK;
        }
        if (!(parent instanceof ShapePaint)) {
            return STATUS_INVALID;
        }
        ((ShapePaint) parent).addStrokeEffect(this);
        dashes.clear();
        ArrayList<Component> kids = getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (kids.get(i) instanceof Dash) {
                dashes.add((Dash) kids.get(i));
            }
        }
        return STATUS_OK;
    }

    @Override
    public void invalidateEffect() {
        valid = false;
        if (parent instanceof ShapePaint) {
            ShapePaint p = (ShapePaint) parent;
            if (p.getShape() != null) {
                p.getShape().addDirt(DIRT_PAINT);
            }
        }
    }

    @Override
    protected void offsetChanged() {
        invalidateEffect();
    }

    @Override
    protected void offsetIsPercentageChanged() {
        invalidateEffect();
    }

    @Override
    public RivePath effectPath(RivePath source, boolean fill) {
        if (fill) {
            return source;
        }
        if (valid && source == lastSource && source.version == lastVersion) { // NOPMD CompareObjectsWithEquals - identity is the intent
            return out;
        }
        lastSource = source;
        lastVersion = source.version;
        valid = true;
        out.rewind();
        out.fillRule = source.fillRule;
        measure.build(source);
        offsetDash.setLength(mOffset);
        offsetDash.setLengthIsPercentage(mOffsetIsPercentage);
        applyDash();
        out.version++;
        return out;
    }

    /// Port of `PathDasher::applyDash`.
    private void applyDash() {
        float length = measure.getLength();
        boolean hasValidDash = false;
        for (int i = 0; i < dashes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (dashes.get(i).normalizedLength(length, false) > 0f) {
                hasValidDash = true;
                break;
            }
        }
        if (!hasValidDash) {
            return;
        }
        int dashIndex = 0;
        float dashed = 0f;
        float distance = offsetDash.normalizedLength(length, true);
        boolean draw = true;
        while (dashed < length) {
            Dash dash = dashes.get(dashIndex++ % dashes.size());
            float dashLength = dash.normalizedLength(length, false);
            if (dashLength > length) {
                dashLength = length;
            }
            float endLength = distance + dashLength;
            if (endLength > length) {
                endLength -= length;
                if (draw) {
                    if (distance < length) {
                        measure.getSegment(distance, length, out, true);
                        measure.getSegment(0f, endLength, out, !measure.isClosed());
                    } else {
                        measure.getSegment(0f, endLength, out, true);
                    }
                }
                distance = endLength - dashLength;
            } else if (draw) {
                measure.getSegment(distance, endLength, out, true);
            }
            distance += dashLength;
            dashed += dashLength;
            draw = !draw;
        }
    }
}
