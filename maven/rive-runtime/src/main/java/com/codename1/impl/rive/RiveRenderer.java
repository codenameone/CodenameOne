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

/// The drawing surface an artboard draws onto. Implemented once over
/// `com.codename1.ui.Graphics` and once over the GPU API, which is the one
/// that can also draw meshes.
public interface RiveRenderer {
    void save();

    void restore();

    /// Concatenates `m` onto the current transform.
    void transform(Mat2D m);

    /// Intersects the clip with `path` under the current transform.
    void clipPath(RivePath path);

    void drawPath(RivePath path, RivePaint paint);

    /// Draws `image` (the decoded image of an `ImageAsset`) with its top-left
    /// at the origin, at `opacity`.
    void drawImage(Object image, int blendMode, float opacity);

    /// Draws `image` deformed over a triangle mesh: `vertices` and `uvs` are
    /// interleaved x,y pairs, `indices` three per triangle.
    void drawImageMesh(Object image, float[] vertices, float[] uvs, int vertexCount,
            int[] indices, int indexCount, int blendMode, float opacity);

    /// Whether [#drawImageMesh] really deforms the image.
    boolean supportsMeshes();
}
