package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.EnvironmentMaps;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.shaders.ScenePresets;
import com.codename1.gpu.shaders.Surface;
import com.codename1.gpu.shaders.Terrain;
import com.codename1.gpu.shaders.Water;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// A harbour at dusk built only from the shader library: the dusk preset's
/// sky dome and light, Gerstner waves, a rocky breakwater, a stack of
/// instanced painted crates, soft shadows, and the post chain with god
/// rays and FXAA. The shader clock is pinned so every capture is the same
/// frame of the waves.
public class GpuShaderHarbourScreenshotTest extends BaseTest {
    private RenderView view;

    private static float[] mul(float[] a, float[] b) {
        float[] out = new float[16];
        Matrix4.multiply(a, b, out);
        return out;
    }

    @Override
    public boolean runTest() {
        Form form = createForm("Shader harbour", new BorderLayout(), "GpuShaderHarbour");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private final ScenePresets.Preset look = ScenePresets.harbourDusk();
            private final PostProcess post = look.getPost().setGodRays(0.5f).setFxaa(true);
            private Mesh dome;
            private Mesh sea;
            private Mesh rock;
            private Mesh crate;
            private Material skyMat;
            private Material water;
            private Material stone;
            private Material paint;
            private final float[] crates = new float[6 * 16];
            private final int[] tints = {0xffc8442e, 0xff2e6fc8, 0xffd9a62a, 0xff3a8a52, 0xffc8442e, 0xff2e6fc8};
            private int width = 1;
            private int height = 1;

            public void onInit(GraphicsDevice device) {
                dome = Primitives.sphere(device, 200f, 16, 24, true);
                sea = Primitives.grid(140f, 140);
                rock = Primitives.sphere(device, 1f, 16, 24, false);
                crate = Primitives.cube(device, 1f);
                skyMat = new Material(Material.Type.UNLIT).setShader(look.getSky()).setFogged(false);
                skyMat.setRenderState(new RenderState().setDepthWrite(false).setCullMode(RenderState.CullMode.NONE));
                skyMat.setCastShadows(false);
                water = new Material(Material.Type.PBR).setShader(Water.ocean());
                water.setCastShadows(false);
                stone = new Material(Material.Type.PBR).setShader(Terrain.rock());
                // one painted metal material for every crate; each copy takes its own colour
                paint = new Material(Material.Type.PBR).setShader(Surface.paintedMetal());
                for (int i = 0; i < 6; i++) {
                    float x = -1.6f + (i % 3) * 1.08f;
                    float y = 1.5f + (i / 3) * 1.02f;
                    float[] m = mul(Matrix4.translation(x, y, 0.4f),
                            Matrix4.rotation((float) Math.toRadians(i * 7 - 10), 0f, 1f, 0f));
                    System.arraycopy(m, 0, crates, i * 16, 16);
                }
                camera.setPerspective(50f, 0.1f, 400f).setPosition(6.5f, 3.4f, 9f).setTarget(-1f, 1.2f, -3f);
                look.apply(device);
                // crisp shadows on the quay, a coarse far cascade over the water
                look.getLight().setShadowCenter(-1f, 0f, 0f).setShadowCascades(2, 60f);
                // the dusk sky, pre-filtered, lights the metal and the water's reflections
                device.getEnvironment().setEnvironmentMap(
                        EnvironmentMaps.fromSky(device, device.getEnvironment(), look.getLight()));
            }

            public void onResize(GraphicsDevice device, int w, int h) {
                width = Math.max(1, w);
                height = Math.max(1, h);
                camera.setAspect((float) width / height);
                device.setViewport(0, 0, w, h);
            }

            private void drawScene(GraphicsDevice device, boolean casters) {
                if (!casters) {
                    device.draw(dome, skyMat, Matrix4.translation(6.5f, 3.4f, 9f));
                    device.draw(sea, water, null);
                }
                device.draw(rock, stone, mul(Matrix4.translation(-1.2f, 0.2f, 0f),
                        Matrix4.scaling(4.5f, 1.4f, 2.6f)));
                device.drawInstanced(crate, paint, crates, tints, 6);
            }

            public void onFrame(GraphicsDevice device) {
                device.setTime(2.5f);
                device.setCamera(camera);
                if (device.beginShadowPass(look.getLight())) {
                    drawScene(device, true);
                    device.endShadowPass();
                }
                boolean postOn = device.beginPostProcess(post, width, height);
                device.clear(look.getClearColor(), true, true);
                drawScene(device, false);
                if (postOn) {
                    device.endPostProcess(2.5f);
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
