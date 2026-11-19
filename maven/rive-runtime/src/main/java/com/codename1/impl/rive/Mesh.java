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

/// Port of `Mesh`: a triangle mesh that deforms its parent [Image]. Its
/// vertices may be skinned, in which case they are already in world space.
public class Mesh extends MeshBase implements Skinnable {
    private final ArrayList<MeshVertex> vertices = new ArrayList<MeshVertex>();
    private int[] indices;
    private int indexCount;
    private float[] vertexBuffer = new float[0];
    private float[] uvBuffer = new float[0];
    private boolean vertexBufferDirty = true;
    private Skin skin;

    void addVertex(MeshVertex v) {
        vertices.add(v);
    }

    public ArrayList<MeshVertex> getVertices() {
        return vertices;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public int[] getIndices() {
        return indices;
    }

    @Override
    public void setSkin(Skin s) {
        skin = s;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    @Override
    public void markSkinDirty() {
        addDirt(DIRT_VERTICES);
    }

    /// Port of `Mesh::markDrawableDirty`.
    public void markDrawableDirty() {
        if (skin != null) {
            skin.addDirt(DIRT_SKIN);
        }
        addDirt(DIRT_VERTICES);
    }

    /// Port of `Mesh::decodeTriangleIndexBytes`: var-uint indices.
    @Override
    protected void decodeTriangleIndexBytes(byte[] value) {
        int[] out = new int[value.length];
        int n = 0;
        int pos = 0;
        while (pos < value.length) {
            int result = 0;
            int shift = 0;
            while (pos < value.length) {
                int b = value[pos++] & 0xff;
                result |= (b & 0x7f) << shift;
                shift += 7;
                if ((b & 0x80) == 0) {
                    break;
                }
            }
            out[n++] = result & 0xffff;
        }
        indices = out;
        indexCount = n;
    }

    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        if (code != STATUS_OK) {
            return code;
        }
        if (parent instanceof Image) {
            ((Image) parent).setMesh(this);
        } else if (parent instanceof NestedArtboard) {
            // Upstream's other MeshHost: deforms the nested artboard's
            // drawing. Kept valid; the nested artboard draws undeformed.
            ((NestedArtboard) parent).setMesh(this);
        } else {
            return STATUS_MISSING;
        }
        return STATUS_OK;
    }

    @Override
    public int onAddedClean(Artboard context) {
        if (indices == null) {
            return STATUS_INVALID;
        }
        for (int i = 0; i < indexCount; i++) {
            if (indices[i] >= vertices.size()) {
                return STATUS_INVALID;
            }
        }
        int n = vertices.size();
        vertexBuffer = new float[n * 2];
        uvBuffer = new float[n * 2];
        for (int i = 0; i < n; i++) {
            MeshVertex v = vertices.get(i);
            uvBuffer[i * 2] = v.getU();
            uvBuffer[i * 2 + 1] = v.getV();
        }
        return super.onAddedClean(context);
    }

    /// Port of `Mesh::hasTriangles`.
    public boolean hasTriangles() {
        return indices != null && indexCount > 0 && indexCount % 3 == 0;
    }

    @Override
    public void buildDependencies() {
        super.buildDependencies();
        if (skin != null) {
            skin.addDependent(this);
        }
        if (parent != null) {
            parent.addDependent(this);
        }
    }

    @Override
    public void update(int value) {
        if (hasDirt(value, DIRT_VERTICES)) {
            if (skin != null) {
                skin.deform(vertices);
            }
            vertexBufferDirty = true;
        }
        super.update(value);
    }

    /// Port of `Mesh::draw`; the caller has saved the renderer.
    void draw(RiveRenderer renderer, Object image, int blendMode, float opacity) {
        if (vertexBufferDirty) {
            for (int i = 0; i < vertices.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                MeshVertex v = vertices.get(i);
                vertexBuffer[i * 2] = v.getRenderX();
                vertexBuffer[i * 2 + 1] = v.getRenderY();
            }
            vertexBufferDirty = false;
        }
        if (skin == null && parent instanceof WorldTransformComponent) {
            renderer.transform(((WorldTransformComponent) parent).getWorldTransform());
        }
        renderer.drawImageMesh(image, vertexBuffer, uvBuffer, vertices.size(), indices, indexCount, blendMode, opacity);
    }
}
