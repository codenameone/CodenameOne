package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// End-to-end screenshot test for the portable 3D API (com.codename1.gpu). It
/// hosts a {@link RenderView} in a normal form and renders a Phong-lit cube at a
/// fixed orientation so the capture is deterministic. On platforms without a 3D
/// backend the view shows its placeholder, which still screenshots cleanly.
public class Gpu3DCubeScreenshotTest extends BaseTest {
    private RenderView view;

    @Override
    public boolean runTest() {
        Form form = createForm("3D Cube", new BorderLayout(), "Gpu3DCube");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh cube;
            private Material material;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 1.6f);
                material = new Material(Material.Type.PHONG)
                        .setColor(0xff3366ff)
                        .setShininess(24f);
                camera.setPerspective(45f, 0.1f, 100f)
                        .setPosition(2.6f, 2.1f, 3.4f)
                        .setTarget(0f, 0f, 0f);
                device.setLight(new Light().setDirection(-0.4f, -1f, -0.55f));
            }

            public void onResize(GraphicsDevice device, int width, int height) {
                camera.setAspect((float) width / Math.max(1, height));
                device.setViewport(0, 0, width, height);
            }

            public void onFrame(GraphicsDevice device) {
                device.clear(0xff101018, true, true);
                device.setCamera(camera);
                float[] model = Matrix4.rotation((float) Math.toRadians(25), 0.35f, 1f, 0.12f);
                device.draw(cube, material, model);
            }

            public void onDispose(GraphicsDevice device) {
            }
        });
        if (view.isSupported()) {
            form.add(BorderLayout.CENTER, view);
        } else {
            form.add(BorderLayout.CENTER, new Label("3D unsupported"));
        }
        form.show();
        return true;
    }

    /// Force a fresh GPU frame to be rendered (and read back for capture) before
    /// the screenshot fires, so a cold GL surface that has not drawn yet cannot
    /// produce a blank capture.
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
