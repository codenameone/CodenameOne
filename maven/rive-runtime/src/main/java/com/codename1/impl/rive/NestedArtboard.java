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

/// Port of `NestedArtboard`: draws an instance of another artboard of the
/// file at this node's world transform, and advances it and its nested
/// animations and state machines with the host.
public class NestedArtboard extends NestedArtboardBase {
    /// Instancing an artboard instances its nested artboards; a file whose
    /// artboards nest each other in a cycle stops here.
    private static final int MAX_NESTING = 16;
    private static int nesting;

    private final ArrayList<NestedAnimation> nestedAnimations = new ArrayList<NestedAnimation>();
    private Artboard referenced;
    private Mesh mesh;

    void setMesh(Mesh m) {
        mesh = m;
    }

    public Mesh getMesh() {
        return mesh;
    }

    void addNestedAnimation(NestedAnimation a) {
        nestedAnimations.add(a);
    }

    public ArrayList<NestedAnimation> getNestedAnimations() {
        return nestedAnimations;
    }

    /// The nested artboard instance, or null when it could not be made.
    public Artboard getArtboardInstance() {
        return referenced;
    }

    @Override
    public int onAddedClean(Artboard context) {
        int code = super.onAddedClean(context);
        RiveFileContext file = context != null ? context.getFile() : null;
        if (file != null && nesting < MAX_NESTING) {
            nesting++;
            try {
                referenced = file.instanceArtboard(mArtboardId);
            } finally {
                nesting--;
            }
        }
        if (referenced != null) {
            for (int i = 0; i < nestedAnimations.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                nestedAnimations.get(i).initializeAnimation(referenced);
            }
        }
        return code;
    }

    /// Port of `NestedArtboard::willDraw`.
    public boolean willDraw() {
        return !isHidden() && referenced != null;
    }

    @Override
    public void draw(RiveRenderer renderer) {
        if (!willDraw()) {
            return;
        }
        boolean clipped = clip(renderer);
        renderer.save();
        renderer.transform(worldTransform);
        referenced.drawHosted(renderer);
        renderer.restore();
        if (clipped) {
            renderer.restore();
        }
    }

    /// Binds the nested artboard for its host's `parent` context (upstream
    /// `NestedArtboard::bindViewModelInstance` / `internalDataContext`): the
    /// instance its data bind path names, in a context whose parent is the
    /// host's, or the host's context itself when it has no path.
    public void bindDataContext(DataContext parent) {
        if (referenced == null) {
            return;
        }
        if (parent == null) {
            referenced.bindDataContext(null);
            return;
        }
        int[] path = DataBindContext.decodePath(getDataBindPathIds());
        if (path.length == 0) {
            referenced.bindDataContext(parent);
            return;
        }
        ViewModelInstance instance = parent.resolveInstance(path);
        referenced.bindDataContext(instance == null ? parent : new DataContext(instance, parent));
    }

    /// Port of `NestedArtboard::advanceComponent`: advances the nested
    /// animations, then the nested artboard. Answers whether it keeps going.
    public boolean advanceNested(float elapsedSeconds) {
        if (referenced == null || isCollapsed() || mIsPaused) {
            return false;
        }
        float local = elapsedSeconds * mSpeed;
        if (mQuantize > 0f) {
            // Quantized playback is approximated by the plain elapsed time.
            local = elapsedSeconds * mSpeed;
        }
        boolean keepGoing = false;
        for (int i = 0; i < nestedAnimations.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (nestedAnimations.get(i).advance(local)) {
                keepGoing = true;
            }
        }
        if (referenced.advance(local)) {
            keepGoing = true;
        }
        if (keepGoing) {
            addDirt(DIRT_PAINT);
        }
        return keepGoing;
    }
}
