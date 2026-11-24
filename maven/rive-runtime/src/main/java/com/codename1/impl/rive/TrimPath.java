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

/// Port of `TrimPath`: draws only the `[start, end]` span (shifted by
/// `offset`) of the paint's path, across all contours (sequential) or of
/// each contour (synchronized).
public class TrimPath extends TrimPathBase implements StrokeEffect {
    public static final int MODE_SEQUENTIAL = 1;
    public static final int MODE_SYNCHRONIZED = 2;

    private final RivePath out = new RivePath();
    private final RivePathMeasure measure = new RivePathMeasure();
    private RivePath lastSource;
    private int lastVersion = -1;
    private boolean valid;
    private int[] indices = new int[8];
    private float[] lengths = new float[16];

    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        if (code != STATUS_OK) {
            return code;
        }
        return mModeValue == MODE_SEQUENTIAL || mModeValue == MODE_SYNCHRONIZED ? STATUS_OK : STATUS_INVALID;
    }

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
    protected void startChanged() {
        invalidateEffect();
    }

    @Override
    protected void endChanged() {
        invalidateEffect();
    }

    @Override
    protected void offsetChanged() {
        invalidateEffect();
    }

    @Override
    protected void modeValueChanged() {
        invalidateEffect();
    }

    @Override
    public RivePath effectPath(RivePath source, boolean fill) {
        if (valid && source == lastSource && source.version == lastVersion) { // NOPMD CompareObjectsWithEquals - identity is the intent
            return out;
        }
        lastSource = source;
        lastVersion = source.version;
        valid = true;
        out.rewind();
        out.fillRule = source.fillRule;
        measure.build(source);
        trim(fill);
        out.version++;
        return out;
    }

    private static float fmod(float a, float b) {
        return a - b * (float) (int) (a / b);
    }

    private void trim(boolean closeShape) {
        float renderOffset = fmod(fmod(mOffset, 1f) + 1f, 1f);
        int count = measure.getContourCount();
        if (count == 0) {
            return;
        }
        if (mModeValue == MODE_SEQUENTIAL) {
            float total = measure.getLength();
            float startLength = total * (mStart + renderOffset);
            float endLength = total * (mEnd + renderOffset);
            if (endLength < startLength) {
                float swap = startLength;
                startLength = endLength;
                endLength = swap;
            }
            if (total > 0f && startLength > total) {
                float wrapped = fmod(startLength, total);
                endLength -= startLength - wrapped;
                startLength = wrapped;
            }
            endLength = Math.min(endLength, Math.max(startLength, 0f) + total);
            int n = 0;
            int i = 0;
            while (endLength > 0) {
                int ci = i % count;
                float cl = measure.getContourLength(ci);
                if (startLength < cl) {
                    if (n == indices.length) {
                        int[] ni = new int[n * 2];
                        float[] nl = new float[n * 4];
                        System.arraycopy(indices, 0, ni, 0, n);
                        System.arraycopy(lengths, 0, nl, 0, n * 2);
                        indices = ni;
                        lengths = nl;
                    }
                    indices[n] = ci;
                    lengths[n * 2] = startLength;
                    lengths[n * 2 + 1] = endLength;
                    n++;
                    endLength -= cl;
                    startLength = 0;
                } else {
                    startLength -= cl;
                    endLength -= cl;
                }
                i++;
            }
            // Upstream order: first segment, then backwards from the last, so
            // a closed contour's tail joins its head without a gap.
            int startingIndex = 0;
            int prev = -1;
            for (int k = 0; k < n; k++) {
                int index = (startingIndex < 0 ? startingIndex + n : startingIndex) % n;
                int ci = indices[index];
                float cl = measure.getContourLength(ci);
                float s = lengths[index * 2];
                float e = lengths[index * 2 + 1];
                boolean closed = measure.isContourClosed(ci);
                measure.getContourSegment(ci, s, e, out, prev != ci || !closed);
                if ((s == 0f && e - s >= cl && closed) || closeShape) {
                    out.close();
                }
                prev = ci;
                startingIndex--;
            }
        } else {
            for (int ci = 0; ci < count; ci++) {
                float cl = measure.getContourLength(ci);
                boolean closed = measure.isContourClosed(ci);
                float s = cl * (mStart + renderOffset);
                float e = cl * (mEnd + renderOffset);
                if (e < s) {
                    float swap = s;
                    s = e;
                    e = swap;
                }
                if (s >= cl) {
                    s -= cl;
                    e -= cl;
                }
                measure.getContourSegment(ci, s, e, out, true);
                int guard = 0;
                while (e > cl && guard++ < 4) {
                    s = 0;
                    e -= cl;
                    measure.getContourSegment(ci, s, e, out, !closed);
                }
                if ((mStart == 0f && mEnd == 1f && closed) || closeShape) {
                    out.close();
                }
            }
        }
    }
}
