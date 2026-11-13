package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shaders.Fx;
import com.codename1.gpu.shaders.Liquid;
import com.codename1.gpu.shaders.ScenePresets;
import com.codename1.gpu.shaders.Surface;
import com.codename1.gpu.shaders.Toon;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Twelve library shaders side by side on spheres under the noon preset:
/// the surfaces in the top rows, the stylized and animated effects below.
/// The shader clock is pinned so the animated ones hold still.
public class GpuShaderGalleryScreenshotTest extends BaseTest {
    private RenderView view;

    @Override
    public boolean runTest() {
        Form form = createForm("Shader gallery", new BorderLayout(), "GpuShaderGallery");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private final ScenePresets.Preset look = ScenePresets.clearNoon();
            private Mesh ball;
            private Material[] mats;

            public void onInit(GraphicsDevice device) {
                ball = Primitives.sphere(device, 0.45f, 24, 32, false);
                ShaderMaterial[] shaders = {
                    Surface.paintedMetal(), Surface.rust(), Surface.wood(), Surface.concrete(),
                    Surface.glass(), Surface.ice(), Surface.fabric(), Surface.carPaint(),
                    Toon.cel(), Fx.hologram(), Liquid.lava(), Fx.dissolve()
                };
                mats = new Material[shaders.length];
                for (int i = 0; i < shaders.length; i++) {
                    Material.Type type = i == 8 ? Material.Type.UNLIT : Material.Type.PBR;
                    mats[i] = new Material(type).setShader(shaders[i]);
                }
                camera.setPerspective(40f, 0.1f, 100f).setPosition(0f, 0f, 6.2f).setTarget(0f, 0f, 0f);
                look.apply(device);
            }

            public void onResize(GraphicsDevice device, int w, int h) {
                camera.setAspect((float) Math.max(1, w) / Math.max(1, h));
                device.setViewport(0, 0, w, h);
            }

            public void onFrame(GraphicsDevice device) {
                device.setTime(1.5f);
                device.setCamera(camera);
                device.clear(look.getClearColor(), true, true);
                for (int i = 0; i < mats.length; i++) {
                    float x = (i % 4 - 1.5f) * 1.1f;
                    float y = (1 - i / 4) * 1.1f;
                    device.draw(ball, mats[i], Matrix4.translation(x, y, 0f));
                }
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
