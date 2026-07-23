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
package com.codename1.videobuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Performs one bulk ffmpeg encode from a completed image sequence and mixed PCM. */
final class StagedVideoEncoder {
    private StagedVideoEncoder() { }

    static Encoder encoder(Path ffmpeg) throws IOException {
        String forced = System.getProperty("video.encoder", "").trim();
        String encoders = capture(List.of(ffmpeg.toString(), "-hide_banner", "-encoders"));
        if (!forced.isEmpty()) {
            if (!encoders.contains(forced)) throw new IOException("ffmpeg encoder is unavailable: " + forced);
            return new Encoder(forced, "h264_videotoolbox".equals(forced));
        }
        boolean appleSilicon = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")
                && (System.getProperty("os.arch", "").equals("aarch64")
                    || System.getProperty("os.arch", "").equals("arm64"));
        if (appleSilicon && !"false".equals(System.getProperty("video.hardwareEncoding"))
                && encoders.contains("h264_videotoolbox")) {
            return new Encoder("h264_videotoolbox", true);
        }
        if (encoders.contains("libx264")) return new Encoder("libx264", false);
        if (encoders.contains(" h264 ")) return new Encoder("h264", false);
        throw new IOException("No H.264 encoder is available in " + ffmpeg);
    }

    static Path findFfmpeg() throws IOException {
        String directory = System.getProperty("ffmpeg.dir", "").trim();
        if (!directory.isEmpty()) {
            Path executable = Path.of(directory).resolve(isWindows() ? "ffmpeg.exe" : "ffmpeg");
            if (Files.isExecutable(executable)) return executable;
        }
        String executable = NarrationService.findExecutable(isWindows() ? "ffmpeg.exe" : "ffmpeg");
        if (!executable.isEmpty()) return Path.of(executable);
        throw new IOException("ffmpeg executable not found; install ffmpeg or configure -Dffmpeg.dir");
    }

    static void linkFrames(Path directory, FramePlan.Plan plan) throws IOException {
        List<FramePlan.Frame> rendered = plan.frames();
        for (int item = 0; item < rendered.size(); item++) {
            FramePlan.Frame frame = rendered.get(item);
            long end = item + 1 < rendered.size()
                    ? rendered.get(item + 1).frameIndex() : plan.frameCount();
            Path asset = asset(directory, item);
            for (long index = frame.frameIndex(); index < end; index++) {
                Path link = frame(directory, index);
                try {
                    Files.createLink(link, asset);
                } catch (UnsupportedOperationException | IOException hardLinkFailure) {
                    try {
                        Files.createSymbolicLink(link, asset.getFileName());
                    } catch (UnsupportedOperationException | IOException symbolicLinkFailure) {
                        throw new IOException("Cannot create a space-efficient staged frame link " + link,
                                symbolicLinkFailure);
                    }
                }
            }
        }
    }

    static void encode(Path ffmpeg, Encoder encoder, Path directory, Path audioRaw,
                       Path output, int fps, int bitRate, long durationMs) throws IOException {
        Path temporary = output.resolveSibling("." + output.getFileName() + ".encoding-"
                + UUID.randomUUID() + ".mp4");
        try {
            runProcess(command(ffmpeg, encoder, directory, audioRaw, temporary,
                    fps, bitRate, durationMs), "ffmpeg bulk encode", false);
            runProcess(List.of(ffmpeg.toString(), "-v", "error", "-i", temporary.toString(),
                    "-map", "0:v:0", "-map", "0:a:0", "-f", "null", "-"),
                    "ffmpeg decode verification", true);
            try {
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void replaceAudio(Path ffmpeg, Path source, Path audioRaw, long durationMs)
            throws IOException {
        if (!Files.isRegularFile(source)) throw new IOException("video does not exist: " + source);
        Path temporary = source.resolveSibling("." + source.getFileName() + ".audio-"
                + UUID.randomUUID() + ".mp4");
        List<String> command = new ArrayList<>();
        command.add(ffmpeg.toString());
        command.add("-y");
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("warning");
        command.add("-i");
        command.add(source.toString());
        command.add("-f");
        command.add("s16le");
        command.add("-ar");
        command.add(String.valueOf(AudioTimeline.SAMPLE_RATE));
        command.add("-ac");
        command.add(String.valueOf(AudioTimeline.CHANNELS));
        command.add("-i");
        command.add(audioRaw.toString());
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("1:a:0");
        command.add("-c:v");
        command.add("copy");
        command.add("-af");
        command.add("loudnorm=I=-16:TP=-1.5:LRA=11");
        command.add("-c:a");
        command.add("aac");
        command.add("-ar");
        command.add(String.valueOf(AudioTimeline.SAMPLE_RATE));
        command.add("-b:a");
        command.add("192000");
        command.add("-t");
        command.add(String.format(Locale.ROOT, "%.3f", durationMs / 1000d));
        command.add("-movflags");
        command.add("+faststart");
        command.add(temporary.toString());
        try {
            runProcess(command, "ffmpeg audio remux", false);
            runProcess(List.of(ffmpeg.toString(), "-v", "error", "-i", temporary.toString(),
                    "-map", "0:v:0", "-map", "0:a:0", "-f", "null", "-"),
                    "ffmpeg remux decode verification", true);
            publish(temporary, source);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void publish(Path temporary, Path output) throws IOException {
        try {
            Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void runProcess(List<String> command, String operation,
                                   boolean failOnDiagnostics) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String diagnostics;
        try (InputStream input = process.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            input.transferTo(bytes);
            diagnostics = bytes.toString(StandardCharsets.UTF_8);
        }
        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("ffmpeg encoding interrupted", ex);
        }
        if (code != 0 || (failOnDiagnostics && !diagnostics.isBlank())) {
            throw new IOException(operation + " failed (exit " + code + "): " + diagnostics.trim());
        }
    }

    static List<String> command(Path ffmpeg, Encoder encoder, Path directory, Path audioRaw,
                                Path output, int fps, int bitRate, long durationMs) {
        List<String> command = new ArrayList<>();
        command.add(ffmpeg.toString());
        command.add("-y");
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("warning");
        command.add("-framerate");
        command.add(String.valueOf(fps));
        command.add("-i");
        command.add(directory.resolve("frame-%08d.jpg").toString());
        command.add("-f");
        command.add("s16le");
        command.add("-ar");
        command.add(String.valueOf(AudioTimeline.SAMPLE_RATE));
        command.add("-ac");
        command.add(String.valueOf(AudioTimeline.CHANNELS));
        command.add("-i");
        command.add(audioRaw.toString());
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("1:a:0");
        command.add("-c:v");
        command.add(encoder.name());
        command.add("-b:v");
        command.add(String.valueOf(bitRate));
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-g");
        command.add(String.valueOf(fps * 2));
        if (encoder.hardware()) {
            command.add("-profile:v");
            command.add("high");
            command.add("-allow_sw");
            command.add("0");
            command.add("-realtime");
            command.add("1");
            command.add("-prio_speed");
            command.add("1");
            command.add("-power_efficient");
            command.add("1");
            command.add("-tag:v");
            command.add("avc1");
        } else if ("libx264".equals(encoder.name())) {
            command.add("-preset");
            command.add("veryfast");
        }
        command.add("-af");
        command.add("loudnorm=I=-16:TP=-1.5:LRA=11");
        command.add("-c:a");
        command.add("aac");
        command.add("-ar");
        command.add(String.valueOf(AudioTimeline.SAMPLE_RATE));
        command.add("-b:a");
        command.add("192000");
        command.add("-t");
        command.add(String.format(Locale.ROOT, "%.3f", durationMs / 1000d));
        command.add("-movflags");
        command.add("+faststart");
        command.add(output.toString());
        return List.copyOf(command);
    }

    static Path asset(Path directory, int index) {
        return directory.resolve(String.format(Locale.ROOT, "asset-%05d.jpg", index));
    }

    private static Path frame(Path directory, long index) {
        return directory.resolve(String.format(Locale.ROOT, "frame-%08d.jpg", index));
    }

    static void deleteTree(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static String capture(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] bytes;
        try (InputStream input = process.getInputStream()) {
            bytes = input.readAllBytes();
        }
        try {
            if (process.waitFor() != 0) throw new IOException("ffmpeg capability query failed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("ffmpeg capability query interrupted", ex);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    record Encoder(String name, boolean hardware) { }
}
