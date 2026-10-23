/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.packager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Thin wrapper around the ffmpeg and ffprobe command line tools.
final class Ffmpeg {
    /// What ffprobe says about a source.
    record Probe(double duration, int width, int height) {
    }

    private Ffmpeg() {
    }

    /// Directory for temporary files: the `reel.tmp` property, else `java.io.tmpdir`.
    static Path tempRoot() throws IOException {
        Path p = Path.of(System.getProperty("reel.tmp", System.getProperty("java.io.tmpdir")));
        Files.createDirectories(p);
        return p;
    }

    static boolean available() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    static boolean hasFilter(String name) {
        try {
            byte[] out = capture(List.of("-hide_banner", "-filters"));
            for (String line : new String(out, StandardCharsets.UTF_8).split("\n")) {
                String[] w = line.trim().split("\\s+");
                if (w.length > 1 && w[1].equals(name)) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /// Runs ffmpeg and returns stdout.
    static byte[] capture(List<String> args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-nostdin");
        if (!args.contains("-hide_banner")) {
            cmd.add("-hide_banner");
            cmd.add("-loglevel");
            cmd.add("error");
        }
        cmd.addAll(args);
        return exec(cmd);
    }

    static void run(List<String> args) throws IOException {
        List<String> a = new ArrayList<>();
        a.add("-y");
        a.addAll(args);
        capture(a);
    }

    private static byte[] exec(List<String> cmd) throws IOException {
        Path err = Files.createTempFile(tempRoot(), "ff", ".log");
        try {
            Process p = new ProcessBuilder(cmd).redirectError(err.toFile()).start();
            byte[] out = p.getInputStream().readAllBytes();
            int rc;
            try {
                rc = p.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
            if (rc != 0) {
                String[] lines = Files.readString(err, StandardCharsets.UTF_8).split("\n");
                StringBuilder sb = new StringBuilder("ffmpeg failed (" + rc + "): ");
                for (int i = Math.max(0, lines.length - 6); i < lines.length; i++) {
                    sb.append(lines[i]).append(' ');
                }
                throw new IOException(sb.toString().trim());
            }
            return out;
        } finally {
            Files.deleteIfExists(err);
        }
    }

    static Probe probe(Path f) throws IOException {
        List<String> cmd = List.of("ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries",
                "stream=width,height:format=duration", "-of", "default=noprint_wrappers=1", f.toString());
        String out = new String(exec(cmd), StandardCharsets.UTF_8);
        double d = 0;
        int w = 0;
        int h = 0;
        for (String line : out.split("\n")) {
            line = line.trim();
            try {
                if (line.startsWith("width=")) {
                    w = Integer.parseInt(line.substring(6));
                } else if (line.startsWith("height=")) {
                    h = Integer.parseInt(line.substring(7));
                } else if (line.startsWith("duration=")) {
                    d = Double.parseDouble(line.substring(9));
                }
            } catch (NumberFormatException ex) {
                // N/A values are ignored
            }
        }
        if (w <= 0 || h <= 0 || d <= 0) {
            throw new IOException("Could not probe " + f + ": " + out.replace('\n', ' '));
        }
        return new Probe(d, w, h);
    }
}
