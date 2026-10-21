/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.GltfScene;
import com.codename1.gpu.GpuCapabilities;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.util.Base64;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AnimatedModelTest extends UITestBase {
    private static final float EPS = 1e-4f;

    static final class StubDevice extends GraphicsDevice {
        public GpuCapabilities getCapabilities() {
            throw new UnsupportedOperationException();
        }

        public Texture createTexture(Image image) {
            return new Texture(image.getWidth(), image.getHeight());
        }

        public Texture createTexture(int width, int height, int[] argb) {
            return new Texture(width, height);
        }

        public void clear(int argbColor, boolean color, boolean depth) {
        }

        public void setViewport(int x, int y, int width, int height) {
        }

        public void draw(Mesh mesh, Material material, float[] modelMatrix) {
        }

        public void dispose(VertexBuffer buffer) {
        }

        public void dispose(IndexBuffer buffer) {
        }

        public void dispose(Texture texture) {
        }
    }

    private static void putFloat(byte[] b, int p, float f) {
        int bits = Float.floatToIntBits(f);
        b[p] = (byte) bits;
        b[p + 1] = (byte) (bits >> 8);
        b[p + 2] = (byte) (bits >> 16);
        b[p + 3] = (byte) (bits >> 24);
    }

    private static void putFloats(byte[] b, int p, float[] f) {
        for (int i = 0; i < f.length; i++) {
            putFloat(b, p + i * 4, f[i]);
        }
    }

    /// A two-bone arm: a skinned triangle bound to the second bone, a rigid
    /// prop parented to that bone, and a clip that bends it 90 degrees about z.
    static byte[] armGltf() {
        byte[] buf = new byte[308];
        putFloats(buf, 0, new float[]{0, 1, 0, 1, 1, 0, 0, 2, 0});
        putFloats(buf, 36, new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0});
        buf[72] = 0;
        buf[74] = 1;
        buf[76] = 2;
        for (int v = 0; v < 3; v++) {
            buf[80 + v * 4] = 1; // joint 1 = "arm"
            putFloat(buf, 92 + v * 16, 1f);
        }
        float[] ibm = new float[32];
        ibm[0] = 1; ibm[5] = 1; ibm[10] = 1; ibm[15] = 1;
        ibm[16] = 1; ibm[21] = 1; ibm[26] = 1; ibm[31] = 1; ibm[29] = -1;
        putFloats(buf, 140, ibm);
        putFloats(buf, 268, new float[]{0, 1});
        float s = (float) Math.sqrt(0.5);
        putFloats(buf, 276, new float[]{0, 0, 0, 1, 0, 0, s, s});
        String uri = "data:application/octet-stream;base64," + Base64.encodeNoNewline(buf);
        String json = "{\"asset\":{\"version\":\"2.0\"},"
                + "\"buffers\":[{\"byteLength\":308,\"uri\":\"" + uri + "\"}],"
                + "\"bufferViews\":["
                + "{\"buffer\":0,\"byteOffset\":0,\"byteLength\":36},"
                + "{\"buffer\":0,\"byteOffset\":36,\"byteLength\":36},"
                + "{\"buffer\":0,\"byteOffset\":72,\"byteLength\":6},"
                + "{\"buffer\":0,\"byteOffset\":80,\"byteLength\":12},"
                + "{\"buffer\":0,\"byteOffset\":92,\"byteLength\":48},"
                + "{\"buffer\":0,\"byteOffset\":140,\"byteLength\":128},"
                + "{\"buffer\":0,\"byteOffset\":268,\"byteLength\":8},"
                + "{\"buffer\":0,\"byteOffset\":276,\"byteLength\":32}],"
                + "\"accessors\":["
                + "{\"bufferView\":0,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"},"
                + "{\"bufferView\":1,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"},"
                + "{\"bufferView\":2,\"componentType\":5123,\"count\":3,\"type\":\"SCALAR\"},"
                + "{\"bufferView\":3,\"componentType\":5121,\"count\":3,\"type\":\"VEC4\"},"
                + "{\"bufferView\":4,\"componentType\":5126,\"count\":3,\"type\":\"VEC4\"},"
                + "{\"bufferView\":5,\"componentType\":5126,\"count\":2,\"type\":\"MAT4\"},"
                + "{\"bufferView\":6,\"componentType\":5126,\"count\":2,\"type\":\"SCALAR\"},"
                + "{\"bufferView\":7,\"componentType\":5126,\"count\":2,\"type\":\"VEC4\"}],"
                + "\"materials\":[{\"name\":\"red\",\"doubleSided\":true,"
                + "\"pbrMetallicRoughness\":{\"baseColorFactor\":[1,0,0,1]}}],"
                + "\"meshes\":["
                + "{\"primitives\":[{\"attributes\":{\"POSITION\":0,\"JOINTS_0\":3,\"WEIGHTS_0\":4},"
                + "\"indices\":2,\"material\":0}]},"
                + "{\"primitives\":[{\"attributes\":{\"POSITION\":1},\"indices\":2}]}],"
                + "\"skins\":[{\"joints\":[1,2],\"inverseBindMatrices\":5}],"
                + "\"nodes\":["
                + "{\"name\":\"Armature\",\"children\":[1,3]},"
                + "{\"name\":\"root_bone\",\"children\":[2]},"
                + "{\"name\":\"arm\",\"translation\":[0,1,0],\"children\":[4]},"
                + "{\"name\":\"body\",\"mesh\":0,\"skin\":0},"
                + "{\"name\":\"prop\",\"mesh\":1}],"
                + "\"scenes\":[{\"nodes\":[0]}],"
                + "\"animations\":[{\"name\":\"Bend\",\"samplers\":[{\"input\":6,\"output\":7}],"
                + "\"channels\":[{\"sampler\":0,\"target\":{\"node\":2,\"path\":\"rotation\"}}]}]}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertVertex(float[] data, int v, float x, float y, float z) {
        assertEquals(x, data[v * 8], EPS, "x of vertex " + v);
        assertEquals(y, data[v * 8 + 1], EPS, "y of vertex " + v);
        assertEquals(z, data[v * 8 + 2], EPS, "z of vertex " + v);
    }

    @Test
    void loadsTheWholeScene() {
        GltfScene scene = GltfLoader.loadScene(armGltf());
        assertEquals(5, scene.getNodeCount());
        assertEquals(2, scene.getPrimitiveCount());
        assertEquals(1, scene.getSkinCount());
        assertEquals(2, scene.findNode("arm"));
        assertEquals(1, scene.getNode(2).getParent());
        assertTrue(scene.getPrimitive(0).isSkinned());
        assertFalse(scene.getPrimitive(1).isSkinned());
        assertEquals(0xffff0000, scene.getMaterial(0).getBaseColor());
        assertTrue(scene.getMaterial(0).isDoubleSided());
        GltfScene.Animation bend = scene.findAnimation("Bend");
        assertNotNull(bend);
        assertEquals(1f, bend.getDuration(), EPS);
        assertArrayEquals(new int[]{0}, scene.getRoots());
    }

    @Test
    void restPoseMatchesTheBindPose() {
        AnimatedModel m = new AnimatedModel(new StubDevice(), GltfLoader.loadScene(armGltf()));
        float[] skinned = m.getParts()[0].getMesh().getVertices().getData();
        assertVertex(skinned, 1, 1, 1, 0);
        float[] prop = m.getParts()[1].getMesh().getVertices().getData();
        assertVertex(prop, 1, 1, 1, 0); // carried up by the arm's translation
        assertEquals(0xffff0000, m.getParts()[0].getMaterial().getColor());
    }

    @Test
    void skinnedAndRigidPartsFollowTheBone() {
        AnimatedModel m = new AnimatedModel(new StubDevice(), GltfLoader.loadScene(armGltf()));
        assertTrue(m.play("Bend", false));
        m.update(1f);
        assertTrue(m.isFinished());
        float[] skinned = m.getParts()[0].getMesh().getVertices().getData();
        assertVertex(skinned, 0, 0, 1, 0);
        assertVertex(skinned, 1, 0, 2, 0);
        assertVertex(skinned, 2, -1, 1, 0);
        assertEquals(0f, skinned[1 * 8 + 3], EPS); // normal rotated with it is still unit
        float[] prop = m.getParts()[1].getMesh().getVertices().getData();
        assertVertex(prop, 1, 0, 2, 0);
        assertTrue(m.getParts()[0].getMesh().getVertices().isDirty());
    }

    @Test
    void halfwayIsHalfTheRotation() {
        AnimatedModel m = new AnimatedModel(new StubDevice(), GltfLoader.loadScene(armGltf()));
        m.play("Bend", true);
        m.update(0.5f);
        float[] prop = m.getParts()[1].getMesh().getVertices().getData();
        float c = (float) Math.cos(Math.PI / 4);
        assertVertex(prop, 1, c, 1 + c, 0);
        m.update(0.75f); // loops back to 0.25
        assertEquals(0.25f, m.getClipTime(), EPS);
        assertFalse(m.isFinished());
    }

    @Test
    void crossFadeBlendsFromThePreviousPose() {
        AnimatedModel m = new AnimatedModel(new StubDevice(), GltfLoader.loadScene(armGltf()));
        m.play("Bend", false);
        m.update(1f);
        assertFalse(m.crossFade("Missing", 0.2f, true));
        m.setClipTime(1f);
        // fading into the same clip from its start: half way between bent and straight
        assertTrue(m.crossFade("Bend", 1f, false));
        m.update(0f);
        float[] prop = m.getParts()[1].getMesh().getVertices().getData();
        assertVertex(prop, 1, 0, 2, 0);
        m.update(1f);
        assertVertex(prop, 1, 0, 2, 0);
        assertEquals("Bend", m.getClipName());
    }

    @Test
    void hidingANodeHidesItsSubtree() {
        AnimatedModel m = new AnimatedModel(new StubDevice(), GltfLoader.loadScene(armGltf()));
        assertTrue(m.hideNode("arm"));
        assertTrue(m.getParts()[0].isVisible()); // skinned body is placed by "body", not "arm"
        assertFalse(m.getParts()[1].isVisible());
        assertFalse(m.hideNode("nope"));
        m.setVisible(false);
        m.setVisible(true);
        assertFalse(m.getParts()[1].isVisible());
        assertTrue(m.setNodeVisible("arm", true));
        assertTrue(m.getParts()[1].isVisible());
    }

    @Test
    void transformAndTintReachEveryPart() {
        AnimatedModel m = new AnimatedModel(new StubDevice(), GltfLoader.loadScene(armGltf()));
        m.setPosition(1, 2, 3).setRotation(0, 90, 0).setScale(2f);
        for (int i = 0; i < m.getParts().length; i++) {
            assertEquals(3f, m.getParts()[i].getZ(), EPS);
            assertEquals(90f, m.getParts()[i].getRotationY(), EPS);
        }
        m.setTint(0xff808080);
        assertEquals(0xff800000, m.getParts()[0].getMaterial().getColor());
        float[] dst = new float[16];
        assertTrue(m.getNodeMatrix("arm", dst));
        assertEquals(1f, dst[13], EPS);
    }

    @Test
    void particlesLiveAndDie() {
        GameCamera cam = new GameCamera().setPerspective(60f, 0.1f, 100f);
        cam.setPosition(0, 5, 10).setTarget(0, 0, 0);
        ParticleEmitter e = new ParticleEmitter(new StubDevice(), 8).setLifetime(0.5f, 0.5f).setGravity(0f);
        e.burst(0, 0, 0, 5);
        assertEquals(5, e.getLiveCount());
        e.update(0.1f, cam);
        assertTrue(e.isActive());
        float[] d = e.getModel().getMesh().getVertices().getData();
        assertNotEquals(d[0], d[8]); // a live quad has extent
        e.burst(0, 0, 0, 10); // overflows the pool and recycles
        assertEquals(8, e.getLiveCount());
        e.update(1f, cam);
        assertFalse(e.isActive());
        for (int i = 0; i < d.length; i++) {
            assertEquals(0f, d[i], 0f);
        }
    }
}
