package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.Camera;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.vr.VREye;
import com.codename1.vr.VRRenderer;
import com.codename1.vr.VRView;

/// End-to-end screenshot test for the stereoscopic VR API (com.codename1.vr).
/// A {@link VRView} renders a fixed scene - three Phong-lit cubes over a grey
/// floor - in side-by-side stereo. Head tracking is disabled so the capture is
/// deterministic on every platform regardless of sensor availability; the
/// stereo parallax between the two eye views is the feature under test. The
/// view is captured in landscape, where a stereo scene reads naturally. On
/// platforms without a 3D backend the view shows its placeholder and the
/// screenshot is skipped, and on tvOS side-by-side stereo has no use (no
/// headset), so the test is skipped there.
public class VRStereoSceneScreenshotTest extends BaseTest {
    private VRView view;

    @Override
    public boolean runTest() {
        if (com.codename1.ui.CN.isTV()) {
            // Stereo VR targets a headset/cardboard viewer; on a TV it is
            // redundant, so skip rather than baseline a nonsensical capture.
            System.out.println("CN1SS:INFO:test=VRStereoScene status=SKIPPED reason=tv-no-stereo-vr");
            done();
            return true;
        }
        Form form = createForm("VR Stereo", new BorderLayout(), "VRStereoScene");
        view = new VRView(new VRRenderer() {
            private Mesh cube;
            private Mesh floor;
            private Material blue;
            private Material red;
            private Material green;
            private Material grey;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 0.5f);
                floor = Primitives.quad(device, 8f);
                blue = new Material(Material.Type.PHONG).setColor(0xff3366ff);
                red = new Material(Material.Type.PHONG).setColor(0xffdd4433);
                green = new Material(Material.Type.PHONG).setColor(0xff33aa55);
                grey = new Material(Material.Type.PHONG).setColor(0xff555560);
                device.setLight(new Light().setDirection(-0.4f, -1f, -0.55f));
            }

            public void onEyeFrame(GraphicsDevice device, VREye eye, Camera camera) {
                // Floor 1.4m below the viewer, laid flat.
                float[] floorM = new float[16];
                Matrix4.multiply(Matrix4.translation(0f, -1.4f, -2f),
                        Matrix4.rotation((float) (-Math.PI / 2), 1f, 0f, 0f), floorM);
                device.draw(floor, grey, floorM);
                // A fixed-orientation cube ahead and two offset cubes for
                // depth cues; near geometry shows the largest eye parallax.
                float[] center = new float[16];
                Matrix4.multiply(Matrix4.translation(0f, 0f, -2f),
                        Matrix4.rotation((float) Math.toRadians(30), 0.4f, 1f, 0.2f), center);
                device.draw(cube, blue, center);
                device.draw(cube, red, Matrix4.translation(-1.2f, -0.3f, -2.6f));
                device.draw(cube, green, Matrix4.translation(1.2f, 0.3f, -3.2f));
            }

            public void onDispose(GraphicsDevice device) {
            }
        });
        // Sensors differ per platform (and are absent on CI); freeze the head
        // orientation so both eye cameras depend only on the VRSettings.
        view.setHeadTrackingEnabled(false);
        if (!view.isSupported()) {
            // No GPU 3D backend on this platform; skip the screenshot rather
            // than gate a placeholder that has no per-platform baseline.
            done();
            return true;
        }
        LandscapeCapture.lock();
        form.add(BorderLayout.CENTER, view);
        form.show();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return com.codename1.ui.CN.isGpuSupported();
    }

    /// Force a fresh, fully-presented frame before the capture. On the iOS
    /// Metal backend a screenshot can otherwise read a previous form's still
    /// current drawable (the late-present race); DesktopMode uses the same
    /// mitigation. A no-op cost on the other backends.
    @Override
    protected long extraSettleBeforeCaptureMillis() {
        return 700;
    }

    /// This test captures in landscape ON PURPOSE (via LandscapeCapture);
    /// opt out of the baseline-orientation guard so it doesn't fight the
    /// deliberate rotation.
    @Override
    protected boolean allowNonBaselineOrientationCapture() {
        return true;
    }

    /// Wait for landscape to settle, then force a fresh GPU frame to be
    /// rendered (and read back for capture) before the screenshot fires,
    /// mirroring the Gpu3D screenshot tests.
    @Override
    protected void registerReadyCallback(final Form parent, final Runnable run) {
        LandscapeCapture.awaitLandscape(parent, new Runnable() {
            public void run() {
                UITimer.timer(1000, false, parent, new Runnable() {
                    public void run() {
                        if (view != null) {
                            view.requestRender();
                        }
                        UITimer.timer(500, false, parent, run);
                    }
                });
            }
        });
    }
}
