package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shader.ShaderNode;
import com.codename1.gpu.shader.ShaderType;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Custom full screen passes (`PostProcess.addPass`) chained in front of a
/// built-in vignette. The first pass ripples the frame sideways along its
/// height; the second inverts the right half and paints a red band across
/// the top tenth. A backend that hands a pass the frame upside down puts the
/// band at the bottom, one that drops the first pass shows straight cube
/// edges, and one that drops the built-in chain after the passes loses the
/// dark corners.
public class GpuPostPassScreenshotTest extends BaseTest {
    private RenderView view;

    private static float[] mul(float[] a, float[] b) {
        float[] out = new float[16];
        Matrix4.multiply(a, b, out);
        return out;
    }

    static PostProcess passes() {
        ShaderNode uv = ShaderNode.screenUv();
        ShaderNode wobble = ShaderNode.sin(uv.y().mul(60f)).mul(ShaderNode.uniform("amount", ShaderType.FLOAT));
        ShaderMaterial ripple = new ShaderMaterial()
                .declareUniform("amount", ShaderType.FLOAT)
                .setUniform("amount", 0.012f)
                .set(ShaderMaterial.Hook.BASE_COLOR,
                        ShaderNode.sceneColor(uv.add(ShaderNode.vec(wobble, ShaderNode.f(0f)))));
        ShaderNode c = ShaderNode.sceneColor(uv).rgb();
        ShaderNode right = ShaderNode.step(ShaderNode.f(0.5f), uv.x());
        ShaderNode inverted = c.add(ShaderNode.f(1f).sub(c.mul(2f)).mul(right));
        ShaderNode top = ShaderNode.f(1f).sub(ShaderNode.step(ShaderNode.f(0.1f), uv.y()));
        ShaderNode banded = ShaderNode.mix(inverted, ShaderNode.vec3(0.9f, 0.1f, 0.1f), top);
        ShaderMaterial split = new ShaderMaterial()
                .set(ShaderMaterial.Hook.BASE_COLOR, ShaderNode.vec(banded, ShaderNode.f(1f)));
        return new PostProcess().setVignette(0.6f).addPass(ripple).addPass(split);
    }

    @Override
    public boolean runTest() {
        Form form = createForm("Custom post passes", new BorderLayout(), "GpuPostPass");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private final PostProcess post = passes();
            private Mesh cube;
            private final Material[] blocks = new Material[3];
            private int width = 1;
            private int height = 1;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 1f);
                int[] colors = {0xffd9643a, 0xff3a8fd9, 0xff58b06a};
                for (int i = 0; i < blocks.length; i++) {
                    blocks[i] = new Material(Material.Type.PHONG).setColor(colors[i]);
                }
                device.setLight(new Light().setDirection(-0.5f, -1f, -0.35f));
                camera.setPerspective(45f, 0.3f, 40f).setPosition(0f, 1.2f, 5.5f).setTarget(0f, 0f, 0f);
            }

            public void onResize(GraphicsDevice device, int w, int h) {
                width = Math.max(1, w);
                height = Math.max(1, h);
                camera.setAspect((float) width / height);
                device.setViewport(0, 0, w, h);
            }

            public void onFrame(GraphicsDevice device) {
                device.setCamera(camera);
                boolean postOn = device.beginPostProcess(post, width, height);
                device.clear(0xffd8d2c4, true, true);
                for (int i = 0; i < blocks.length; i++) {
                    device.draw(cube, blocks[i], mul(Matrix4.translation(-1.6f + 1.6f * i, 0f, 0f),
                            Matrix4.rotation((float) Math.toRadians(25 + 20 * i), 0.3f, 1f, 0f)));
                }
                if (postOn) {
                    device.endPostProcess(1f);
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
