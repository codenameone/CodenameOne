/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.gpu;

import com.codename1.ui.Image;

/// The whole of a glTF 2.0 file as plain data: every node of the scene graph,
/// every triangle primitive, the materials, the skins and the animation clips.
/// Produced by `GltfLoader#loadScene(byte[])`.
///
/// Nothing here is uploaded to a `GraphicsDevice`; the vertex data is kept on the
/// CPU so a renderer can pose it. `com.codename1.gaming.AnimatedModel` is the
/// ready made consumer: it samples the clips, skins the vertices and draws the
/// result as ordinary `Mesh`es.
///
/// Node transforms follow the glTF convention: a node's local matrix is
/// `T * R * S` of its translation, rotation (an `x, y, z, w` quaternion) and
/// scale, and its world matrix is its parent's world matrix times that.
public final class GltfScene {
    final Node[] nodes;
    final int[] roots;
    final Primitive[] primitives;
    final MaterialInfo[] materials;
    final Image[] images;
    final Skin[] skins;
    final Animation[] animations;
    private Texture[] textures;
    private GraphicsDevice textureDevice;

    GltfScene(Node[] nodes, int[] roots, Primitive[] primitives, MaterialInfo[] materials,
              Image[] images, Skin[] skins, Animation[] animations) {
        this.nodes = nodes;
        this.roots = roots;
        this.primitives = primitives;
        this.materials = materials;
        this.images = images;
        this.skins = skins;
        this.animations = animations;
    }

    /// The number of nodes in the file.
    public int getNodeCount() {
        return nodes.length;
    }

    /// The node at `index`.
    public Node getNode(int index) {
        return nodes[index];
    }

    /// The index of the first node named `name`, or -1.
    public int findNode(String name) {
        for (int i = 0; i < nodes.length; i++) {
            if (name.equals(nodes[i].name)) {
                return i;
            }
        }
        return -1;
    }

    /// The indices of the scene's root nodes, parents before children.
    public int[] getRoots() {
        return roots;
    }

    /// The number of triangle primitives across every mesh node.
    public int getPrimitiveCount() {
        return primitives.length;
    }

    /// The primitive at `index`.
    public Primitive getPrimitive(int index) {
        return primitives[index];
    }

    /// The number of materials.
    public int getMaterialCount() {
        return materials.length;
    }

    /// The material at `index`.
    public MaterialInfo getMaterial(int index) {
        return materials[index];
    }

    /// The decoded embedded image at `index`, or null when it could not be read.
    public Image getImage(int index) {
        return images[index];
    }

    /// The texture for image `index` on `device`, uploaded on first use and then
    /// shared by every model drawn from this scene. Returns null when the image
    /// could not be decoded. Asking with a different device uploads afresh.
    public Texture getTexture(GraphicsDevice device, int index) {
        if (index < 0 || index >= images.length || images[index] == null) {
            return null;
        }
        if (textures == null || textureDevice != device) { //NOPMD CompareObjectsWithEquals - a different device instance needs its own textures
            textures = new Texture[images.length];
            textureDevice = device;
        }
        if (textures[index] == null) {
            textures[index] = device.createTexture(images[index]);
            textures[index].setFilter(Texture.Filter.LINEAR);
        }
        return textures[index];
    }

    /// The number of skins.
    public int getSkinCount() {
        return skins.length;
    }

    /// The skin at `index`.
    public Skin getSkin(int index) {
        return skins[index];
    }

    /// The number of animation clips.
    public int getAnimationCount() {
        return animations.length;
    }

    /// The clip at `index`.
    public Animation getAnimation(int index) {
        return animations[index];
    }

    /// The first clip named `name`, or null.
    public Animation findAnimation(String name) {
        for (Animation a : animations) {
            if (name.equals(a.name)) {
                return a;
            }
        }
        return null;
    }

    /// One node of the scene graph.
    public static final class Node {
        final String name;
        final int parent;
        final int[] children;
        final float[] translation;
        final float[] rotation;
        final float[] scale;
        final int skin;

        Node(String name, int parent, int[] children, float[] translation, float[] rotation,
                float[] scale, int skin) {
            this.name = name;
            this.parent = parent;
            this.children = children;
            this.translation = translation;
            this.rotation = rotation;
            this.scale = scale;
            this.skin = skin;
        }

        /// The node's name, or an empty string.
        public String getName() {
            return name;
        }

        /// The parent node's index, or -1 for a root.
        public int getParent() {
            return parent;
        }

        /// The child node indices.
        public int[] getChildren() {
            return children;
        }

        /// The rest translation `x, y, z`.
        public float[] getTranslation() {
            return translation;
        }

        /// The rest rotation as an `x, y, z, w` quaternion.
        public float[] getRotation() {
            return rotation;
        }

        /// The rest scale `x, y, z`.
        public float[] getScale() {
            return scale;
        }

        /// The index of the skin that deforms this node's mesh, or -1.
        public int getSkin() {
            return skin;
        }
    }

    /// One triangle primitive, with its vertex data in the node's local space.
    public static final class Primitive {
        final int node;
        final int material;
        final float[] positions;
        final float[] normals;
        final float[] texcoords;
        final int[] indices;
        final int[] joints;
        final float[] weights;

        Primitive(int node, int material, float[] positions, float[] normals, float[] texcoords,
                  int[] indices, int[] joints, float[] weights) {
            this.node = node;
            this.material = material;
            this.positions = positions;
            this.normals = normals;
            this.texcoords = texcoords;
            this.indices = indices;
            this.joints = joints;
            this.weights = weights;
        }

        /// The node that places this primitive.
        public int getNode() {
            return node;
        }

        /// The material index, or -1.
        public int getMaterial() {
            return material;
        }

        /// The number of vertices.
        public int getVertexCount() {
            return positions.length / 3;
        }

        /// `x, y, z` per vertex.
        public float[] getPositions() {
            return positions;
        }

        /// `x, y, z` per vertex; computed flat normals when the file has none.
        public float[] getNormals() {
            return normals;
        }

        /// `u, v` per vertex, or null.
        public float[] getTexcoords() {
            return texcoords;
        }

        /// Triangle indices.
        public int[] getIndices() {
            return indices;
        }

        /// Four joint indices per vertex (indices into the skin's joint list), or
        /// null when the primitive is not skinned.
        public int[] getJoints() {
            return joints;
        }

        /// Four weights per vertex, or null when the primitive is not skinned.
        public float[] getWeights() {
            return weights;
        }

        /// True when the primitive carries joints and weights.
        public boolean isSkinned() {
            return joints != null && weights != null;
        }
    }

    /// The parts of a glTF material a `Material` can express.
    public static final class MaterialInfo {
        final String name;
        final int baseColor;
        final int image;
        final boolean doubleSided;
        final boolean blend;

        MaterialInfo(String name, int baseColor, int image, boolean doubleSided, boolean blend) {
            this.name = name;
            this.baseColor = baseColor;
            this.image = image;
            this.doubleSided = doubleSided;
            this.blend = blend;
        }

        /// The material's name, or an empty string.
        public String getName() {
            return name;
        }

        /// The base color factor as ARGB.
        public int getBaseColor() {
            return baseColor;
        }

        /// The index of the base color image, or -1.
        public int getImage() {
            return image;
        }

        /// True when back faces must not be culled.
        public boolean isDoubleSided() {
            return doubleSided;
        }

        /// True when the material's alpha mode is `BLEND`.
        public boolean isBlend() {
            return blend;
        }
    }

    /// A skeleton: the joint nodes and the matrices that take a vertex from the
    /// mesh's bind pose into each joint's space.
    public static final class Skin {
        final int[] joints;
        final float[] inverseBindMatrices;

        Skin(int[] joints, float[] inverseBindMatrices) {
            this.joints = joints;
            this.inverseBindMatrices = inverseBindMatrices;
        }

        /// The joint node indices.
        public int[] getJoints() {
            return joints;
        }

        /// 16 column-major floats per joint.
        public float[] getInverseBindMatrices() {
            return inverseBindMatrices;
        }
    }

    /// A named animation clip.
    public static final class Animation {
        final String name;
        final Channel[] channels;
        final float duration;

        Animation(String name, Channel[] channels) {
            this.name = name;
            this.channels = channels;
            float d = 0f;
            for (Channel c : channels) {
                float[] t = c.times;
                if (t.length > 0 && t[t.length - 1] > d) {
                    d = t[t.length - 1];
                }
            }
            this.duration = d;
        }

        /// The clip's name, or an empty string.
        public String getName() {
            return name;
        }

        /// The clip's length in seconds.
        public float getDuration() {
            return duration;
        }

        /// The channels that animate node properties.
        public Channel[] getChannels() {
            return channels;
        }
    }

    /// Keyframes for one property of one node.
    public static final class Channel {
        /// The channel animates a node's translation.
        public static final int TRANSLATION = 0;
        /// The channel animates a node's rotation.
        public static final int ROTATION = 1;
        /// The channel animates a node's scale.
        public static final int SCALE = 2;

        final int node;
        final int path;
        final float[] times;
        final float[] values;
        final boolean step;

        Channel(int node, int path, float[] times, float[] values, boolean step) {
            this.node = node;
            this.path = path;
            this.times = times;
            this.values = values;
            this.step = step;
        }

        /// The animated node.
        public int getNode() {
            return node;
        }

        /// `TRANSLATION`, `ROTATION` or `SCALE`.
        public int getPath() {
            return path;
        }

        /// Keyframe times in seconds, ascending.
        public float[] getTimes() {
            return times;
        }

        /// The keyframe values: 3 floats per key, 4 for a rotation.
        public float[] getValues() {
            return values;
        }

        /// True for `STEP` interpolation, false for `LINEAR`.
        public boolean isStep() {
            return step;
        }

        /// Samples the channel at `time` (clamped to the keys) into `dst`.
        public void sample(float time, float[] dst) {
            int width = path == ROTATION ? 4 : 3;
            int n = times.length;
            if (n == 0) {
                return;
            }
            if (time <= times[0] || n == 1) {
                System.arraycopy(values, 0, dst, 0, width);
                return;
            }
            if (time >= times[n - 1]) {
                System.arraycopy(values, (n - 1) * width, dst, 0, width);
                return;
            }
            int lo = 0;
            int hi = n - 1;
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (times[mid] <= time) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            if (step) {
                System.arraycopy(values, lo * width, dst, 0, width);
                return;
            }
            float span = times[hi] - times[lo];
            float t = span > 0f ? (time - times[lo]) / span : 0f;
            int a = lo * width;
            int b = hi * width;
            if (path == ROTATION) {
                nlerp(values, a, b, t, dst);
                return;
            }
            for (int i = 0; i < width; i++) {
                dst[i] = values[a + i] + (values[b + i] - values[a + i]) * t;
            }
        }

        /// Normalized linear interpolation along the shorter arc. Keyframes are
        /// dense enough that the difference from slerp is not visible, and it
        /// costs no trigonometry per joint per frame.
        static void nlerp(float[] v, int a, int b, float t, float[] dst) {
            float dot = v[a] * v[b] + v[a + 1] * v[b + 1] + v[a + 2] * v[b + 2] + v[a + 3] * v[b + 3];
            float sign = dot < 0f ? -1f : 1f;
            float x = v[a] + (sign * v[b] - v[a]) * t;
            float y = v[a + 1] + (sign * v[b + 1] - v[a + 1]) * t;
            float z = v[a + 2] + (sign * v[b + 2] - v[a + 2]) * t;
            float w = v[a + 3] + (sign * v[b + 3] - v[a + 3]) * t;
            float len = (float) Math.sqrt(x * x + y * y + z * z + w * w);
            if (len > 0f) {
                x /= len;
                y /= len;
                z /= len;
                w /= len;
            }
            dst[0] = x;
            dst[1] = y;
            dst[2] = z;
            dst[3] = w;
        }
    }
}
