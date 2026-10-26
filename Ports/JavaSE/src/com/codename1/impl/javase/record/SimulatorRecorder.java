/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.javase.record;

import com.codename1.impl.javase.ffmpeg.FFMPEGVideoIO;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;

/// Records the simulator screen to a video file.
///
/// Frames are taken on a clock of the recorder's own at the configured rate, not
/// when the application repaints: a still screen still advances the video, so the
/// result plays back in real time. When capture falls behind the clock, the last
/// frame is written again rather than letting the video run fast.
///
/// Audio is assembled when recording stops. The track is the microphone when that
/// is on, silence otherwise, with a short synthesized click mixed in at every tap
/// when tap sounds are on. The application's own audio output is not captured.
///
/// Without ffmpeg the recorder writes a numbered PNG sequence next to the requested
/// file and says so in its result, so a recording is never lost silently.
public final class SimulatorRecorder {

    /// Supplies frames. Called on the recorder thread; the image is read and then
    /// released, so it may be reused between calls.
    public interface FrameSource {
        BufferedImage captureFrame(RecordingOptions.Frame frame);
    }

    /// What a stopped recording produced.
    public static final class Result {
        public final File file;
        public final long durationMs;
        public final int frames;
        public final int width;
        public final int height;
        public final boolean audio;
        public final String note;

        Result(File file, long durationMs, int frames, int width, int height, boolean audio, String note) {
            this.file = file;
            this.durationMs = durationMs;
            this.frames = frames;
            this.width = width;
            this.height = height;
            this.audio = audio;
            this.note = note;
        }
    }

    private static final int SAMPLE_RATE = 44100;

    private static final class Caption {
        final String text;
        final long start;
        final long end;

        Caption(String text, long start, long end) {
            this.text = text;
            this.start = start;
            this.end = end;
        }
    }

    private final FrameSource source;
    private RecordingOptions options;
    private boolean running;
    private Thread thread;
    private long startTime;
    private int frameCount;
    private int width;
    private int height;
    private File output;
    private File videoFile;
    private VideoWriter writer;
    private File pngDir;
    private String note;
    private IOException failure;
    private final List<Long> taps = new ArrayList<Long>();
    private final List<Caption> captions = new ArrayList<Caption>();
    private TargetDataLine mic;
    private ByteArrayOutputStream micData;
    private Thread micThread;
    private BufferedImage scaled;
    private AppAudioTap appAudio;
    /// True from the moment a stop begins until the file is finished; a new
    /// recording cannot start over a writer that is still being closed.
    private boolean stopping;
    private final Object stopLock = new Object();
    private Result lastResult;
    private IOException lastError;

    public SimulatorRecorder(FrameSource source) {
        this.source = source;
    }

    /// The outcome of the most recent finished recording, or null. Set by every
    /// stop, including the automatic one at the time limit or after a failure.
    public synchronized Result getLastResult() {
        return lastResult;
    }

    /// The error that ended the most recent recording, or null.
    public synchronized IOException getLastError() {
        return lastError;
    }

    public synchronized boolean isRecording() {
        return running;
    }

    public synchronized RecordingOptions getOptions() {
        return options;
    }

    /// Milliseconds since the recording started, or -1.
    public synchronized long elapsed() {
        return running ? System.currentTimeMillis() - startTime : -1;
    }

    public synchronized int getFrameCount() {
        return frameCount;
    }

    public synchronized File getOutput() {
        return output;
    }

    /// Starts recording. Throws [IllegalStateException] when already recording.
    public synchronized void start(RecordingOptions opts) throws IOException {
        if (running || stopping) {
            throw new IllegalStateException("Already recording to " + output);
        }
        lastResult = null;
        lastError = null;
        options = opts;
        output = opts.resolveOutput().getAbsoluteFile();
        File parent = output.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        BufferedImage first = source.captureFrame(opts.getFrame());
        if (first == null) {
            throw new IOException("The simulator has no screen to record yet");
        }
        width = even(Math.round(first.getWidth() * opts.getScale()));
        height = even(Math.round(first.getHeight() * opts.getScale()));
        scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        frameCount = 0;
        taps.clear();
        captions.clear();
        note = null;
        failure = null;
        writer = null;
        pngDir = null;
        boolean gif = "gif".equals(opts.getFormat());
        videoFile = gif ? new File(output.getPath() + ".tmp.mp4") : output;
        if (FFMPEGVideoIO.isAvailable()) {
            VideoWriterBuilder b = new VideoWriterBuilder()
                    .path(videoFile.getAbsolutePath())
                    .container(gif ? "mp4" : opts.getFormat())
                    .width(width)
                    .height(height)
                    .frameRate(opts.getFps())
                    .hasAudio(opts.hasAudioTrack());
            if (opts.hasAudioTrack()) {
                b.sampleRate(SAMPLE_RATE).audioChannels(1);
            }
            // Straight to the ffmpeg writer: the builder's build() looks the
            // VideoIO up through Display, which a recorder does not need.
            writer = new FFMPEGVideoIO().createWriter(b);
        } else {
            pngDir = new File(output.getPath() + ".frames");
            if (!pngDir.isDirectory() && !pngDir.mkdirs()) {
                throw new IOException("Cannot create " + pngDir);
            }
            note = "ffmpeg was not found, so frames were written as PNG files to " + pngDir
                    + ". Install ffmpeg (or set -Dffmpeg.dir) to record video.";
        }
        if (opts.getAudio().includesMicrophone() && opts.hasAudioTrack() && writer != null) {
            startMicrophone();
        }
        startTime = System.currentTimeMillis();
        appAudio = null;
        if (opts.getAudio().includesApp() && opts.hasAudioTrack() && writer != null) {
            appAudio = new AppAudioTap(startTime, opts.getMaxSeconds() * 1000L);
            AppAudioTap.setActive(appAudio);
        }
        // The frame already captured is frame 0, written here: a recording
        // stopped before the capture thread runs still has a video stream, and
        // muxing audio onto a video with no frames fails.
        compose(first, 0);
        try {
            writeFrame(0);
        } catch (IOException e) {
            AppAudioTap.setActive(null);
            appAudio = null;
            if (mic != null) {
                mic.stop();
                mic.close();
                mic = null;
            }
            if (writer != null) {
                writer.close();
                writer = null;
            }
            throw e;
        }
        running = true;
        thread = new Thread(new Runnable() {
            public void run() {
                captureLoop();
            }
        }, "SimulatorRecorder");
        thread.setDaemon(true);
        thread.start();
    }

    private static int even(int v) {
        int r = Math.max(2, v);
        return (r & 1) == 0 ? r : r + 1;
    }

    /// Notes a tap for the tap sound, at the current time.
    public synchronized void tap() {
        if (running) {
            taps.add(Long.valueOf(System.currentTimeMillis() - startTime));
        }
    }

    /// Shows a caption over the bottom of the video for this long.
    public synchronized void caption(String text, long durationMs) {
        if (!running) {
            throw new IllegalStateException("Not recording");
        }
        long now = System.currentTimeMillis() - startTime;
        captions.add(new Caption(text, now, now + Math.max(100, durationMs)));
    }

    private void captureLoop() {
        long frameMs = 1000L / options.getFps();
        long maxMs = options.getMaxSeconds() * 1000L;
        try {
            while (isRecording()) {
                long now = System.currentTimeMillis() - startTime;
                if (now >= maxMs) {
                    autoStop();
                    return;
                }
                BufferedImage img = source.captureFrame(options.getFrame());
                if (img != null) {
                    compose(img, now);
                }
                long due = (long) frameCount * 1000L / options.getFps();
                // Write the frame as often as needed to catch the clock up, so a
                // slow capture never makes the video play faster than it happened.
                do {
                    writeFrame(due);
                    due = (long) frameCount * 1000L / options.getFps();
                } while (due + frameMs <= now);
                long sleep = due - (System.currentTimeMillis() - startTime);
                if (sleep > 0) {
                    Thread.sleep(sleep);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            synchronized (this) {
                failure = e;
            }
            // Close ffmpeg and the microphone now; nobody may ever call stop().
            autoStop();
        }
    }

    /// Finishes the recording from a thread of its own (stop() joins the capture
    /// thread, so the capture thread cannot run it). The outcome is kept in
    /// getLastResult()/getLastError().
    private void autoStop() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    stop();
                } catch (IOException e) {
                    // recorded in lastError by stop()
                } catch (IllegalStateException e) {
                    // already stopped by someone else
                }
            }
        }, "SimulatorRecorderAutoStop").start();
    }

    private void compose(BufferedImage img, long now) {
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, width, height, null);
        Caption[] active;
        synchronized (this) {
            active = captions.toArray(new Caption[captions.size()]);
        }
        for (Caption c : active) {
            if (now >= c.start && now < c.end) {
                paintCaption(g, c.text, width, height);
            }
        }
        g.dispose();
    }

    static void paintCaption(Graphics2D g, String text, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, w / 22)));
        FontMetrics fm = g.getFontMetrics();
        int maxW = w * 9 / 10;
        List<String> lines = wrap(text, fm, maxW - fm.getHeight());
        int lineH = fm.getHeight();
        int boxH = lineH * lines.size() + lineH / 2;
        int boxW = 0;
        for (String l : lines) {
            boxW = Math.max(boxW, fm.stringWidth(l));
        }
        boxW += lineH;
        int x = (w - boxW) / 2;
        int y = h - boxH - h / 12;
        g.setColor(new Color(0, 0, 0, 170));
        g.fill(new RoundRectangle2D.Float(x, y, boxW, boxH, lineH, lineH));
        g.setColor(Color.WHITE);
        int ty = y + lineH / 4 + fm.getAscent();
        for (String l : lines) {
            g.drawString(l, (w - fm.stringWidth(l)) / 2, ty);
            ty += lineH;
        }
    }

    private static List<String> wrap(String text, FontMetrics fm, int maxW) {
        List<String> out = new ArrayList<String>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && fm.stringWidth(candidate) > maxW) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    private void writeFrame(long ptsMs) throws IOException {
        if (writer != null) {
            int[] argb = scaled.getRGB(0, 0, width, height, null, 0, width);
            writer.writeFrame(argb, width, height, ptsMs);
        } else {
            ImageIO.write(scaled, "png", new File(pngDir, String.format("frame-%05d.png", Integer.valueOf(frameCount))));
        }
        synchronized (this) {
            frameCount++;
        }
    }

    private void startMicrophone() {
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            mic = AudioSystem.getTargetDataLine(fmt);
            mic.open(fmt);
            mic.start();
            micData = new ByteArrayOutputStream();
            final TargetDataLine line = mic;
            final ByteArrayOutputStream sink = micData;
            micThread = new Thread(new Runnable() {
                public void run() {
                    byte[] buf = new byte[4096];
                    while (line.isOpen()) {
                        int n = line.read(buf, 0, buf.length);
                        if (n <= 0) {
                            break;
                        }
                        synchronized (sink) {
                            sink.write(buf, 0, n);
                        }
                    }
                }
            }, "SimulatorRecorderMic");
            micThread.setDaemon(true);
            micThread.start();
        } catch (Exception e) {
            mic = null;
            micData = null;
            note = "The microphone could not be opened (" + e.getMessage() + "); the audio track is silent.";
        }
    }

    /// Stops recording and finishes the file. Throws when encoding failed at any
    /// point during the recording.
    public Result stop() throws IOException {
        synchronized (stopLock) {
            Thread t;
            synchronized (this) {
                if (!running && thread == null) {
                    throw new IllegalStateException("Not recording");
                }
                running = false;
                stopping = true;
                t = thread;
                thread = null;
            }
            Result r = null;
            IOException err = null;
            try {
                // No timeout: the writer below must never be closed while the
                // capture thread may still be writing a frame into it.
                if (t != null && t != Thread.currentThread()) {
                    boolean interrupted = false;
                    while (t.isAlive()) {
                        try {
                            t.join();
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
                r = finish();
                return r;
            } catch (IOException e) {
                err = e;
                throw e;
            } finally {
                synchronized (this) {
                    stopping = false;
                    lastResult = r;
                    lastError = err;
                }
            }
        }
    }

    private Result finish() throws IOException {
        AppAudioTap app = appAudio;
        if (app != null) {
            AppAudioTap.setActive(null);
            appAudio = null;
        }
        long duration = (long) frameCount * 1000L / options.getFps();
        byte[] micBytes = null;
        if (mic != null) {
            mic.stop();
            mic.close();
            if (micThread != null) {
                try {
                    micThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            synchronized (micData) {
                micBytes = micData.toByteArray();
            }
            mic = null;
        }
        IOException err;
        synchronized (this) {
            err = failure;
        }
        if (writer != null) {
            try {
                if (options.hasAudioTrack() && err == null) {
                    List<Long> tapCopy;
                    synchronized (this) {
                        tapCopy = new ArrayList<Long>(taps);
                    }
                    short[] pcm = buildAudio(duration, micBytes, options.isTapSound() ? tapCopy : new ArrayList<Long>());
                    if (app != null) {
                        app.mixInto(pcm);
                    }
                    writer.writeAudio(pcm, SAMPLE_RATE, 1, 0);
                }
            } finally {
                writer.close();
                writer = null;
            }
        }
        if (err != null) {
            throw err;
        }
        if ("gif".equals(options.getFormat()) && pngDir == null) {
            FFMPEGVideoIO.transcodeToGif(videoFile, output, Math.min(options.getFps(), 20), width);
            if (!videoFile.delete()) {
                videoFile.deleteOnExit();
            }
        }
        File produced = pngDir != null ? pngDir : output;
        return new Result(produced, duration, frameCount, width, height,
                pngDir == null && options.hasAudioTrack(), note);
    }

    /// Builds the mono PCM track: microphone samples (or silence) with a click mixed
    /// in at every tap. Package private for the tests.
    static short[] buildAudio(long durationMs, byte[] micLittleEndian, List<Long> tapsMs) {
        int n = (int) Math.max(1, durationMs * SAMPLE_RATE / 1000L);
        short[] pcm = new short[n];
        if (micLittleEndian != null) {
            int m = Math.min(n, micLittleEndian.length / 2);
            for (int i = 0; i < m; i++) {
                pcm[i] = (short) ((micLittleEndian[i * 2] & 0xff) | (micLittleEndian[i * 2 + 1] << 8));
            }
        }
        short[] click = click();
        for (Long tap : tapsMs) {
            int at = (int) (tap.longValue() * SAMPLE_RATE / 1000L);
            for (int i = 0; i < click.length && at + i < n; i++) {
                if (at + i < 0) {
                    continue;
                }
                int mixed = pcm[at + i] + click[i];
                pcm[at + i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixed));
            }
        }
        return pcm;
    }

    /// A 25 ms tick: a decaying mix of two tones, close to the system keyboard
    /// click on both platforms and short enough not to blur fast typing.
    static short[] click() {
        int len = SAMPLE_RATE * 25 / 1000;
        short[] out = new short[len];
        for (int i = 0; i < len; i++) {
            double t = i / (double) SAMPLE_RATE;
            double env = Math.exp(-t * 260.0);
            double v = Math.sin(2 * Math.PI * 1900 * t) * 0.6 + Math.sin(2 * Math.PI * 3400 * t) * 0.4;
            out[i] = (short) (v * env * 12000);
        }
        return out;
    }
}
