package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.Texture;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

/// End-to-end screenshot test for a textured, unlit cube rendered through the
/// portable 3D API. The texture is generated procedurally (a checkerboard) so
/// the test has no asset dependency, and the cube is drawn at a fixed
/// orientation for a deterministic capture.
public class Gpu3DTexturedCubeScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() {
        // See Gpu3DCubeScreenshotTest: skip on the time-budgeted iOS/HTML5 suites.
        String platform = Display.getInstance().getPlatformName();
        if ("ios".equals(platform) || "HTML5".equals(platform)) {
            System.out.println("CN1SS:INFO:test=Gpu3DTexturedCube status=SKIPPED reason=screenshot-suite-time-budget");
            done();
            return true;
        }
        Form form = createForm("3D Textured", new BorderLayout(), "Gpu3DTexturedCube");
        RenderView view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh cube;
            private Material material;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 1.6f);
                Texture tex = device.createTexture(64, 64, checker());
                tex.setFilter(Texture.Filter.NEAREST);
                material = new Material(Material.Type.UNLIT).setTexture(tex);
                camera.setPerspective(45f, 0.1f, 100f)
                        .setPosition(2.6f, 2.1f, 3.4f)
                        .setTarget(0f, 0f, 0f);
            }

            public void onResize(GraphicsDevice device, int width, int height) {
                camera.setAspect((float) width / Math.max(1, height));
                device.setViewport(0, 0, width, height);
            }

            public void onFrame(GraphicsDevice device) {
                device.clear(0xff101018, true, true);
                device.setCamera(camera);
                float[] model = Matrix4.rotation((float) Math.toRadians(20), 0.2f, 1f, 0f);
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

    private static int[] checker() {
        int[] px = new int[64 * 64];
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                boolean c = ((x / 8) + (y / 8)) % 2 == 0;
                px[y * 64 + x] = c ? 0xffff5533 : 0xff33ff88;
            }
        }
        return px;
    }
}
