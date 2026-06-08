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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Animation test for the portable 3D API. Like the other hellocodenameone
/// animation tests it captures the stages of an animation into a single
/// deterministic grid rather than one timing-dependent frame. Here the six
/// fixed rotation stages are drawn together in a single GPU frame: the cube is
/// rendered six times, once into each cell of a 2x3 grid of viewports, each at a
/// pinned rotation angle. Because the whole grid is one real GPU frame it is
/// captured by the standard screenshot path (no per-frame device screenshots),
/// so it is reproducible and portable across every backend.
///
/// Sub-viewport rectangles use the GL convention (origin bottom-left); each
/// platform compares against its own baseline, so a backend whose native
/// viewport origin differs only flips the vertical cell order.
public class Gpu3DAnimationTest extends BaseTest {
    private static final int FRAMES = 6;
    private static final int COLS = 2;
    private static final int ROWS = 3;

    private RenderView view;

    @Override
    public boolean runTest() {
        Form form = createForm("3D Animation", new BorderLayout(), "Gpu3DAnimation");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh cube;
            private Material material;
            private int surfaceW;
            private int surfaceH;

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
                surfaceW = width;
                surfaceH = height;
            }

            public void onFrame(GraphicsDevice device) {
                device.clear(0xff101018, true, true);
                device.setCamera(camera);
                int cellW = Math.max(1, surfaceW / COLS);
                int cellH = Math.max(1, surfaceH / ROWS);
                camera.setAspect((float) cellW / cellH);
                for (int i = 0; i < FRAMES; i++) {
                    int col = i % COLS;
                    int row = i / COLS;
                    int vx = col * cellW;
                    // GL viewport origin is bottom-left; place row 0 at the top.
                    int vy = surfaceH - (row + 1) * cellH;
                    device.setViewport(vx, vy, cellW, cellH);
                    float angle = (float) Math.toRadians(i * (360.0 / FRAMES));
                    device.draw(cube, material, Matrix4.rotation(angle, 0.35f, 1f, 0.12f));
                }
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

    /// Force a fresh GPU frame to be rendered before the screenshot fires, so a
    /// cold GL surface that has not drawn yet cannot produce a blank capture.
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
