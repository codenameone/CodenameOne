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

/// Clips its parent's drawables to the shapes under its source node, in
/// artboard space.
public class ClippingShape extends ClippingShapeBase {
    private final ArrayList<Shape> shapes = new ArrayList<Shape>();
    private final RivePath clipPath = new RivePath();

    public RivePath getClipPath() {
        return clipPath;
    }

    @Override
    public int onAddedClean(Artboard context) {
        Core s = context.resolve(mSourceId);
        if (!(s instanceof Node)) {
            return STATUS_OK;
        }
        Node source = (Node) s;
        ArrayList<Core> objects = context.getObjects();
        for (int i = 0; i < objects.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Core o = objects.get(i);
            if (o instanceof Shape) {
                for (ContainerComponent p = (Shape) o; p != null; p = p.getParent()) {
                    if (p == source) { // NOPMD CompareObjectsWithEquals - identity is the intent
                        shapes.add((Shape) o);
                        break;
                    }
                }
            }
        }
        return STATUS_OK;
    }

    @Override
    public void buildDependencies() {
        for (int i = 0; i < shapes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            shapes.get(i).getComposer().addDependent(this);
            shapes.get(i).addDependent(this);
        }
    }

    @Override
    public void update(int value) {
        if (hasDirt(value, DIRT_PATH | DIRT_WORLD_TRANSFORM | DIRT_CLIPPING)) {
            clipPath.rewind();
            clipPath.fillRule = mFillRule;
            for (int i = 0; i < shapes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                Shape s = shapes.get(i);
                clipPath.addPath(s.getComposedPath(), s.getWorldTransform());
            }
        }
    }

    @Override
    protected void isVisibleChanged() {
        addDirt(DIRT_CLIPPING);
    }
}
