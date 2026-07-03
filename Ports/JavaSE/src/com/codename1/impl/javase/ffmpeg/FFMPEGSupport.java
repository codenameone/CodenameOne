/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase.ffmpeg;

import com.codename1.io.JSONParser;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Shared low level helpers for the ffmpeg backed `com.codename1.media.VideoIO`
/// implementation used by the JavaSE simulator. Wraps process execution, ffprobe based
/// metadata probing and stream utilities so the reader, writer and codec enumeration
/// share a single code path.
final class FFMPEGSupport {
    private FFMPEGSupport() {
    }

    static String ffmpeg() throws IOException {
        File f = FFMPEGMedia.resolveExecutable("ffmpeg");
        if (f == null) {
            throw new IOException("ffmpeg executable not found");
        }
        return f.getAbsolutePath();
    }

    static String ffprobe() throws IOException {
        File f = FFMPEGMedia.resolveExecutable("ffprobe");
        if (f == null) {
            throw new IOException("ffprobe executable not found");
        }
        return f.getAbsolutePath();
    }

    /// Converts a file: URI or plain path into an absolute filesystem path.
    static String normalizeSource(String source) {
        if (source != null && source.startsWith("file:")) {
            try {
                return new File(URI.create(source)).getAbsolutePath();
            } catch (Exception ex) {
                return source.substring("file:".length());
            }
        }
        return source;
    }

    /// Runs a command to completion, returning its combined stdout as a UTF-8 string.
    /// stderr is drained concurrently to avoid the child blocking on a full pipe.
    static String runCapture(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).start();
        final InputStream err = process.getErrorStream();
        Thread drain = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] b = new byte[4096];
                    while (err.read(b) >= 0) {
                        // discard
                    }
                } catch (IOException ignored) {
                } finally {
                    closeQuietly(err);
                }
            }
        }, "cn1-ffmpeg-capture-stderr");
        drain.setDaemon(true);
        drain.start();
        String out;
        try (InputStream in = process.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copyStream(in, bos);
            out = new String(bos.toByteArray(), "UTF-8");
        }
        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return out;
    }

    /// Starts a process and spawns a daemon thread that drains (and discards) its stderr
    /// so the child never blocks writing diagnostics while we stream its stdout. The
    /// caller reads stdout and is responsible for waiting on / destroying the process.
    static Process startWithDrain(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).start();
        final InputStream err = process.getErrorStream();
        Thread drain = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] b = new byte[4096];
                    while (err.read(b) >= 0) {
                        // discard
                    }
                } catch (IOException ignored) {
                } finally {
                    closeQuietly(err);
                }
            }
        }, "cn1-ffmpeg-stderr");
        drain.setDaemon(true);
        drain.start();
        return process;
    }

    /// Like `#startWithDrain(List)` but captures (a bounded prefix of) stderr into the
    /// supplied sink so the caller can report it if the process fails.
    static Process startWithErrorCapture(List<String> command, final StringBuilder errSink) throws IOException {
        Process process = new ProcessBuilder(command).start();
        spawnErrorDrain(process.getErrorStream(), errSink);
        return process;
    }

    private static void spawnErrorDrain(final InputStream err, final StringBuilder errSink) {
        Thread drain = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] b = new byte[4096];
                    int len;
                    while ((len = err.read(b)) >= 0) {
                        synchronized (errSink) {
                            if (errSink.length() < 65536) {
                                errSink.append(new String(b, 0, len, "UTF-8"));
                            }
                        }
                    }
                } catch (IOException ignored) {
                } finally {
                    closeQuietly(err);
                }
            }
        }, "cn1-ffmpeg-stderr-capture");
        drain.setDaemon(true);
        drain.start();
    }

    /// Runs a command that produces no streamed stdout (e.g. muxing to a file), capturing
    /// stderr and throwing an `IOException` containing it when the process exits non-zero.
    static void runChecked(List<String> command) throws IOException {
        StringBuilder errSink = new StringBuilder();
        Process process = new ProcessBuilder(command).start();
        spawnErrorDrain(process.getErrorStream(), errSink);
        closeQuietly(process.getInputStream());
        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            destroyQuietly(process);
            throw new IOException("ffmpeg interrupted", ex);
        }
        if (code != 0) {
            throw new IOException("ffmpeg failed (exit " + code + "): " + errSink.toString().trim());
        }
    }

    /// Runs a command with stdout and stderr merged, returning the combined output. Used
    /// for the {@code -encoders}/{@code -decoders} listings, which ffmpeg prints to
    /// stderr rather than stdout.
    static String runCaptureMerged(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out;
        try (InputStream in = process.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copyStream(in, bos);
            out = new String(bos.toByteArray(), "UTF-8");
        }
        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return out;
    }

    /// Probes a media file with ffprobe, returning its salient metadata.
    static ProbeInfo probe(String source) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add(ffprobe());
        command.add("-v");
        command.add("error");
        command.add("-print_format");
        command.add("json");
        command.add("-show_streams");
        command.add("-show_format");
        command.add(source);
        String json = runCapture(command);
        ProbeInfo data = new ProbeInfo();
        try {
            JSONParser parser = new JSONParser();
            Map parsed = parser.parseJSON(new StringReader(json));
            Map format = (Map) parsed.get("format");
            if (format != null) {
                Object duration = format.get("duration");
                if (duration != null) {
                    data.durationMillis = (long) Math.round(Double.parseDouble(duration.toString()) * 1000d);
                }
            }
            List streams = (List) parsed.get("streams");
            if (streams != null) {
                for (Object entry : streams) {
                    Map stream = (Map) entry;
                    String codecType = String.valueOf(stream.get("codec_type"));
                    if ("video".equals(codecType) && !data.hasVideo) {
                        data.hasVideo = true;
                        data.width = parseInt(stream.get("width"), 0);
                        data.height = parseInt(stream.get("height"), 0);
                        data.frameRate = parseFrameRate(String.valueOf(stream.get("avg_frame_rate")));
                        if (data.frameRate <= 0d) {
                            data.frameRate = parseFrameRate(String.valueOf(stream.get("r_frame_rate")));
                        }
                    } else if ("audio".equals(codecType) && !data.hasAudio) {
                        data.hasAudio = true;
                        data.audioSampleRate = parseInt(stream.get("sample_rate"), 44100);
                        data.audioChannels = parseInt(stream.get("channels"), 2);
                    }
                }
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Failed to parse ffprobe output", ex);
        }
        if (data.hasVideo && data.frameRate <= 0d) {
            data.frameRate = 30d;
        }
        return data;
    }

    static int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            String s = value.toString().trim();
            if (s.indexOf('.') >= 0) {
                return (int) Math.round(Double.parseDouble(s));
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    static double parseFrameRate(String frameRate) {
        if (frameRate == null || frameRate.isEmpty() || "0/0".equals(frameRate)) {
            return 0d;
        }
        if (frameRate.indexOf('/') > 0) {
            String[] parts = frameRate.split("/");
            if (parts.length == 2) {
                try {
                    double numerator = Double.parseDouble(parts[0]);
                    double denominator = Double.parseDouble(parts[1]);
                    if (denominator != 0d) {
                        return numerator / denominator;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        try {
            return Double.parseDouble(frameRate);
        } catch (NumberFormatException ex) {
            return 0d;
        }
    }

    /// Fully reads buffer.length bytes, returning false on premature EOF.
    static boolean readFully(InputStream input, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int len = input.read(buffer, offset, buffer.length - offset);
            if (len < 0) {
                return false;
            }
            offset += len;
        }
        return true;
    }

    /// Converts a tightly packed RGBA byte buffer to a Codename One ARGB int array.
    static int[] rgbaToArgb(byte[] rgba, int pixelCount) {
        int[] argb = new int[pixelCount];
        int o = 0;
        for (int i = 0; i < pixelCount; i++) {
            int r = rgba[o] & 0xff;
            int g = rgba[o + 1] & 0xff;
            int b = rgba[o + 2] & 0xff;
            int a = rgba[o + 3] & 0xff;
            argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
            o += 4;
        }
        return argb;
    }

    /// Plain java.io stream copy. Deliberately avoids `com.codename1.io.Util` so this
    /// helper works without an initialized Codename One runtime (e.g. in unit tests).
    static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
    }

    static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }

    static void destroyQuietly(Process p) {
        if (p != null) {
            try {
                p.destroy();
            } catch (Exception ignored) {
            }
        }
    }

    /// Metadata extracted from an ffprobe run.
    static final class ProbeInfo {
        boolean hasVideo;
        boolean hasAudio;
        int width;
        int height;
        double frameRate;
        long durationMillis = -1;
        int audioSampleRate = -1;
        int audioChannels = -1;
    }
}
