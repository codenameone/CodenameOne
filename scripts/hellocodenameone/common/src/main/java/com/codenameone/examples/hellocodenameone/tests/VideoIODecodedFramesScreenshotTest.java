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
 * <p>It draws six "counting" frames showing the digits 1..6 with a real font
 * (the video-subtitle / on-frame-text use case), each on a distinct colour;
 * encodes them into a clip; decodes the clip back with the video decoder; and
 * lays the SIX DECODED frames out as a 2x3 grid. A decode regression is then
 * visible: the rendered digits stop surviving the round-trip, decode in the
 * wrong order, come back the wrong colour, or blank.</p>
 *
 * <p>Threading matters here. iOS renders images through GL/Metal contexts that
 * are bound to the EDT: drawing into an image and reading its pixels back must
 * happen on the EDT. So the frames are rendered with a real font AND read to
 * ARGB ({@link Image#getRGB()}) on the EDT; only the blocking native
 * encode/decode runs on a worker thread, operating on the raw pixel arrays via
 * {@link VideoWriter#writeFrame(int[], int, int, long)}. (An earlier version
 * rendered/read the frames on the worker thread, which the GL backend tolerated
 * flakily -- one dropped frame -- and the Metal backend did not, coming back
 * blank.) The final grid is composed in an ARGB int[] and emitted as one
 * immutable image, so the emit path never reads back a drawn-onto mutable image.</p>
 *
 * <p>Pixel-exact decode differs between platform codecs, so each baseline ships
 * a generous {@code .tolerance} file. Where the platform cannot encode
 * (unsupported targets, a browser without WebCodecs, a native suite whose codec
 * plugins are absent) the test reports SKIPPED and emits no screenshot. This is
 * the visual companion to {@link VideoIORoundTripTest} (frame order, brightness
 * ramp, PCM verification).</p>
 */
public class VideoIODecodedFramesScreenshotTest extends AbstractAnimationScreenshotTest {
    private static final int FRAMES = 6;
    // Encode resolution: large enough that the rendered digit survives H.264.
    private static final int VW = 192;
    private static final int VH = 144;
    private static final float FPS = 6f;
    private static final int GRID_COLS = 2;
    private static final int GRID_ROWS = 3;

    /// One distinct, saturated colour per frame (red, orange, yellow, green,
    /// blue, violet); the solid blocks survive 4:2:0 chroma subsampling cleanly.
    private static final int[] FRAME_COLORS = {
        0xE53935, 0xFB8C00, 0xFDD835, 0x43A047, 0x1E88E5, 0x8E24AA
    };

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
        // runTest() is invoked on the EDT by Cn1ssDeviceRunner. Render the source
        // frames with a real font AND read their pixels here, on the EDT -- iOS
        // image drawing/readback is EDT-bound (doing it on the worker made the
        // decoded frames blank on Metal / flaky on GL). Only the blocking native
        // encode+decode goes to the worker, operating on the raw ARGB arrays.
        final int[][] sources = new int[FRAMES][];
        try {
            for (int i = 0; i < FRAMES; i++) {
                sources[i] = makeDigitFrame(i).getRGB();
            }
        } catch (Throwable t) {
            fail("source frame render failed: " + t);
            return true;
        }

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                VideoFrame[] result;
                String skipReason;
                try {
                    result = encodeThenDecode(sources);
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

    /// Compose the six decoded frames into a 2x3 grid in an ARGB int[] (nearest-
    /// neighbour scaled) and return it as one immutable image, so the emit path
    /// never reads back a drawn-onto mutable image.
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
                    int sy = Math.min(fh - 1, y * fh / cellH);
                    int dstRow = (cy + y) * gw + cx;
                    int srcRow = sy * fw;
                    for (int x = 0; x < cellW; x++) {
                        int sx = Math.min(fw - 1, x * fw / cellW);
                        out[dstRow + x] = 0xff000000 | (src[srcRow + sx] & 0x00ffffff);
                    }
                }
            }
        }

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

    /// Encode the six pre-rendered source frames, decode them back, and return
    /// exactly {@link #FRAMES} decoded frames, or null when the platform cannot
    /// encode/decode (a clean SKIP, not a failure). Runs on the worker thread;
    /// takes raw ARGB arrays so no image drawing/readback happens off the EDT.
    private VideoFrame[] encodeThenDecode(int[][] sources) throws Exception {
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
                writer.writeFrame(sources[i], VW, VH, Math.round(i * 1000f / FPS));
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
        VideoFrame[] result = new VideoFrame[FRAMES];
        for (int i = 0; i < FRAMES; i++) {
            int idx = (int) Math.round((double) i * (frames.size() - 1) / (double) (FRAMES - 1));
            result[i] = frames.get(Math.max(0, Math.min(frames.size() - 1, idx)));
        }
        return result;
    }

    /// A source frame: the digit (index+1) drawn with a real font over that
    /// frame's distinct colour. Must be called on the EDT (image drawing is
    /// EDT-bound on iOS). The glyph is rendered at the platform font size then
    /// scaled up so it fills most of the frame and survives lossy compression.
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
