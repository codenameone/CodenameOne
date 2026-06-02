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

/// Behavioral animation test for the portable 3D API. It puts a
/// {@link RenderView} into continuous mode, drives a spinning cube whose model
/// matrix is derived from a frame counter, pumps a handful of explicit render
/// requests, and asserts that multiple frames were actually rendered. This
/// proves the per-platform animation loop (timer driven repaints on the
/// simulator, the native display link / requestAnimationFrame on device) is
/// wired through to the application `Renderer`.
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
        if (!Display.getInstance().isOpenGLSupported()) {
            // No 3D backend on this platform; nothing to animate.
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
        view.setContinuous(true);
        form.add(BorderLayout.CENTER, view);
        form.show();
        UITimer.timer(1500, false, form, new Runnable() {
            public void run() {
                pump(0);
            }
        });
        return true;
    }

    private void pump(final int n) {
        if (n >= 6) {
            if (frames < 2) {
                fail("3D animation loop did not advance frames: " + frames);
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
