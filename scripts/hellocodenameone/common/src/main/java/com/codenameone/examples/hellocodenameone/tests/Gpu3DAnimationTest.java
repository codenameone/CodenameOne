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

/// Captures a single, deterministic frame of an animated (rotating) 3D cube
/// through the live GPU `RenderView`. The model matrix is pinned to a fixed
/// rotation that is clearly different from the static cube screenshot, so the
/// capture both proves the animation/transform path renders and exercises the
/// platform screenshot's ability to read back a live GPU scene.
public class Gpu3DAnimationTest extends BaseTest {
    private static final float ANGLE = (float) Math.toRadians(140.0);

    @Override
    public boolean runTest() {
        Form form = createForm("3D Animation", new BorderLayout(), "Gpu3DAnimation");
        RenderView view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh cube;
            private Material material;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 1.6f);
                material = new Material(Material.Type.PHONG)
                        .setColor(0xffee5522)
                        .setShininess(18f);
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
                device.draw(cube, material, Matrix4.rotation(ANGLE, 0.35f, 1f, 0.12f));
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
