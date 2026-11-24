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

/// Composes a shape's paths into one path in the shape's local space. It is a
/// component of its own, as in rive-runtime, because the paths are children
/// of the shape: the shape cannot also depend on them without a cycle, so the
/// composer depends on both and runs after them.
public final class PathComposer extends Component {
    private final Shape shape;
    private final Mat2D inverseWorld = new Mat2D();
    private final Mat2D toLocal = new Mat2D();

    PathComposer(Shape shape) {
        this.shape = shape;
    }

    @Override
    public int coreType() {
        return -1;
    }

    @Override
    public void buildDependencies() {
        shape.addDependent(this);
        for (int i = 0; i < shape.getPaths().size(); i++) {
            shape.getPaths().get(i).addDependent(this);
        }
    }

    @Override
    public void update(int value) {
        if (hasDirt(value, DIRT_PATH)) {
            RivePath composed = shape.getComposedPath();
            inverseWorld.invert(shape.getWorldTransform());
            composed.rewind();
            for (int i = 0; i < shape.getPaths().size(); i++) {
                Path p = shape.getPaths().get(i);
                if (p.isCollapsed()) {
                    continue;
                }
                toLocal.multiply(inverseWorld, p.getPathTransform());
                composed.addPath(p.getRawPath(), toLocal);
            }
        }
    }
}
