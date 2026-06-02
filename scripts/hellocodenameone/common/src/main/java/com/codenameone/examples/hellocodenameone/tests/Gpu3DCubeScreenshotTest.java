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
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

/// End-to-end screenshot test for the portable 3D API (com.codename1.gpu). It
/// hosts a {@link RenderView} in a normal form and renders a Phong-lit cube at a
/// fixed orientation so the capture is deterministic. On platforms without a 3D
/// backend the view shows its placeholder, which still screenshots cleanly.
public class Gpu3DCubeScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() {
        // The iOS and HTML5 suites run the full screenshot set against a tight
        // per-job time budget; the 3D path is exercised on the simulator
        // backend (and the iOS Metal backend renders correctly, verified
        // separately). Skip here to keep those suites within budget.
        String platform = Display.getInstance().getPlatformName();
        if ("ios".equals(platform) || "HTML5".equals(platform)) {
            System.out.println("CN1SS:INFO:test=Gpu3DCube status=SKIPPED reason=screenshot-suite-time-budget");
            done();
            return true;
        }
        Form form = createForm("3D Cube", new BorderLayout(), "Gpu3DCube");
        RenderView view = new RenderView(new Renderer() {
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
}
