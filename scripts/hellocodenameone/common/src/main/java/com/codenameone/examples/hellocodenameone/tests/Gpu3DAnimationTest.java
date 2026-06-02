package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Behavioral animation test for the portable 3D API. It hosts a
/// {@link RenderView} and drives a spinning cube whose model matrix is derived
/// from a frame counter, pumping a series of explicit on-demand render requests
/// and asserting that multiple frames were actually rendered to the
/// application `Renderer`. This proves the per-platform render path delivers
/// frames on demand.
///
/// On-demand rendering (rather than a free-running continuous loop) is used so
/// the test cannot wedge the screenshot suite on any platform; continuous mode
/// is a thin wrapper over the same per-frame path and is exercised by real apps.
public class Gpu3DAnimationTest extends BaseTest {
    private volatile int frames;
    private RenderView view;
    private Form form;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        // Skip on the time-budgeted iOS/HTML5 full-suite jobs (see
        // Gpu3DCubeScreenshotTest) and where there is no 3D backend.
        String platform = Display.getInstance().getPlatformName();
        if ("ios".equals(platform) || "HTML5".equals(platform)
                || !Display.getInstance().isOpenGLSupported()) {
            done();
            return true;
        }
        form = new Form("3D Animation", new BorderLayout());
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh cube;
            private Material material;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 1.5f);
                material = new Material(Material.Type.LAMBERT).setColor(0xff44cc66);
                camera.setPerspective(45f, 0.1f, 100f)
                        .setPosition(2f, 2f, 3f)
                        .setTarget(0f, 0f, 0f);
            }

            public void onResize(GraphicsDevice device, int width, int height) {
                camera.setAspect((float) width / Math.max(1, height));
                device.setViewport(0, 0, width, height);
            }

            public void onFrame(GraphicsDevice device) {
                frames++;
                device.clear(0xff000000, true, true);
                device.setCamera(camera);
                device.draw(cube, material, Matrix4.rotation(frames * 0.1f, 0f, 1f, 0f));
            }

            public void onDispose(GraphicsDevice device) {
            }
        });
        form.add(BorderLayout.CENTER, view);
        form.show();
        UITimer.timer(1200, false, form, new Runnable() {
            public void run() {
                pump(0);
            }
        });
        return true;
    }

    private void pump(final int n) {
        if (n >= 8) {
            if (frames < 2) {
                fail("3D animation did not advance frames on demand: " + frames);
                return;
            }
            done();
            return;
        }
        view.requestRender();
        UITimer.timer(120, false, form, new Runnable() {
            public void run() {
                pump(n + 1);
            }
        });
    }
}
