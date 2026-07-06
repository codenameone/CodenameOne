package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.vr.Media360View;

/// End-to-end screenshot test for the 360 panorama viewer
/// (com.codename1.vr.Media360View). A synthetic equirectangular panorama -
/// sky and ground bands, a sun disc and meridian stripes - is generated in
/// code so no asset is needed, then shown in side-by-side stereo at a fixed
/// look angle with head tracking off, making the capture deterministic. The
/// view is captured in landscape, where a panorama reads naturally. The
/// vertical color transitions also verify the sphere's u direction: the
/// stripes must appear in their generated order, not mirrored. On platforms
/// without a 3D backend the screenshot is skipped; unlike the stereo VR test
/// this one still runs on tvOS, where panning a 360 image is a sensible use.
public class Media360PanoramaScreenshotTest extends BaseTest {
    private Media360View view;

    @Override
    public boolean runTest() {
        view = new Media360View();
        if (!view.isSupported()) {
            done();
            return true;
        }
        view.setImage(generatePanorama());
        view.setStereo(true);
        // Sensors differ per platform (the CI Android emulator reports a
        // live rotation vector); freeze head tracking so the look angle is
        // exactly the yaw/pitch set below.
        view.setHeadTrackingEnabled(false);
        // A fixed off-axis look angle exercises the sphere mapping more than
        // the straight-ahead default.
        view.setYaw(30f);
        view.setPitch(10f);
        LandscapeCapture.lock();
        // This is the last of the landscape VR / 360 tests, so once its
        // capture is done it detaches the 3D peer (so no lingering Metal layer
        // is grabbed by a later test's screenshot under the iOS late-present
        // race) and restores portrait - leaving the device as the rest of the
        // suite (which runs in portrait) expects. A plain Form is used instead
        // of createForm() so the capture completion can run that teardown
        // before done().
        final Form form = new Form("360 Panorama", new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                LandscapeCapture.awaitLandscape(this, () -> UITimer.timer(1000, false, this, () -> {
                    view.getRenderView().requestRender();
                    UITimer.timer(500, false, this, () -> captureWhenSettled(this, "Media360Panorama", () -> {
                        removeAll();
                        revalidate();
                        LandscapeCapture.restorePortrait(this,
                                Media360PanoramaScreenshotTest.this::done);
                    }));
                }));
            }
        };
        form.add(BorderLayout.CENTER, view);
        form.show();
        return true;
    }

    /// Sky gradient over ground, a sun disc and four meridian stripes of
    /// distinct colors. Pure fills and lines - no text - so the pixels only
    /// depend on the drawing primitives, not platform font rendering.
    private static Image generatePanorama() {
        int w = 1024;
        int h = 512;
        Image img = Image.createImage(w, h, 0xff000000);
        Graphics g = img.getGraphics();
        for (int y = 0; y < h / 2; y++) {
            int t = 255 * y / (h / 2);
            g.setColor((0x30 << 16) | ((0x60 + t / 3) << 8) | Math.min(255, 0xa0 + t / 2));
            g.drawLine(0, y, w, y);
        }
        for (int y = h / 2; y < h; y++) {
            int t = 255 * (y - h / 2) / (h / 2);
            g.setColor(((0x60 - t / 8) << 16) | ((0x50 - t / 8) << 8) | 0x30);
            g.drawLine(0, y, w, y);
        }
        g.setColor(0xffeeaa);
        g.fillArc(w / 4 - 40, h / 4 - 40, 80, 80, 0, 360);
        int[] stripeColors = {0xffffff, 0xdd4433, 0x33aa55, 0x3366ff};
        for (int i = 0; i < 4; i++) {
            int x = (i * w / 4 + w / 2) % w;
            g.setColor(stripeColors[i]);
            g.fillRect(x - 4, h / 3, 8, h / 3);
        }
        return img;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return com.codename1.ui.CN.isGpuSupported();
    }
}
