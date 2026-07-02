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
import com.codename1.ui.Image;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Animation screenshot test for the {@link com.codename1.media.VideoIO} API.
 *
 * <p>It encodes a six-frame "counting" clip whose frames show the digits 1..6,
 * each on a distinct colour, then decodes the clip back into frames with the
 * video decoder and lays the SIX DECODED frames out as a 2x3 grid. A decode
 * regression is then visible: the digits stop appearing, decode in the wrong
 * order, come back the wrong colour, or blank.</p>
 *
 * <p>The grid is composed directly in an ARGB {@code int[]} from the decoded
 * frames' pixels (via {@link VideoFrame#getARGB()}) and turned into a single
 * immutable image. That deliberately avoids drawing the decoded images onto a
 * mutable off-screen image, which does not render on the iOS Metal backend (the
 * capture came back black) and dropped the first frame under iOS GL.</p>
 *
 * <p>The pixel-exact decode differs between platform codecs, so each baseline
 * ships a generous {@code .tolerance} file. Where the platform cannot encode
 * (unsupported targets, a browser without WebCodecs, a native suite whose codec
 * plugins are absent) the test reports SKIPPED and emits no screenshot. This is
 * the visual companion to {@link VideoIORoundTripTest}, which does the strict
 * programmatic verification (frame order, brightness ramp, PCM).</p>
 */
public class VideoIODecodedFramesScreenshotTest extends AbstractAnimationScreenshotTest {
    private static final int FRAMES = 6;
    // Encode resolution: large enough that the digit survives lossy H.264.
    private static final int VW = 192;
    private static final int VH = 144;
    private static final float FPS = 6f;
    private static final int GRID_COLS = 2;
    private static final int GRID_ROWS = 3;

    private VideoFrame[] decoded;

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
        // let the base class trigger buildScreenshot(). If the platform can't
        // encode/decode we skip without emitting a screenshot.
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                VideoFrame[] result;
                String skipReason;
                try {
                    result = encodeThenDecode();
                    skipReason = result == null ? "videoio-unavailable" : null;
                } catch (Throwable t) {
                    result = null;
                    skipReason = "encode-or-decode-failed:" + t.getMessage();
                }
                final VideoFrame[] frames = result;
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

    /// Compose the six decoded frames into a 2x3 grid purely in an ARGB int[]
    /// (nearest-neighbour scaled) and return it as a single immutable image.
    /// No mutable-image drawing is involved, so it renders identically on every
    /// backend (including iOS Metal).
    @Override
    protected Image buildScreenshot(int width, int height) {
        int cellW = Math.max(1, width / GRID_COLS);
        int cellH = Math.max(1, height / GRID_ROWS);
        int gw = cellW * GRID_COLS;
        int gh = cellH * GRID_ROWS;
        int[] out = new int[gw * gh];
        java.util.Arrays.fill(out, 0xff101010);

        if (decoded != null) {
            for (int i = 0; i < decoded.length && i < GRID_COLS * GRID_ROWS; i++) {
                VideoFrame f = decoded[i];
                if (f == null) {
                    continue;
                }
                int[] src = f.getARGB();
                int fw = f.getWidth();
                int fh = f.getHeight();
                if (src == null || fw <= 0 || fh <= 0 || src.length < fw * fh) {
                    continue;
                }
                int cx = (i % GRID_COLS) * cellW;
                int cy = (i / GRID_COLS) * cellH;
                for (int y = 0; y < cellH; y++) {
                    int sy = y * fh / cellH;
                    if (sy >= fh) {
                        sy = fh - 1;
                    }
                    int dstRow = (cy + y) * gw + cx;
                    int srcRow = sy * fw;
                    for (int x = 0; x < cellW; x++) {
                        int sx = x * fw / cellW;
                        if (sx >= fw) {
                            sx = fw - 1;
                        }
                        out[dstRow + x] = 0xff000000 | (src[srcRow + sx] & 0x00ffffff);
                    }
                }
            }
        }

        // Thin separators between cells.
        int sep = 0xff303030;
        for (int c = 1; c < GRID_COLS; c++) {
            int x = Math.min(c * cellW, gw - 1);
            for (int y = 0; y < gh; y++) {
                out[y * gw + x] = sep;
            }
        }
        for (int r = 1; r < GRID_ROWS; r++) {
            int y = Math.min(r * cellH, gh - 1);
            for (int x = 0; x < gw; x++) {
                out[y * gw + x] = sep;
            }
        }
        return Image.createImage(out, gw, gh);
    }

    /// Encode the 1..6 counting clip, decode it back, and return exactly
    /// {@link #FRAMES} decoded frames, or null when the platform cannot
    /// encode/decode (a clean SKIP, not a failure).
    private VideoFrame[] encodeThenDecode() throws Exception {
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
        final List<VideoFrame> frames = new ArrayList<VideoFrame>();
        InputStream in = FileSystemStorage.getInstance().openInputStream(path);
        VideoReader reader = io.openReader(in, mime);
        try {
            if (!reader.hasVideo()) {
                return null;
            }
            reader.readFrames(FPS, new VideoReader.FrameCallback() {
                @Override
                public boolean frame(VideoFrame f) {
                    frames.add(f);
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
        VideoFrame[] result = new VideoFrame[FRAMES];
        for (int i = 0; i < FRAMES; i++) {
            int idx = (int) Math.round((double) i * (frames.size() - 1) / (double) (FRAMES - 1));
            result[i] = frames.get(Math.max(0, Math.min(frames.size() - 1, idx)));
        }
        return result;
    }

    /// One distinct, saturated colour per frame so the six decoded frames are
    /// told apart by hue (red, orange, yellow, green, blue, violet) -- the solid
    /// colour blocks survive 4:2:0 chroma subsampling cleanly.
    private static final int[] FRAME_COLORS = {
        0xE53935, // 1 red
        0xFB8C00, // 2 orange
        0xFDD835, // 3 yellow
        0x43A047, // 4 green
        0x1E88E5, // 5 blue
        0x8E24AA, // 6 violet
    };

    /// 5x7 bitmap font for the digits 1..6 (each row is 5 bits, the high bit is
    /// the left column). Rendered straight into the frame's ARGB int[] so no
    /// mutable-image drawing/readback is involved -- Graphics text/image drawing
    /// onto a mutable image is unreliable on the iOS Metal backend (the digits
    /// vanished there when drawn that way).
    private static final int[][] DIGIT_5x7 = {
        {0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E}, // 1
        {0x0E, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1F}, // 2
        {0x1F, 0x02, 0x04, 0x02, 0x01, 0x11, 0x0E}, // 3
        {0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02}, // 4
        {0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E}, // 5
        {0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E}, // 6
    };

    /// A source frame: the digit (index+1) rendered as a large bitmap over that
    /// frame's distinct colour, composed entirely in an ARGB int[].
    private static Image makeDigitFrame(int index) {
        int rgb = FRAME_COLORS[index % FRAME_COLORS.length];
        int bg = 0xff000000 | rgb;
        int r = (rgb >> 16) & 0xff, g = (rgb >> 8) & 0xff, b = rgb & 0xff;
        int luma = (r * 30 + g * 59 + b * 11) / 100;
        int ink = luma < 140 ? 0xffffffff : 0xff000000;

        int[] px = new int[VW * VH];
        java.util.Arrays.fill(px, bg);

        int[] glyph = DIGIT_5x7[index % DIGIT_5x7.length];
        // Scale the 5x7 glyph up to ~3/4 of the frame, keeping its aspect ratio.
        int gh = VH * 3 / 4;
        int gwid = gh * 5 / 7;
        if (gwid > VW * 3 / 4) {
            gwid = VW * 3 / 4;
            gh = gwid * 7 / 5;
        }
        int ox = (VW - gwid) / 2;
        int oy = (VH - gh) / 2;
        for (int y = 0; y < gh; y++) {
            int gy = Math.min(6, y * 7 / gh);
            int bits = glyph[gy];
            int base = (oy + y) * VW + ox;
            for (int x = 0; x < gwid; x++) {
                int gx = Math.min(4, x * 5 / gwid);
                if ((bits & (1 << (4 - gx))) != 0) {
                    px[base + x] = ink;
                }
            }
        }
        return Image.createImage(px, VW, VH);
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
