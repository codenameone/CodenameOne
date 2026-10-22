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
package com.codename1.gaming;

import com.codename1.gpu.GltfScene;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.Quaternion;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;

/// One posed, animated instance of a `GltfScene`: a character, a creature, a
/// door that swings. It plays the scene's animation clips, cross fades between
/// them, deforms skinned parts with their skeleton and carries rigid parts
/// (a sword in a hand bone, a helmet on the head) along with the bones they
/// hang from.
///
/// Each triangle primitive of the scene becomes one `Model` with its own
/// vertex buffer, so add the instance to a view with `addTo(GameView)` and move
/// it with `setPosition`, `setRotation` and `setScale` -- every part follows.
/// Skinning runs on the CPU and re-uploads the posed vertices when the pose
/// changes, which keeps it identical on every port and needs nothing from the
/// shaders.
///
/// Many instances can share one scene: the vertex data, the clips and the
/// uploaded textures are shared; only the posed buffers are per instance.
///
/// ```java
/// import com.codename1.gaming.*;
/// import com.codename1.gpu.*;
/// import com.codename1.io.Log;
/// import com.codename1.ui.*;
/// import com.codename1.ui.layouts.BorderLayout;
/// import java.io.InputStream;
///
/// class Knight extends GameView {
///     private AnimatedModel unit;
///
///     protected void onSetup(GraphicsDevice device) {
///         try {
///             InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/knight.glb");
///             GltfScene knight = GltfLoader.loadScene(in);
///             in.close();
///             unit = new AnimatedModel(device, knight);
///             unit.hideNode("Shield_Spike");          // the file carries every prop
///             unit.play("Walking_A", true);
///             unit.addTo(this);
///         } catch (Exception err) {
///             Log.e(err);
///         }
///     }
///
///     protected void update(double deltaSeconds) {
///         if (unit != null) {
///             unit.update((float) deltaSeconds);
///         }
///     }
///
///     void attack() {
///         unit.crossFade("1H_Melee_Attack_Chop", 0.15f, false);
///     }
/// }
///
/// Form f = new Form("Knight", new BorderLayout());
/// f.add(BorderLayout.CENTER, new Knight());
/// f.show();
/// ```
public class AnimatedModel {
    private static final int POSE = 10; // t(3) r(4) s(3)

    private final GltfScene scene;
    private final Model[] parts;
    private final boolean[] hidden;
    private final int[] order;
    private final float[] rest;
    private final float[] pose;
    private final float[] fadePose;
    private final float[] world;
    private final float[] local = new float[16]; //NOPMD SingularField - per-frame scratch, kept to avoid allocating every frame
    private final float[] rot = new float[16];
    private final float[] skinMatrix = new float[16];
    private final float[] jointMatrices;
    private final float[] key = new float[4];

    private GltfScene.Animation clip;
    private float time;
    private boolean loop;
    private float speed = 1f;
    private GltfScene.Animation fadeFrom;
    private float fadeFromTime;
    private boolean fadeFromLoop;
    private float fadeLength;
    private float fadeElapsed;
    private boolean posed;

    private float x;
    private float y;
    private float z;
    private float rotX;
    private float rotY;
    private float rotZ;
    private float scale = 1f;
    private boolean visible = true;

    /// Creates an instance of `scene`, uploading its textures through
    /// `device` the first time any instance needs them. Call it from
    /// `GameView#onSetup(GraphicsDevice)` or later.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that will draw the instance
    ///
    /// - `scene`: a scene from `GltfLoader#loadScene(byte[])`
    public AnimatedModel(GraphicsDevice device, GltfScene scene) {
        this.scene = scene;
        int nodeCount = scene.getNodeCount();
        rest = new float[nodeCount * POSE];
        for (int i = 0; i < nodeCount; i++) {
            GltfScene.Node n = scene.getNode(i);
            System.arraycopy(n.getTranslation(), 0, rest, i * POSE, 3);
            System.arraycopy(n.getRotation(), 0, rest, i * POSE + 3, 4);
            System.arraycopy(n.getScale(), 0, rest, i * POSE + 7, 3);
        }
        pose = new float[rest.length];
        fadePose = new float[rest.length];
        world = new float[nodeCount * 16];
        order = parentsFirst(scene);
        int maxJoints = 0;
        for (int i = 0; i < scene.getSkinCount(); i++) {
            maxJoints = Math.max(maxJoints, scene.getSkin(i).getJoints().length);
        }
        jointMatrices = new float[maxJoints * 16];

        int count = scene.getPrimitiveCount();
        parts = new Model[count];
        hidden = new boolean[count];
        for (int i = 0; i < count; i++) {
            GltfScene.Primitive p = scene.getPrimitive(i);
            VertexBuffer vb = new VertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, p.getVertexCount());
            IndexBuffer ib = new IndexBuffer(p.getIndices().length);
            ib.setData(p.getIndices());
            Material material = new Material(Material.Type.LAMBERT);
            RenderState state = RenderState.opaque();
            if (p.getMaterial() >= 0 && p.getMaterial() < scene.getMaterialCount()) {
                GltfScene.MaterialInfo info = scene.getMaterial(p.getMaterial());
                material.setColor(info.getBaseColor());
                Texture t = scene.getTexture(device, info.getImage());
                if (t != null) {
                    material.setTexture(t);
                }
                if (info.isDoubleSided()) {
                    state.setCullMode(RenderState.CullMode.NONE);
                }
                if (info.isBlend()) {
                    state.setBlendMode(RenderState.BlendMode.ALPHA);
                }
            }
            material.setRenderState(state);
            parts[i] = new Model(new Mesh(vb, ib, PrimitiveType.TRIANGLES), material);
        }
        pose();
    }

    /// The scene this instance poses.
    public GltfScene getScene() {
        return scene;
    }

    /// The models that draw this instance, one per primitive.
    public Model[] getParts() {
        return parts;
    }

    /// Adds every part to `view`.
    public void addTo(GameView view) {
        for (Model part : parts) {
            view.addModel(part);
        }
    }

    /// Removes every part from `view`.
    public void removeFrom(GameView view) {
        for (Model part : parts) {
            view.removeModel(part);
        }
    }

    /// Hides the primitives placed by the node named `name` and by every node
    /// below it. Character files often carry every weapon and accessory a
    /// variant might hold; hide the ones this instance does not use.
    ///
    /// #### Returns
    ///
    /// true when a node of that name exists
    public boolean hideNode(String name) {
        return setNodeVisible(name, false);
    }

    /// Shows or hides the primitives placed by the node named `name` and by
    /// every node below it.
    ///
    /// #### Returns
    ///
    /// true when a node of that name exists
    public boolean setNodeVisible(String name, boolean show) {
        int node = scene.findNode(name);
        if (node < 0) {
            return false;
        }
        for (int i = 0; i < parts.length; i++) {
            if (isUnder(scene.getPrimitive(i).getNode(), node)) {
                hidden[i] = !show;
                parts[i].setVisible(visible && show);
            }
        }
        posed = false;
        return true;
    }

    private boolean isUnder(int node, int ancestor) {
        while (node >= 0) {
            if (node == ancestor) {
                return true;
            }
            node = scene.getNode(node).getParent();
        }
        return false;
    }

    /// Multiplies every part's color by `argb`, for a team tint or a hit flash.
    /// Pass `0xffffffff` to restore the file's colors.
    public void setTint(int argb) {
        for (int i = 0; i < parts.length; i++) {
            GltfScene.Primitive p = scene.getPrimitive(i);
            int base = 0xffffffff;
            if (p.getMaterial() >= 0 && p.getMaterial() < scene.getMaterialCount()) {
                base = scene.getMaterial(p.getMaterial()).getBaseColor();
            }
            parts[i].getMaterial().setColor(multiply(base, argb));
        }
    }

    private static int multiply(int a, int b) {
        int out = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int ca = (a >>> shift) & 0xff;
            int cb = (b >>> shift) & 0xff;
            out |= ((ca * cb + 127) / 255) << shift;
        }
        return out;
    }

    /// Starts the clip named `name` from its beginning, replacing whatever was
    /// playing.
    ///
    /// #### Returns
    ///
    /// false (and nothing changes) when the scene has no such clip
    public boolean play(String name, boolean loop) {
        GltfScene.Animation a = scene.findAnimation(name);
        if (a == null) {
            return false;
        }
        clip = a;
        time = 0f;
        this.loop = loop;
        fadeFrom = null;
        posed = false;
        return true;
    }

    /// Blends from the current pose into the clip named `name` over `seconds`.
    /// Calling it with the clip that is already playing and looping does
    /// nothing, so it is safe to call every frame with the state's clip.
    ///
    /// #### Returns
    ///
    /// false (and nothing changes) when the scene has no such clip
    public boolean crossFade(String name, float seconds, boolean loop) {
        GltfScene.Animation a = scene.findAnimation(name);
        if (a == null) {
            return false;
        }
        if (a == clip && this.loop && loop) { //NOPMD CompareObjectsWithEquals - the same clip instance
            return true;
        }
        if (clip == null || seconds <= 0f) {
            return play(name, loop);
        }
        fadeFrom = clip;
        fadeFromTime = time;
        fadeFromLoop = this.loop;
        fadeLength = seconds;
        fadeElapsed = 0f;
        clip = a;
        time = 0f;
        this.loop = loop;
        posed = false;
        return true;
    }

    /// The name of the clip playing, or null.
    public String getClipName() {
        return clip == null ? null : clip.getName();
    }

    /// The position in the current clip, in seconds.
    public float getClipTime() {
        return time;
    }

    /// Jumps to `seconds` in the current clip.
    public void setClipTime(float seconds) {
        time = seconds;
        posed = false;
    }

    /// True when a non-looping clip has reached its end.
    public boolean isFinished() {
        return clip != null && !loop && time >= clip.getDuration();
    }

    /// The playback rate; 1 is the authored speed.
    public float getSpeed() {
        return speed;
    }

    /// Sets the playback rate; 1 is the authored speed.
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /// Advances the animation by `deltaSeconds` and re-poses the vertices.
    /// Call it once per frame, typically from `GameView#update(double)`.
    public void update(float deltaSeconds) {
        float dt = deltaSeconds * speed;
        if (clip != null) {
            time = advance(clip, time + dt, loop);
            posed = false;
        }
        if (fadeFrom != null) {
            fadeFromTime = advance(fadeFrom, fadeFromTime + dt, fadeFromLoop);
            fadeElapsed += deltaSeconds;
            if (fadeElapsed >= fadeLength) {
                fadeFrom = null;
            }
            posed = false;
        }
        if (!posed && visible) {
            pose();
        }
    }

    private static float advance(GltfScene.Animation a, float t, boolean loop) {
        float d = a.getDuration();
        if (d <= 0f) {
            return 0f;
        }
        if (loop) {
            t = t % d;
            return t < 0f ? t + d : t;
        }
        return t > d ? d : t;
    }

    /// Computes the current pose and writes the deformed vertices of every
    /// visible part. `update(float)` calls this; call it directly only after
    /// changing the clip time of a paused instance.
    public void pose() {
        sample(clip, time, pose);
        if (fadeFrom != null) {
            sample(fadeFrom, fadeFromTime, fadePose);
            float w = fadeLength <= 0f ? 1f : Math.min(1f, fadeElapsed / fadeLength);
            blend(fadePose, pose, w, pose);
        }
        computeWorld();
        int skinned = -1;
        for (int i = 0; i < parts.length; i++) {
            if (hidden[i]) {
                continue;
            }
            GltfScene.Primitive p = scene.getPrimitive(i);
            int skin = scene.getNode(p.getNode()).getSkin();
            if (p.isSkinned() && skin >= 0) {
                if (skinned != skin) {
                    computeJoints(scene.getSkin(skin));
                    skinned = skin;
                }
                skin(p, parts[i].getMesh().getVertices());
            } else {
                rigid(p, p.getNode(), parts[i].getMesh().getVertices());
            }
        }
        posed = true;
    }

    private void sample(GltfScene.Animation a, float t, float[] dst) {
        System.arraycopy(rest, 0, dst, 0, rest.length);
        if (a == null) {
            return;
        }
        GltfScene.Channel[] channels = a.getChannels();
        for (GltfScene.Channel c : channels) {
            int node = c.getNode();
            if (node < 0 || node * POSE >= dst.length) {
                continue;
            }
            c.sample(t, key);
            int base = node * POSE;
            switch (c.getPath()) {
                case GltfScene.Channel.TRANSLATION:
                    System.arraycopy(key, 0, dst, base, 3);
                    break;
                case GltfScene.Channel.ROTATION:
                    System.arraycopy(key, 0, dst, base + 3, 4);
                    break;
                default:
                    System.arraycopy(key, 0, dst, base + 7, 3);
                    break;
            }
        }
    }

    private static void blend(float[] a, float[] b, float w, float[] dst) {
        for (int n = 0; n < a.length; n += POSE) {
            for (int i = 0; i < 3; i++) {
                dst[n + i] = a[n + i] + (b[n + i] - a[n + i]) * w;
                dst[n + 7 + i] = a[n + 7 + i] + (b[n + 7 + i] - a[n + 7 + i]) * w;
            }
            float dot = a[n + 3] * b[n + 3] + a[n + 4] * b[n + 4] + a[n + 5] * b[n + 5] + a[n + 6] * b[n + 6];
            float sign = dot < 0f ? -1f : 1f;
            float len = 0f;
            for (int i = 3; i < 7; i++) {
                float v = a[n + i] + (sign * b[n + i] - a[n + i]) * w;
                dst[n + i] = v;
                len += v * v;
            }
            len = (float) Math.sqrt(len);
            if (len > 0f) {
                for (int i = 3; i < 7; i++) {
                    dst[n + i] /= len;
                }
            }
        }
    }

    private void computeWorld() {
        for (int n : order) {
            int b = n * POSE;
            key[0] = pose[b + 3];
            key[1] = pose[b + 4];
            key[2] = pose[b + 5];
            key[3] = pose[b + 6];
            Quaternion.toMatrix(key, rot);
            float sx = pose[b + 7];
            float sy = pose[b + 8];
            float sz = pose[b + 9];
            // local = T * R * S
            for (int i = 0; i < 3; i++) {
                local[i] = rot[i] * sx;
                local[4 + i] = rot[4 + i] * sy;
                local[8 + i] = rot[8 + i] * sz;
            }
            local[3] = 0f;
            local[7] = 0f;
            local[11] = 0f;
            local[12] = pose[b];
            local[13] = pose[b + 1];
            local[14] = pose[b + 2];
            local[15] = 1f;
            int parent = scene.getNode(n).getParent();
            if (parent < 0) {
                System.arraycopy(local, 0, world, n * 16, 16);
            } else {
                multiplyInto(world, parent * 16, local, 0, world, n * 16);
            }
        }
    }

    private void computeJoints(GltfScene.Skin skin) {
        int[] joints = skin.getJoints();
        float[] ibm = skin.getInverseBindMatrices();
        for (int j = 0; j < joints.length; j++) {
            multiplyInto(world, joints[j] * 16, ibm, j * 16, jointMatrices, j * 16);
        }
    }

    /// `dst[d..] = a[ao..] * b[bo..]`, column major; `dst` must not overlap `b`.
    private static void multiplyInto(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
        for (int c = 0; c < 4; c++) {
            float b0 = b[bo + c * 4];
            float b1 = b[bo + c * 4 + 1];
            float b2 = b[bo + c * 4 + 2];
            float b3 = b[bo + c * 4 + 3];
            for (int r = 0; r < 4; r++) {
                dst[d + c * 4 + r] = a[ao + r] * b0 + a[ao + 4 + r] * b1 + a[ao + 8 + r] * b2 + a[ao + 12 + r] * b3;
            }
        }
    }

    private void skin(GltfScene.Primitive p, VertexBuffer vb) {
        float[] pos = p.getPositions();
        float[] nor = p.getNormals();
        float[] uv = p.getTexcoords();
        int[] joints = p.getJoints();
        float[] weights = p.getWeights();
        float[] out = vb.getData();
        float[] m = skinMatrix;
        int jointLimit = jointMatrices.length / 16;
        int count = p.getVertexCount();
        for (int v = 0; v < count; v++) {
            for (int i = 0; i < 16; i++) {
                m[i] = 0f;
            }
            for (int k = 0; k < 4; k++) {
                float w = weights[v * 4 + k];
                int j = joints[v * 4 + k];
                if (w == 0f || j < 0 || j >= jointLimit) {
                    continue;
                }
                int o = j * 16;
                for (int i = 0; i < 16; i++) {
                    m[i] += jointMatrices[o + i] * w;
                }
            }
            write(m, pos, nor, uv, v, out);
        }
        vb.setDirty();
    }

    private void rigid(GltfScene.Primitive p, int node, VertexBuffer vb) {
        float[] out = vb.getData();
        System.arraycopy(world, node * 16, skinMatrix, 0, 16);
        int count = p.getVertexCount();
        float[] pos = p.getPositions();
        float[] nor = p.getNormals();
        float[] uv = p.getTexcoords();
        for (int v = 0; v < count; v++) {
            write(skinMatrix, pos, nor, uv, v, out);
        }
        vb.setDirty();
    }

    private static void write(float[] m, float[] pos, float[] nor, float[] uv, int v, float[] out) {
        int i3 = v * 3;
        int o = v * 8;
        float px = pos[i3];
        float py = pos[i3 + 1];
        float pz = pos[i3 + 2];
        out[o] = m[0] * px + m[4] * py + m[8] * pz + m[12];
        out[o + 1] = m[1] * px + m[5] * py + m[9] * pz + m[13];
        out[o + 2] = m[2] * px + m[6] * py + m[10] * pz + m[14];
        float nx = nor[i3];
        float ny = nor[i3 + 1];
        float nz = nor[i3 + 2];
        // joint matrices carry no shear in practice, so the upper 3x3
        // re-normalized is the normal transform.
        float tx = m[0] * nx + m[4] * ny + m[8] * nz;
        float ty = m[1] * nx + m[5] * ny + m[9] * nz;
        float tz = m[2] * nx + m[6] * ny + m[10] * nz;
        float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (len > 0f) {
            tx /= len;
            ty /= len;
            tz /= len;
        }
        out[o + 3] = tx;
        out[o + 4] = ty;
        out[o + 5] = tz;
        if (uv != null) {
            out[o + 6] = uv[v * 2];
            out[o + 7] = uv[v * 2 + 1];
        }
    }

    /// Copies the current world matrix of the node named `name` (in the
    /// instance's own space, before `setPosition` and friends) into `dst16`.
    /// Use it to attach an effect to a bone, such as a spark at a sword tip.
    ///
    /// #### Returns
    ///
    /// false when no node has that name
    public boolean getNodeMatrix(String name, float[] dst16) {
        int n = scene.findNode(name);
        if (n < 0) {
            return false;
        }
        if (!posed) {
            pose();
        }
        System.arraycopy(world, n * 16, dst16, 0, 16);
        return true;
    }

    private static int[] parentsFirst(GltfScene scene) {
        int n = scene.getNodeCount();
        int[] out = new int[n];
        int[] stack = new int[n];
        int count = 0;
        for (int r = 0; r < n; r++) {
            if (scene.getNode(r).getParent() >= 0) {
                continue;
            }
            int sp = 0;
            stack[sp++] = r;
            while (sp > 0 && count < n) {
                int node = stack[--sp];
                out[count++] = node;
                int[] children = scene.getNode(node).getChildren();
                for (int c = children.length - 1; c >= 0; c--) {
                    if (sp < n && children[c] >= 0 && children[c] < n) {
                        stack[sp++] = children[c];
                    }
                }
            }
        }
        if (count < n) {
            int[] trimmed = new int[count];
            System.arraycopy(out, 0, trimmed, 0, count);
            return trimmed;
        }
        return out;
    }

    /// Places the instance.
    public AnimatedModel setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        for (Model part : parts) {
            part.setPosition(x, y, z);
        }
        return this;
    }

    /// Rotates the instance, in degrees about each axis.
    public AnimatedModel setRotation(float degX, float degY, float degZ) {
        rotX = degX;
        rotY = degY;
        rotZ = degZ;
        for (Model part : parts) {
            part.setRotation(degX, degY, degZ);
        }
        return this;
    }

    /// Scales the instance uniformly.
    public AnimatedModel setScale(float scale) {
        this.scale = scale;
        for (Model part : parts) {
            part.setScale(scale);
        }
        return this;
    }

    /// The x position.
    public float getX() {
        return x;
    }

    /// The y position.
    public float getY() {
        return y;
    }

    /// The z position.
    public float getZ() {
        return z;
    }

    /// The rotation about the y axis in degrees.
    public float getRotationY() {
        return rotY;
    }

    /// The rotation about the x axis in degrees.
    public float getRotationX() {
        return rotX;
    }

    /// The rotation about the z axis in degrees.
    public float getRotationZ() {
        return rotZ;
    }

    /// The uniform scale.
    public float getScale() {
        return scale;
    }

    /// True unless hidden with `setVisible(false)`.
    public boolean isVisible() {
        return visible;
    }

    /// Shows or hides every part. A hidden instance skips its skinning work.
    public void setVisible(boolean visible) {
        this.visible = visible;
        for (int i = 0; i < parts.length; i++) {
            parts[i].setVisible(visible && !hidden[i]);
        }
    }
}
