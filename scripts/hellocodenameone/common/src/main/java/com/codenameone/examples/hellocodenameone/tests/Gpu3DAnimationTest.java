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
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.util.SuccessCallback;

/// Animation test for the portable 3D API. Like the other hellocodenameone
/// animation tests it captures the various stages of an animation into a single
/// deterministic grid rather than one timing-dependent frame: it spins a cube
/// through six fixed rotation angles, capturing the live GPU `RenderView` at each
/// angle (cropped out of a real device screenshot) and composing the six frames
/// into a 2x3 grid. Each angle is pinned, so the capture is reproducible.
public class Gpu3DAnimationTest extends BaseTest {
    private static final int FRAMES = 6;

    private RenderView view;
    private Form form;
    private volatile float angle;
    private final Image[] frames = new Image[FRAMES];

    @Override
    public boolean shouldTakeScreenshot() {
        // We emit a composed grid image ourselves rather than a single capture.
        return false;
    }

    @Override
    public boolean runTest() {
        if (!Display.getInstance().isOpenGLSupported()) {
            done();
            return true;
        }
        form = new Form("3D Animation", new BorderLayout());
        view = new RenderView(new Renderer() {
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
                device.draw(cube, material, Matrix4.rotation(angle, 0.35f, 1f, 0.12f));
            }

            public void onDispose(GraphicsDevice device) {
            }
        });
        if (!view.isSupported()) {
            form.add(BorderLayout.CENTER, new Label("3D unsupported"));
            form.show();
            done();
            return true;
        }
        form.add(BorderLayout.CENTER, view);
        form.show();
        // Let the peer come up, then capture each pinned rotation stage.
        UITimer.timer(1200, false, form, new Runnable() {
            public void run() {
                captureStage(0);
            }
        });
        return true;
    }

    private void captureStage(final int i) {
        if (i >= FRAMES) {
            emitGrid();
            return;
        }
        angle = (float) Math.toRadians(i * (360.0 / FRAMES));
        view.requestRender();
        UITimer.timer(300, false, form, new Runnable() {
            public void run() {
                Display.getInstance().screenshot(new SuccessCallback<Image>() {
                    public void onSucess(Image full) {
                        try {
                            if (full != null) {
                                int x = Math.max(0, view.getAbsoluteX());
                                int y = Math.max(0, view.getAbsoluteY());
                                int w = Math.min(view.getWidth(), full.getWidth() - x);
                                int h = Math.min(view.getHeight(), full.getHeight() - y);
                                if (w > 0 && h > 0) {
                                    frames[i] = full.subImage(x, y, w, h, true);
                                }
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        captureStage(i + 1);
                    }
                });
            }
        });
    }

    private void emitGrid() {
        int fw = 0;
        int fh = 0;
        for (int i = 0; i < FRAMES; i++) {
            if (frames[i] != null) {
                fw = frames[i].getWidth();
                fh = frames[i].getHeight();
                break;
            }
        }
        if (fw <= 0 || fh <= 0) {
            fw = Math.max(1, view.getWidth());
            fh = Math.max(1, view.getHeight());
        }
        int cellW = 200;
        int cellH = Math.max(1, cellW * fh / Math.max(1, fw));
        int cols = 2;
        int rows = (FRAMES + cols - 1) / cols;
        Image grid = Image.createImage(cols * cellW, rows * cellH, 0xff101010);
        Graphics g = grid.getGraphics();
        for (int i = 0; i < FRAMES; i++) {
            if (frames[i] == null) {
                continue;
            }
            int col = i % cols;
            int row = i / cols;
            Image scaled = frames[i].scaled(cellW, cellH);
            g.drawImage(scaled, col * cellW, row * cellH);
        }
        Cn1ssDeviceRunnerHelper.emitImage(grid, "Gpu3DAnimation", new Runnable() {
            public void run() {
                done();
            }
        });
    }
}
