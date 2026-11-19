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

/// Port of `Weight`: up to four bone influences for a vertex, packed one byte
/// per influence into `values` and `indices`.
public class Weight extends WeightBase {
    protected boolean fitsVertex(Vertex vertex) {
        return !(vertex instanceof CubicVertex);
    }

    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        if (code != STATUS_OK) {
            return code;
        }
        if (!(parent instanceof Vertex) || !fitsVertex((Vertex) parent)) {
            // Upstream's MissingObject keeps the object, unattached.
            return STATUS_OK;
        }
        ((Vertex) parent).setWeight(this);
        return STATUS_OK;
    }

    private static int encoded(int index, int data) {
        return (data >>> (index * 8)) & 0xFF;
    }

    /// Port of `Weight::deform`: blends the bone transforms selected by the
    /// packed `indices`/`weights` and applies them to `world * (x, y)`,
    /// writing the result into `out[0..1]`.
    public static void deform(float x, float y, int indices, int weights, Mat2D world,
            float[] boneTransforms, float[] out) {
        float xx = 0;
        float xy = 0;
        float yx = 0;
        float yy = 0;
        float tx = 0;
        float ty = 0;
        for (int i = 0; i < 4; i++) {
            int weight = encoded(i, weights);
            if (weight == 0) {
                continue;
            }
            float w = weight / 255.0f;
            int b = encoded(i, indices) * 6;
            if (b + 5 >= boneTransforms.length) {
                continue;
            }
            xx += boneTransforms[b] * w;
            xy += boneTransforms[b + 1] * w;
            yx += boneTransforms[b + 2] * w;
            yy += boneTransforms[b + 3] * w;
            tx += boneTransforms[b + 4] * w;
            ty += boneTransforms[b + 5] * w;
        }
        float wx = world.mapX(x, y);
        float wy = world.mapY(x, y);
        out[0] = xx * wx + yx * wy + tx;
        out[1] = xy * wx + yy * wy + ty;
    }
}
