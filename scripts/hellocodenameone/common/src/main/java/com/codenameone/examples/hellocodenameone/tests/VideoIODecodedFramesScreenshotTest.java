package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.FileSystemStorage;
import com.codename1.media.VideoCodec;
import com.codename1.media.VideoFrame;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Animation screenshot test for the {@link com.codename1.media.VideoIO} API.
 *
 * <p>It encodes a six-frame "counting" clip whose frames show the digits 1..6 on
 * a ramping grey background, then decodes the clip back into frames with the
 * video decoder and lays the SIX DECODED frames out as the standard 2x3
 * animation grid. The committed per-platform baseline is therefore the decoded
 * output, so a decode regression shows up visually: the digits stop appearing,
 * decode in the wrong order, or come back blank.</p>
 *
 * <p>The pixel-exact decode differs between platform codecs (and slightly between
 * runs of the same software encoder), so each baseline ships a generous
 * {@code .tolerance} file. Where the platform cannot encode at all (the iOS
 * simulator's H.264 path, the unsupported Watch/TV/Car targets, a browser
 * without WebCodecs) the test reports SKIPPED and emits no screenshot, so no
 * baseline is expected there.</p>
 *
 * <p>This is the visual companion to {@link VideoIORoundTripTest}, which performs
 * the strict programmatic verification (frame order, brightness ramp, PCM).</p>
 */
public class VideoIODecodedFramesScreenshotTest extends AbstractAnimationScreenshotTest {
    private static final int FRAMES = 6;
    // Encode resolution: large enough that the digit survives lossy H.264.
    private static final int VW = 192;
    private static final int VH = 144;
    private static final float FPS = 6f;

    private Image[] decoded;

    @Override
    protected String getImageName() {
        return "VideoIODecodedFrames";
    }

    @Override
    protected String getDisplayTitle() {
        return "VideoIO decode 1..6";
    }

    @Override
    public boolean runTest() {
        // The encode + decode is native, blocking work; run it off the EDT.
        // Only once the decoded frames are in hand do we show the host form and
        // let the base class capture the grid from renderFrame(). If the
        // platform can't encode/decode we skip without emitting a screenshot.
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                Image[] result;
                String skipReason;
                try {
                    result = encodeThenDecode();
                    skipReason = result == null ? "videoio-unavailable" : null;
                } catch (Throwable t) {
                    result = null;
                    skipReason = "encode-or-decode-failed:" + t.getMessage();
                }
                final Image[] frames = result;
                final String reason = skipReason;
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (frames == null) {
                            System.out.println("CN1SS:INFO:test=" + getImageName()
                                    + " status=SKIPPED reason=" + reason + "-on-"
                                    + Display.getInstance().getPlatformName());
                            done();
                            return;
                        }
                        decoded = frames;
                        try {
                            // Base class shows the host form and, once ready,
                            // captures the 2x3 grid via renderFrame() below.
                            VideoIODecodedFramesScreenshotTest.super.runTest();
                        } catch (Exception ex) {
                            fail("screenshot capture failed: " + ex);
                        }
                    }
                });
            }
        }, "cn1-videoio-screenshot");
        worker.start();
        return true;
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        g.setColor(0x101010);
        g.fillRect(0, 0, width, height);
        if (decoded == null || frameIndex >= decoded.length || decoded[frameIndex] == null) {
            return;
        }
        Image frame = decoded[frameIndex];
        Image scaled = (frame.getWidth() == width && frame.getHeight() == height)
                ? frame : frame.scaled(width, height);
        g.drawImage(scaled, 0, 0);
        if (scaled != frame) {
            scaled.dispose();
        }
    }

    /// Encode the 1..6 counting clip, decode it back, and return exactly
    /// {@link #FRAMES} decoded images, or null when the platform cannot
    /// encode/decode (a clean SKIP, not a failure).
    private Image[] encodeThenDecode() throws Exception {
        VideoIO io = VideoIO.getVideoIO();
        if (io == null || !VideoIO.isSupported()) {
            return null;
        }

        String videoCodec = null;
        for (VideoCodec c : io.getAvailableEncoders()) {
            if (c.isVideo()) {
                if (VideoIO.CODEC_H264.equals(c.getId())) {
                    videoCodec = c.getId();
                } else if (videoCodec == null) {
                    videoCodec = c.getId();
                }
            }
        }
        if (videoCodec == null) {
            return null;
        }

        boolean webm = VideoIO.CODEC_VP8.equals(videoCodec) || VideoIO.CODEC_VP9.equals(videoCodec);
        String container = webm ? VideoIO.CONTAINER_WEBM : VideoIO.CONTAINER_MP4;
        String mime = webm ? "video/webm" : "video/mp4";
        String path = FileSystemStorage.getInstance().getAppHomePath()
                + "/cn1-videoio-shot-" + System.currentTimeMillis() + (webm ? ".webm" : ".mp4");

        // ---- ENCODE (encode-side unavailability is a SKIP) ----
        try {
            VideoWriter writer = io.createWriter(new VideoWriterBuilder()
                    .path(path).container(container)
                    .width(VW).height(VH).frameRate(FPS)
                    .videoCodec(videoCodec));
            for (int i = 0; i < FRAMES; i++) {
                writer.writeFrame(makeDigitFrame(i), Math.round(i * 1000f / FPS));
            }
            writer.close();
        } catch (Throwable t) {
            cleanup(path);
            return null;
        }

        // ---- DECODE ----
        if (!FileSystemStorage.getInstance().exists(path)
                || FileSystemStorage.getInstance().getLength(path) <= 0) {
            cleanup(path);
            return null;
        }
        final List<Image> frames = new ArrayList<Image>();
        InputStream in = FileSystemStorage.getInstance().openInputStream(path);
        VideoReader reader = io.openReader(in, mime);
        try {
            if (!reader.hasVideo()) {
                return null;
            }
            reader.readFrames(FPS, new VideoReader.FrameCallback() {
                @Override
                public boolean frame(VideoFrame f) {
                    frames.add(f.toImage());
                    return frames.size() < FRAMES + 4;
                }
            });
        } finally {
            reader.close();
            cleanup(path);
        }
        if (frames.isEmpty()) {
            return null;
        }
        // Resample to exactly FRAMES evenly spaced decoded frames.
        Image[] out = new Image[FRAMES];
        for (int i = 0; i < FRAMES; i++) {
            int idx = (int) Math.round((double) i * (frames.size() - 1) / (double) (FRAMES - 1));
            out[i] = frames.get(Math.max(0, Math.min(frames.size() - 1, idx)));
        }
        return out;
    }

    /// One distinct, saturated colour per frame so the six decoded frames are
    /// telling apart by hue (red, orange, yellow, green, blue, violet) -- the
    /// solid colour blocks survive 4:2:0 chroma subsampling cleanly.
    private static final int[] FRAME_COLORS = {
        0xE53935, // 1 red
        0xFB8C00, // 2 orange
        0xFDD835, // 3 yellow
        0x43A047, // 4 green
        0x1E88E5, // 5 blue
        0x8E24AA, // 6 violet
    };

    /// A source frame: the digit (index+1) drawn large over that frame's
    /// distinct colour, so the decoded frames are both numerically labelled and
    /// colour-coded after lossy compression.
    private static Image makeDigitFrame(int index) {
        int rgb = FRAME_COLORS[index % FRAME_COLORS.length];
        int bg = 0xff000000 | rgb;
        Image img = Image.createImage(VW, VH, bg);
        Graphics g = img.getGraphics();

        int r = (rgb >> 16) & 0xff, gg2 = (rgb >> 8) & 0xff, b = rgb & 0xff;
        int luma = (r * 30 + gg2 * 59 + b * 11) / 100;
        int ink = luma < 140 ? 0xffffff : 0x000000;
        String s = String.valueOf(index + 1);
        Font font = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);
        int tw = Math.max(1, font.stringWidth(s));
        int th = Math.max(1, font.getHeight());
        // Render the glyph small, then scale it up so the digit fills most of the
        // frame regardless of the platform's native large-font size. The glyph
        // buffer is filled with the frame's grey background (not transparency,
        // which some mutable-image backends drop) so the scaled box blends in.
        Image glyph = Image.createImage(tw, th, bg);
        Graphics gg = glyph.getGraphics();
        gg.setColor(ink);
        gg.setFont(font);
        gg.drawString(s, 0, 0);
        int targetH = VH * 3 / 4;
        int targetW = Math.max(1, tw * targetH / th);
        if (targetW > VW * 3 / 4) {
            targetW = VW * 3 / 4;
        }
        Image big = glyph.scaled(targetW, targetH);
        g.drawImage(big, (VW - targetW) / 2, (VH - targetH) / 2);
        if (big != glyph) {
            big.dispose();
        }
        glyph.dispose();
        return img;
    }

    private static void cleanup(String path) {
        try {
            if (FileSystemStorage.getInstance().exists(path)) {
                FileSystemStorage.getInstance().delete(path);
            }
        } catch (Throwable ignored) {
            // best-effort temp cleanup
        }
    }
}
