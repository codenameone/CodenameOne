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

import com.codename1.io.Log;
import com.codename1.mcp.MCP;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Codename One application lifecycle and CLI dispatcher. */
public final class VideoBuilder {
    private Form current;

    public void init(Object context) {
        try {
            Resources theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
        } catch (Exception ex) {
            Log.e(ex);
        }
    }

    public void start() {
        VideoBuilderCommand command = VideoBuilderCommand.get();
        if (current != null && command.isInteractive()) { current.show(); return; }
        switch (command.name()) {
            case "validate" -> background(() -> validate(command));
            case "prepare" -> background(() -> prepare(command));
            case "render" -> background(() -> render(command));
            case "remux" -> background(() -> remux(command));
            case "frame" -> background(() -> frame(command));
            case "preview" -> preview(command);
            case "mcp" -> startMcp(command);
            default -> showHelp();
        }
    }

    public void stop() { current = Display.getInstance().getCurrent(); }
    public void destroy() { MCP.stop(); }

    private void validate(VideoBuilderCommand command) throws Exception {
        VideoScript script = VideoScript.load(command.script());
        VideoBuilderCommand.emit(script.summaryJson());
    }

    private void prepare(VideoBuilderCommand command) throws Exception {
        VideoScript script = VideoScript.load(command.script());
        List<NarrationService.PreparedNarration> prepared = new NarrationService(script).prepare();
        StringBuilder json = new StringBuilder("{\"ok\":true,\"preparedNarration\":")
                .append(prepared.size()).append(",\"durationMs\":").append(script.getDurationMs())
                .append(",\"cues\":[");
        for (NarrationService.PreparedNarration cue : prepared) {
            if (json.charAt(json.length() - 1) != '[') json.append(',');
            json.append("{\"id\":\"").append(VideoScript.escape(cue.sceneId()))
                    .append("\",\"atMs\":").append(cue.atMs())
                    .append(",\"durationMs\":").append(cue.durationMs()).append('}');
        }
        VideoBuilderCommand.emit(json.append("]}").toString());
    }

    private void render(VideoBuilderCommand command) throws Exception {
        VideoScript script = VideoScript.load(command.script());
        List<NarrationService.PreparedNarration> narration = hasNarration(script)
                ? new NarrationService(script).prepare() : List.of();
        Path outputDirectory = command.output().toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);
        List<SubtitleWriter.SubtitleFile> subtitles = SubtitleWriter.write(
                outputDirectory, script.getId(), narration);
        List<String> orientations = command.orientation().equals("both")
                ? List.of("landscape", "portrait") : List.of(command.orientation());
        List<VideoRenderer.RenderResult> results = new ArrayList<>();
        for (String orientation : orientations) {
            Path output = outputDirectory.resolve(script.getId() + "-" + orientation + ".mp4");
            VideoRenderer renderer = new VideoRenderer(script, orientation);
            results.add(renderer.render(output, narration, (position, duration, scene) ->
                    System.err.println("render " + orientation + " " + position + "/" + duration + " scene=" + scene)));
        }
        Path report = outputDirectory.resolve(script.getId() + "-report.json");
        String json = reportJson(script, results, subtitles);
        Files.writeString(report, json, StandardCharsets.UTF_8);
        VideoBuilderCommand.emit(json);
    }

    /** Rebuilds narration, captions, and the mixed audio track without repainting video frames. */
    private void remux(VideoBuilderCommand command) throws Exception {
        VideoScript script = VideoScript.load(command.script());
        List<NarrationService.PreparedNarration> narration = hasNarration(script)
                ? new NarrationService(script).prepare() : List.of();
        Path outputDirectory = command.output().toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);
        SubtitleWriter.write(outputDirectory, script.getId(), narration);
        AudioTimeline timeline = new AudioTimeline(script, narration);
        Path raw = Files.createTempFile(outputDirectory, "." + script.getId() + "-audio-", ".s16le");
        try {
            timeline.writeRaw(raw);
            Path ffmpeg = StagedVideoEncoder.findFfmpeg();
            List<String> orientations = command.orientation().equals("both")
                    ? List.of("landscape", "portrait") : List.of(command.orientation());
            for (String orientation : orientations) {
                Path source = outputDirectory.resolve(script.getId() + "-" + orientation + ".mp4");
                StagedVideoEncoder.replaceAudio(ffmpeg, source, raw, timeline.durationMs());
            }
        } finally {
            Files.deleteIfExists(raw);
        }
        VideoBuilderCommand.emit("{\"ok\":true,\"id\":\"" + VideoScript.escape(script.getId())
                + "\",\"operation\":\"remux\",\"durationMs\":" + timeline.durationMs() + "}");
    }

    private void frame(VideoBuilderCommand command) throws Exception {
        VideoScript script = VideoScript.load(command.script());
        String orientation = command.orientation().equals("both")
                ? "landscape" : command.orientation();
        long positionMs = Long.getLong("video.frameAtMs", 0L);
        Path directory = command.output().toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path output = directory.resolve(script.getId() + "-" + orientation + "-"
                + positionMs + ".jpg");
        new VideoRenderer(script, orientation).captureFrame(output, positionMs);
        VideoBuilderCommand.emit("{\"ok\":true,\"path\":\""
                + VideoScript.escape(output.toString()) + "\",\"atMs\":" + positionMs + "}");
    }

    private void preview(VideoBuilderCommand command) {
        try {
            VideoScript script = VideoScript.load(command.script());
            String orientation = command.orientation().equals("both") ? "landscape" : command.orientation();
            VideoRenderer renderer = new VideoRenderer(script, orientation);
            current = renderer.getForm();
            renderer.showPreview();
        } catch (Exception ex) {
            Log.e(ex);
            Dialog.show("Preview failed", ex.getMessage(), "Close", null);
        }
    }

    private void startMcp(VideoBuilderCommand command) {
        VideoBuilderMcp.register();
        if (command.stdio()) MCP.startStdioServer();
        else MCP.startSocketServer(command.port() > 0 ? command.port() : 8642);
        System.err.println("MCP_CONNECTED transport=" + (command.stdio() ? "stdio" : "socket")
                + (command.stdio() ? "" : " port=" + (command.port() > 0 ? command.port() : 8642)));
        current = new Form("Video Builder MCP");
        current.add(new Label("MCP server is running"));
        if (!command.stdio()) current.show();
    }

    private void showHelp() {
        current = new Form("Codename One Video Builder");
        current.add(new Label("Use validate, prepare, preview, render, remux, frame, or mcp."));
        current.show();
        System.err.println("Usage: video-builder validate|prepare|preview|render|remux|frame <video.json> [--orientation landscape|portrait|both] [--output dir]");
    }

    private void background(ThrowingRunnable task) {
        Thread thread = new Thread(() -> {
            int code = 0;
            try { task.run(); }
            catch (VideoScript.ScriptException ex) { code = 2; error(ex); }
            catch (java.io.IOException ex) { code = 3; error(ex); }
            catch (Exception ex) { code = 4; error(ex); }
            System.exit(code);
        }, "video-builder-command");
        thread.start();
    }

    private static boolean hasNarration(VideoScript script) {
        for (VideoScript.Scene scene : script.getScenes()) {
            if (scene.narration() != null && !scene.narration().text().isBlank()) return true;
            for (VideoScript.Action action : scene.actions()) {
                if ("narration.cue".equals(action.type())
                        && !VideoScript.string(action.values().get("text"), "").isBlank()) return true;
            }
        }
        return false;
    }

    private static String reportJson(VideoScript script, List<VideoRenderer.RenderResult> results,
                                     List<SubtitleWriter.SubtitleFile> subtitles) throws Exception {
        StringBuilder out = new StringBuilder("{\"ok\":true,\"id\":\"").append(VideoScript.escape(script.getId())).append("\",\"outputs\":[");
        for (VideoRenderer.RenderResult result : results) {
            if (out.charAt(out.length() - 1) != '[') out.append(',');
            out.append("{\"path\":\"").append(VideoScript.escape(result.path().toString())).append("\",\"orientation\":\"")
                    .append(result.orientation()).append("\",\"width\":").append(result.width()).append(",\"height\":")
                    .append(result.height()).append(",\"frameRate\":").append(result.frameRate()).append(",\"durationMs\":")
                    .append(result.durationMs()).append(",\"frameCount\":").append(result.frameCount())
                    .append(",\"sha256\":\"").append(fileSha256(result.path())).append("\"}");
        }
        out.append("],\"subtitles\":[");
        for (SubtitleWriter.SubtitleFile subtitle : subtitles) {
            if (out.charAt(out.length() - 1) != '[') out.append(',');
            out.append("{\"format\":\"").append(subtitle.format()).append("\",\"path\":\"")
                    .append(VideoScript.escape(subtitle.path().toString())).append("\",\"sha256\":\"")
                    .append(fileSha256(subtitle.path())).append("\"}");
        }
        return out.append("]}").toString();
    }

    private static String fileSha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        StringBuilder out = new StringBuilder();
        for (byte value : digest.digest()) out.append(String.format("%02x", value & 0xff));
        return out.toString();
    }

    private static void error(Exception ex) {
        ex.printStackTrace(System.err);
        VideoBuilderCommand.emit("{\"ok\":false,\"error\":\"" + VideoScript.escape(String.valueOf(ex.getMessage())) + "\"}");
    }

    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}
