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

/// Paths painted by the fills and strokes among its children. The paths are
/// composed into one path in the shape's local space each time one changes.
public class Shape extends ShapeBase {
    protected final ArrayList<Path> paths = new ArrayList<Path>();
    protected final ArrayList<ShapePaint> paints = new ArrayList<ShapePaint>();
    protected final RivePath composed = new RivePath();
    private PathComposer composer;

    public void addPath(Path p) {
        paths.add(p);
    }

    public void addPaint(ShapePaint p) {
        paints.add(p);
    }

    public ArrayList<Path> getPaths() {
        return paths;
    }

    public RivePath getComposedPath() {
        return composed;
    }

    public PathComposer getComposer() {
        return composer;
    }

    public void pathChanged() {
        if (composer != null) {
            composer.addDirt(DIRT_PATH, true);
        }
    }

    @Override
    public int onAddedClean(Artboard context) {
        int code = super.onAddedClean(context);
        composer = new PathComposer(this);
        composer.setArtboard(context);
        context.addRuntimeComponent(composer);
        return code;
    }

    @Override
    public void update(int value) {
        super.update(value);
        if (hasDirt(value, DIRT_WORLD_TRANSFORM)) {
            pathChanged();
        }
        if (hasDirt(value, DIRT_RENDER_OPACITY | DIRT_PAINT)) {
            for (int i = 0; i < paints.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                paints.get(i).updatePaint(renderOpacity);
            }
        }
    }

    @Override
    protected void onDirty(int value) {
        if (hasDirt(value, DIRT_RENDER_OPACITY)) {
            for (int i = 0; i < paints.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                paints.get(i).addDirt(DIRT_PAINT);
            }
        }
    }

    @Override
    public void draw(RiveRenderer renderer) {
        if (isHidden() || composed.isEmpty()) {
            return;
        }
        boolean clipped = clip(renderer);
        renderer.save();
        renderer.transform(worldTransform);
        for (int i = 0; i < paints.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ShapePaint p = paints.get(i);
            if (!p.isVisible() || p.isCollapsed()) {
                continue;
            }
            composed.fillRule = p instanceof Fill ? ((Fill) p).getFillRule() : RivePath.FILL_NON_ZERO;
            renderer.drawPath(p.effectPath(composed), p.getRenderPaint());
        }
        renderer.restore();
        if (clipped) {
            renderer.restore();
        }
    }
}
