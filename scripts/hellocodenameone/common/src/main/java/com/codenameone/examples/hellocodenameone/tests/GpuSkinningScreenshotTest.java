package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// GPU skinning: three tubes in `VertexFormat.SKINNED`, each bound to a
/// three joint chain, bent by joint matrices handed to the material with
/// `Material.setSkinning`. Every vertex is moved in the vertex stage; the
/// buffers hold the straight bind pose. The tubes bend left, not at all and
/// right by different amounts, so a backend that drops the joints draws three
/// straight columns, and one that reads the weights or the joint rows wrong
/// tears the tubes apart at the joints. The shadows go through the skinned
/// depth program, so they bend with the tubes.
public class GpuSkinningScreenshotTest extends BaseTest {
    private static final int SIDES = 12;
    private static final int RINGS = 16;
    private static final float HEIGHT = 2.4f;
    private static final float RADIUS = 0.22f;
    private static final int JOINTS = 3;

    private RenderView view;

    private static float[] mul(float[] a, float[] b) {
        float[] out = new float[16];
        Matrix4.multiply(a, b, out);
        return out;
    }

    // A tube along +Y in the bind pose. A vertex blends the two joints
    // nearest its height, so the bend is smooth across each joint.
    private static Mesh tube(GraphicsDevice device) {
        int count = (RINGS + 1) * SIDES;
        VertexBuffer vb = new VertexBuffer(VertexFormat.SKINNED, count);
        float[] d = vb.getData();
        float segment = HEIGHT / JOINTS;
        for (int r = 0; r <= RINGS; r++) {
            float y = HEIGHT * r / RINGS;
            float s = Math.max(0f, Math.min(JOINTS - 1f, y / segment - 0.5f));
            int j0 = Math.min(JOINTS - 2, (int) s);
            float f = s - j0;
            for (int i = 0; i < SIDES; i++) {
                double a = 2 * Math.PI * i / SIDES;
                float nx = (float) Math.cos(a);
                float nz = (float) Math.sin(a);
                int o = (r * SIDES + i) * 16;
                d[o] = nx * RADIUS;
                d[o + 1] = y;
                d[o + 2] = nz * RADIUS;
                d[o + 3] = nx;
                d[o + 4] = 0f;
                d[o + 5] = nz;
                d[o + 6] = (float) i / SIDES;
                d[o + 7] = (float) r / RINGS;
                d[o + 8] = j0;
                d[o + 9] = j0 + 1;
                d[o + 12] = 1f - f;
                d[o + 13] = f;
            }
        }
        int[] idx = new int[RINGS * SIDES * 6];
        int k = 0;
        for (int r = 0; r < RINGS; r++) {
            for (int i = 0; i < SIDES; i++) {
                int a = r * SIDES + i;
                int b = r * SIDES + (i + 1) % SIDES;
                int c = a + SIDES;
                int e = b + SIDES;
                idx[k++] = a;
                idx[k++] = c;
                idx[k++] = b;
                idx[k++] = b;
                idx[k++] = c;
                idx[k++] = e;
            }
        }
        IndexBuffer ib = new IndexBuffer(idx.length);
        ib.setData(idx);
        return new Mesh(vb, ib, PrimitiveType.TRIANGLES);
    }

    // Joint j sits at the base of its segment and turns by `angle` about Z
    // relative to its parent: a chain bending in one plane. The rows are the
    // first three rows of each joint's matrix, as setSkinning takes them.
    private static float[] bend(float angle) {
        float[] rows = new float[JOINTS * 12];
        float[] m = Matrix4.identity();
        float segment = HEIGHT / JOINTS;
        for (int j = 0; j < JOINTS; j++) {
            if (j > 0) {
                float pivot = j * segment;
                m = mul(m, mul(Matrix4.translation(0f, pivot, 0f),
                        mul(Matrix4.rotation(angle, 0f, 0f, 1f), Matrix4.translation(0f, -pivot, 0f))));
            }
            for (int row = 0; row < 3; row++) {
                for (int c = 0; c < 4; c++) {
                    rows[j * 12 + row * 4 + c] = m[c * 4 + row];
                }
            }
        }
        return rows;
    }

    @Override
    public boolean runTest() {
        Form form = createForm("GPU skinning", new BorderLayout(), "GpuSkinning");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh tube;
            private Mesh floor;
            private Material ground;
            private final Material[] tubes = new Material[3];
            private boolean gpu;

            public void onInit(GraphicsDevice device) {
                tube = tube(device);
                floor = Primitives.quad(device, 1f);
                ground = new Material(Material.Type.LAMBERT).setColor(0xff59626e);
                gpu = device.isGpuSkinning();
                int[] colors = {0xffe0703a, 0xff4fb07a, 0xff4a7fd8};
                float[] angles = {0.55f, 0f, -0.4f};
                for (int i = 0; i < 3; i++) {
                    RenderState rs = RenderState.opaque();
                    rs.setCullMode(RenderState.CullMode.NONE);
                    tubes[i] = new Material(Material.Type.PBR).setColor(colors[i]).setRoughness(0.45f);
                    tubes[i].setRenderState(rs);
                    if (gpu) {
                        tubes[i].setSkinning(bend(angles[i]), JOINTS);
                    }
                }
                device.setLight(new Light().setDirection(-0.5f, -1f, -0.4f).setShadows(true));
                camera.setPerspective(45f, 0.1f, 50f).setPosition(0f, 2.2f, 6.2f).setTarget(0f, 1.1f, 0f);
            }

            public void onResize(GraphicsDevice device, int w, int h) {
                camera.setAspect((float) Math.max(1, w) / Math.max(1, h));
                device.setViewport(0, 0, w, h);
            }

            private void drawScene(GraphicsDevice device) {
                for (int i = 0; i < 3; i++) {
                    device.draw(tube, tubes[i], Matrix4.translation(-1.6f + 1.6f * i, 0f, 0f));
                }
            }

            public void onFrame(GraphicsDevice device) {
                device.setCamera(camera);
                if (device.beginShadowPass(device.getLight())) {
                    drawScene(device);
                    device.endShadowPass();
                }
                device.clear(0xff151a22, true, true);
                device.draw(floor, ground, mul(Matrix4.rotation((float) Math.toRadians(-90), 1f, 0f, 0f),
                        Matrix4.scaling(14f, 14f, 1f)));
                drawScene(device);
            }

            public void onDispose(GraphicsDevice device) {
            }
        });
        if (!view.isSupported()) {
            done();
            return true;
        }
        form.add(BorderLayout.CENTER, view);
        form.show();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return com.codename1.ui.CN.isGpuSupported();
    }

    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        UITimer.timer(1200, false, parent, new Runnable() {
            public void run() {
                if (view != null) {
                    view.requestRender();
                }
                UITimer.timer(600, false, parent, run);
            }
        });
    }
}
