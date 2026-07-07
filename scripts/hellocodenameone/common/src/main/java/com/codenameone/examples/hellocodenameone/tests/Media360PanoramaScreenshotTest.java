package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
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
        // Uses createForm (like VRStereoScene) rather than a hand-rolled Form:
        // the immersive tests run at the suite tail, so nothing needs the peer
        // detached or portrait restored afterwards, and the plain-Form capture
        // path was what left the iOS Metal simulator reading a stale frame here
        // while the identical createForm path worked for VRStereoScene.
        Form form = createForm("360 Panorama", new BorderLayout(), "Media360Panorama");
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
        form.add(BorderLayout.CENTER, view);
        form.show();
        return true;
    }

    /// Sky gradient over ground, a sun disc and four meridian stripes of
    /// distinct colors, composed directly into an ARGB pixel array. The result
    /// is an immutable, array-backed image (Image.createImage(int[], w, h)):
    /// texturing it only reads the backing array, never a surface. A mutable
    /// image built with a Graphics would instead force the GPU device to read
    /// its pixels back (createTexture calls Image.getRGB), which on the iOS
    /// Metal simulator returns white / drops the first present so the panorama
    /// never appeared. Building the pixels by hand also keeps the texture
    /// identical on every platform (no fillArc anti-aliasing variance).
    private static Image generatePanorama() {
        int w = 1024;
        int h = 512;
        int[] px = new int[w * h];
        // Sky gradient over the top half.
        for (int y = 0; y < h / 2; y++) {
            int t = 255 * y / (h / 2);
            int color = 0xff000000 | (0x30 << 16)
                    | ((0x60 + t / 3) << 8) | Math.min(255, 0xa0 + t / 2);
            int row = y * w;
            for (int x = 0; x < w; x++) {
                px[row + x] = color;
            }
        }
        // Ground gradient over the bottom half.
        for (int y = h / 2; y < h; y++) {
            int t = 255 * (y - h / 2) / (h / 2);
            int color = 0xff000000 | ((0x60 - t / 8) << 16)
                    | ((0x50 - t / 8) << 8) | 0x30;
            int row = y * w;
            for (int x = 0; x < w; x++) {
                px[row + x] = color;
            }
        }
        // Sun disc centered at (w/4, h/4), radius 40, hard-edged.
        int cx = w / 4;
        int cy = h / 4;
        int r2 = 40 * 40;
        for (int y = cy - 40; y <= cy + 40; y++) {
            if (y < 0 || y >= h) {
                continue;
            }
            int dy = y - cy;
            int row = y * w;
            for (int x = cx - 40; x <= cx + 40; x++) {
                if (x < 0 || x >= w) {
                    continue;
                }
                int dx = x - cx;
                if (dx * dx + dy * dy <= r2) {
                    px[row + x] = 0xffffeeaa;
                }
            }
        }
        // Four meridian stripes, 8px wide and h/3 tall from y=h/3.
        int[] stripeColors = {0xffffffff, 0xffdd4433, 0xff33aa55, 0xff3366ff};
        for (int i = 0; i < 4; i++) {
            int sx = (i * w / 4 + w / 2) % w;
            int color = stripeColors[i];
            for (int y = h / 3; y < h / 3 + h / 3; y++) {
                int row = y * w;
                for (int x = sx - 4; x < sx + 4; x++) {
                    if (x < 0 || x >= w) {
                        continue;
                    }
                    px[row + x] = color;
                }
            }
        }
        return Image.createImage(px, w, h);
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

    /// Wait for landscape to settle, warm the panorama texture with an early
    /// render, then force a fresh GPU frame before the capture fires - mirroring
    /// VRStereoScene but with extra time for the 1024x512 texture upload.
    @Override
    protected void registerReadyCallback(final Form parent, final Runnable run) {
        LandscapeCapture.awaitLandscape(parent, new Runnable() {
            public void run() {
                if (view != null) {
                    view.getRenderView().requestRender();
                }
                UITimer.timer(1500, false, parent, new Runnable() {
                    public void run() {
                        if (view != null) {
                            view.getRenderView().requestRender();
                        }
                        UITimer.timer(700, false, parent, run);
                    }
                });
            }
        });
    }
}
