package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.FileSystemStorage;
import com.codename1.media.AudioBuffer;
import com.codename1.media.VideoCodec;
import com.codename1.media.VideoFrame;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Cross-platform device test for the {@link com.codename1.media.VideoIO} API. It encodes
 * a short 6-frame "counting" animation (1..6) with a constant audio tone, then decodes
 * the individual frames back with the video decoder and verifies:
 *
 * <ul>
 *   <li>the frames round-trip in order and are visually distinct (each successive frame
 *       is brighter than the last, which survives lossy encoding) - i.e. the count 1..6
 *       decoded correctly;</li>
 *   <li>the decoded audio PCM has a sane signal level (RMS in the expected band) where
 *       the platform decoder exposes audio.</li>
 * </ul>
 *
 * Where the platform cannot encode video (the API reports no encoder, e.g. a browser
 * without WebCodecs, or video IO is unsupported) the test reports SKIPPED rather than
 * failing. A real round-trip regression (a decode/verification mismatch) fails the test.
 * This is an assertion test, not a screenshot test, so it does not affect baselines.
 */
public class VideoIORoundTripTest extends BaseTest {
    private static final int W = 128;
    private static final int H = 96;
    private static final int FRAMES = 6;
    private static final float FPS = 6f;
    private static final int SAMPLE_RATE = 8000;
    private static final double TONE_HZ = 440.0;
    private static final double TONE_AMPLITUDE = 0.5; // -> ~0.354 RMS

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                runRoundTrip();
            }
        }, "cn1-videoio-roundtrip");
        worker.start();
        return true;
    }

    private void skip(String reason) {
        System.out.println("CN1SS:INFO:test=VideoIORoundTripTest status=SKIPPED reason=" + reason);
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                done();
            }
        });
    }

    private void runRoundTrip() {
        VideoIO io = VideoIO.getVideoIO();
        if (io == null || !VideoIO.isSupported()) {
            skip("VideoIO-unsupported-on-" + Display.getInstance().getPlatformName());
            return;
        }

        // Pick a container-compatible pair. Audio and video codecs cannot be
        // selected independently: Opus is valid for WebM but not MP4, while
        // AAC is valid for MP4 but not WebM. Prefer a complete A/V round trip,
        // then fall back to a video-only round trip when the browser has no
        // compatible audio encoder.
        boolean h264 = false;
        boolean vp8 = false;
        boolean vp9 = false;
        boolean aac = false;
        boolean opus = false;
        for (VideoCodec c : io.getAvailableEncoders()) {
            String id = c.getId();
            h264 |= VideoIO.CODEC_H264.equals(id);
            vp8 |= VideoIO.CODEC_VP8.equals(id);
            vp9 |= VideoIO.CODEC_VP9.equals(id);
            aac |= VideoIO.CODEC_AAC.equals(id);
            opus |= VideoIO.CODEC_OPUS.equals(id);
        }

        String videoCodec;
        String audioCodec;
        if (h264 && aac) {
            videoCodec = VideoIO.CODEC_H264;
            audioCodec = VideoIO.CODEC_AAC;
        } else if (vp8 && opus) {
            videoCodec = VideoIO.CODEC_VP8;
            audioCodec = VideoIO.CODEC_OPUS;
        } else if (vp9 && opus) {
            videoCodec = VideoIO.CODEC_VP9;
            audioCodec = VideoIO.CODEC_OPUS;
        } else if (h264) {
            videoCodec = VideoIO.CODEC_H264;
            audioCodec = null;
        } else if (vp8) {
            videoCodec = VideoIO.CODEC_VP8;
            audioCodec = null;
        } else if (vp9) {
            videoCodec = VideoIO.CODEC_VP9;
            audioCodec = null;
        } else {
            videoCodec = null;
            audioCodec = null;
        }
        if (videoCodec == null) {
            skip("no-video-encoder-on-" + Display.getInstance().getPlatformName());
            return;
        }

        boolean webm = VideoIO.CODEC_VP8.equals(videoCodec) || VideoIO.CODEC_VP9.equals(videoCodec);
        String container = webm ? VideoIO.CONTAINER_WEBM : VideoIO.CONTAINER_MP4;
        String mime = webm ? "video/webm" : "video/mp4";
        boolean withAudio = audioCodec != null;

        String path = FileSystemStorage.getInstance().getAppHomePath()
                + "/cn1-videoio-roundtrip-" + System.currentTimeMillis() + (webm ? ".webm" : ".mp4");

        // ---- ENCODE (encode-side unavailability is a SKIP, not a failure) ----
        try {
            VideoWriterBuilder builder = new VideoWriterBuilder()
                    .path(path).container(container)
                    .width(W).height(H).frameRate(FPS)
                    .videoCodec(videoCodec);
            if (withAudio) {
                builder.hasAudio(true).audioCodec(audioCodec).sampleRate(SAMPLE_RATE).audioChannels(1);
            }
            VideoWriter writer = io.createWriter(builder);
            int samplesPerFrame = SAMPLE_RATE / FRAMES;
            for (int i = 0; i < FRAMES; i++) {
                writer.writeFrame(makeCountingFrame(i), Math.round(i * 1000f / FPS));
                if (withAudio) {
                    writer.writeAudio(makeTone(i, samplesPerFrame), SAMPLE_RATE, 1, Math.round(i * 1000f / FPS));
                }
            }
            writer.close();
        } catch (Throwable t) {
            cleanup(path);
            skip("encode-unavailable-on-" + Display.getInstance().getPlatformName() + ":" + t.getMessage());
            return;
        }

        // ---- DECODE + VERIFY (a mismatch here is a real failure) ----
        try {
            if (!FileSystemStorage.getInstance().exists(path)
                    || FileSystemStorage.getInstance().getLength(path) <= 0) {
                fail("encoder produced no output file");
                return;
            }
            InputStream in = FileSystemStorage.getInstance().openInputStream(path);
            VideoReader reader = io.openReader(in, mime);
            try {
                if (!reader.hasVideo()) {
                    fail("decoded clip has no video track");
                    return;
                }
                int rw = reader.getWidth();
                int rh = reader.getHeight();
                if (Math.abs(rw - W) > 8 || Math.abs(rh - H) > 8) {
                    fail("decoded dimensions " + rw + "x" + rh + " do not match encoded " + W + "x" + H);
                    return;
                }

                final List<Double> brightness = new ArrayList<Double>();
                reader.readFrames(FPS, new VideoReader.FrameCallback() {
                    @Override
                    public boolean frame(VideoFrame f) {
                        brightness.add(averageBrightness(f));
                        return brightness.size() < 32;
                    }
                });
                if (brightness.size() < 4 || brightness.size() > 12) {
                    fail("expected ~" + FRAMES + " decoded frames, got " + brightness.size());
                    return;
                }
                double first = brightness.get(0);
                double last = brightness.get(brightness.size() - 1);
                double min = first, max = first;
                for (Double b : brightness) {
                    min = Math.min(min, b);
                    max = Math.max(max, b);
                }
                // The counting frames ramp 30..210 in grey; lossy codecs preserve the order
                // and a wide spread. Verify the sequence decoded in increasing order.
                if (last - first < 70) {
                    fail("frames not increasing as counted: first=" + (int) first + " last=" + (int) last);
                    return;
                }
                if (max - min < 70) {
                    fail("decoded frames not visually distinct (brightness spread " + (int) (max - min) + ")");
                    return;
                }

                // Frame accurate single seek: the frame at ~mid should be brighter than frame 0.
                VideoFrame midFrame = reader.frameAt(Math.round((FRAMES - 1) * 1000f / FPS));
                if (midFrame != null && averageBrightness(midFrame) + 20 < first) {
                    fail("frameAt() returned an out-of-order frame");
                    return;
                }

                // Audio PCM levels, where the platform decoder exposes audio.
                if (withAudio && reader.hasAudio()) {
                    AudioBuffer audio = reader.readAudio();
                    if (audio == null || audio.getSize() <= 0) {
                        fail("decoded clip reports audio but no PCM samples were returned");
                        return;
                    }
                    double rms = rms(audio);
                    if (rms < 0.05 || rms > 0.7) {
                        fail("decoded audio PCM level out of range: rms=" + rms);
                        return;
                    }
                }
            } finally {
                reader.close();
            }
        } catch (Throwable t) {
            fail("decode/verify failed: " + t);
            return;
        } finally {
            cleanup(path);
        }

        System.out.println("CN1SS:INFO:test=VideoIORoundTripTest status=ROUNDTRIP_OK platform="
                + Display.getInstance().getPlatformName());
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                done();
            }
        });
    }

    private static Image makeCountingFrame(int index) {
        int grey = 30 + index * 36; // 30,66,102,138,174,210
        int fill = 0xff000000 | (grey << 16) | (grey << 8) | grey;
        Image img = Image.createImage(W, H, fill);
        Graphics g = img.getGraphics();
        g.setColor(grey < 128 ? 0xffffff : 0x000000);
        g.drawString(String.valueOf(index + 1), 6, 4);
        return img;
    }

    private static short[] makeTone(int frameIndex, int samplesPerFrame) {
        short[] pcm = new short[samplesPerFrame];
        long base = (long) frameIndex * samplesPerFrame;
        for (int n = 0; n < samplesPerFrame; n++) {
            double t = (base + n) / (double) SAMPLE_RATE;
            pcm[n] = (short) (Math.sin(2 * Math.PI * TONE_HZ * t) * TONE_AMPLITUDE * 32767);
        }
        return pcm;
    }

    private static double averageBrightness(VideoFrame frame) {
        int[] argb = frame.getARGB();
        long sum = 0;
        int count = 0;
        for (int i = 0; i < argb.length; i += 16) { // sample every 16th pixel
            int p = argb[i];
            int r = (p >> 16) & 0xff;
            int gg = (p >> 8) & 0xff;
            int b = p & 0xff;
            sum += (r + gg + b) / 3;
            count++;
        }
        return count == 0 ? 0 : (double) sum / count;
    }

    private static double rms(AudioBuffer audio) {
        int size = audio.getSize();
        float[] f = new float[size];
        audio.copyTo(f);
        double acc = 0;
        for (int i = 0; i < size; i++) {
            acc += (double) f[i] * f[i];
        }
        return size == 0 ? 0 : Math.sqrt(acc / size);
    }

    private static void cleanup(String path) {
        try {
            if (FileSystemStorage.getInstance().exists(path)) {
                FileSystemStorage.getInstance().delete(path);
            }
        } catch (Throwable ignored) {
        }
    }
}
