package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.Texture;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

import java.io.InputStream;

/// End-to-end screenshot test for loading a real authored model through the
/// portable 3D mesh-loading API (`GltfLoader`). Instead of a built in primitive
/// it renders the Khronos "BoomBox" glTF sample (a CC0 model, ~6K triangles with
/// its own base-color texture) loaded from a bundled binary glTF (`.glb`) asset
/// and lit with a Phong material. The model ships as a project resource and is
/// read with `getResourceAsStream`, so the same asset loads on every platform.
/// The BoomBox is authored at roughly 2 cm across, so it is scaled up and drawn
/// at a fixed orientation for a stable capture.
public class Gpu3DModelScreenshotTest extends BaseTest {
    private RenderView view;

    @Override
    public boolean runTest() {
        Form form = createForm("3D Model", new BorderLayout(), "Gpu3DModel");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh model;
            private Material material;

            public void onInit(GraphicsDevice device) {
                GltfLoader.GltfModel loaded = loadBoomBox(device);
                model = loaded.getMesh();
                material = new Material(Material.Type.PHONG).setShininess(16f);
                Texture tex = loaded.getBaseColorTexture();
                if (tex != null) {
                    material.setTexture(tex);
                }
                camera.setPerspective(45f, 0.1f, 100f)
                        .setPosition(1.9f, 1.5f, 2.6f)
                        .setTarget(0f, 0f, 0f);
                device.setLight(new Light().setDirection(-0.4f, -0.7f, -0.6f));
            }

            public void onResize(GraphicsDevice device, int width, int height) {
                camera.setAspect((float) width / Math.max(1, height));
                device.setViewport(0, 0, width, height);
            }

            public void onFrame(GraphicsDevice device) {
                device.clear(0xff101018, true, true);
                device.setCamera(camera);
                // The model is ~0.02 units across; scale it up, then rotate.
                float[] scale = Matrix4.scaling(70f, 70f, 70f);
                float[] rot = Matrix4.rotation((float) Math.toRadians(35), 0f, 1f, 0f);
                float[] m = Matrix4.identity();
                Matrix4.multiply(rot, scale, m);
                device.draw(model, material, m);
            }

            public void onDispose(GraphicsDevice device) {
            }
        });
        if (!view.isSupported()) {
            // No GPU 3D backend on this platform (e.g. the iOS GL build); skip the
            // screenshot (shouldTakeScreenshot() returns false) rather than gate a
            // "3D unsupported" placeholder that has no per-platform baseline.
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
    public void cleanup() {
        // Detach the RenderView so its GPU peer (canvas/native view) is torn
        // down before the next test runs, preventing a stale 3D frame from
        // bleeding into a later test's screenshot.
        if (view != null) {
            view.remove();
            view = null;
        }
        super.cleanup();
    }

    private GltfLoader.GltfModel loadBoomBox(GraphicsDevice device) {
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/boombox.glb");
        if (in == null) {
            in = getClass().getResourceAsStream("/boombox.glb");
        }
        if (in == null) {
            throw new RuntimeException("boombox.glb resource not found");
        }
        try {
            return GltfLoader.loadModel(device, in);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load boombox.glb: " + ex, ex);
        }
    }

    /// Force a fresh GPU frame before the screenshot so a cold GL surface cannot
    /// produce a blank capture. See Gpu3DCubeScreenshotTest.
    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        UITimer.timer(1000, false, parent, new Runnable() {
            public void run() {
                if (view != null) {
                    view.requestRender();
                }
                UITimer.timer(500, false, parent, run);
            }
        });
    }
}
