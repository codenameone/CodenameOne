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

/// A path generated from a few parameters (size and origin) rather than
/// stored vertices. Subclasses fill [#vertices] in [#buildVertices()].
public abstract class ParametricPath extends ParametricPathBase {
    /// Regenerates [#vertices] from the parameters.
    protected abstract void buildVertices();

    protected final StraightVertex straight(int index) {
        while (vertices.size() <= index) {
            vertices.add(new StraightVertex());
        }
        PathVertex v = vertices.get(index);
        if (!(v instanceof StraightVertex)) {
            v = new StraightVertex();
            vertices.set(index, v);
        }
        return (StraightVertex) v;
    }

    protected final void truncate(int count) {
        while (vertices.size() > count) {
            vertices.remove(vertices.size() - 1);
        }
    }

    @Override
    public void update(int value) {
        if (hasDirt(value, DIRT_PATH)) {
            buildVertices();
        }
        super.update(value);
    }

    @Override
    protected void widthChanged() {
        markPathDirty();
    }

    @Override
    protected void heightChanged() {
        markPathDirty();
    }

    @Override
    protected void originXChanged() {
        markPathDirty();
    }

    @Override
    protected void originYChanged() {
        markPathDirty();
    }
}
