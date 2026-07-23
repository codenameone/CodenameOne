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

import com.codename1.ai.Tool;
import com.codename1.io.JSONParser;
import com.codename1.mcp.MCP;
import com.codename1.ui.Display;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Domain-specific MCP surface for LLM-driven video generation. */
final class VideoBuilderMcp {
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final Map<String, Job> JOBS = new ConcurrentHashMap<>();
    private static volatile boolean registered;

    private VideoBuilderMcp() { }

    static synchronized void register() {
        if (registered) return;
        registered = true;
        MCP.getServer().setServerInfo("Codename One Video Builder", "1.0");
        MCP.addTool(new Tool("video_schema", "Return the version 1 video script JSON schema.", emptySchema(), arguments -> schema()));
        MCP.addTool(new Tool("video_validate", "Validate a video script and compiled demo references.", pathSchema(), arguments -> {
            VideoScript script = VideoScript.load(path(arguments));
            return script.summaryJson();
        }));
        MCP.addTool(new Tool("video_prepare", "Prepare and cache local narration as an asynchronous job.", pathSchema(), arguments -> start(arguments, false)));
        MCP.addTool(new Tool("video_render", "Render landscape, portrait, or both as an asynchronous job.", renderSchema(), arguments -> start(arguments, true)));
        MCP.addTool(new Tool("video_preview", "Open an interactive preview for a script.", renderSchema(), arguments -> preview(arguments)));
        MCP.addTool(new Tool("video_status", "Return a video job's state and progress.", jobSchema(), arguments -> status(arguments)));
        MCP.addTool(new Tool("video_cancel", "Cancel a running video job.", jobSchema(), arguments -> cancel(arguments)));
    }

    private static String start(String arguments, boolean render) throws Exception {
        Map<String, Object> args = JSONParser.parseJSON(arguments);
        String id = "video-" + IDS.incrementAndGet();
        Job job = new Job(id);
        JOBS.put(id, job);
        Thread thread = new Thread(() -> run(job, args, render), "video-builder-mcp-" + id);
        job.thread = thread;
        thread.start();
        return "{\"jobId\":\"" + id + "\",\"state\":\"queued\"}";
    }

    private static void run(Job job, Map<String, Object> args, boolean render) {
        job.state = "running";
        try {
            Path scriptPath = Path.of(VideoScript.string(args.get("path"), ""));
            VideoScript script = VideoScript.load(scriptPath);
            List<NarrationService.PreparedNarration> narration = new NarrationService(script).prepare();
            if (job.cancelled) { job.state = "cancelled"; return; }
            if (!render) {
                job.result = "{\"preparedNarration\":" + narration.size() + "}";
                job.state = "completed";
                return;
            }
            String orientation = VideoScript.string(args.get("orientation"), "landscape");
            requireOrientation(orientation);
            Path output = Path.of(VideoScript.string(args.get("output"), "output")).toAbsolutePath().normalize();
            Files.createDirectories(output);
            List<SubtitleWriter.SubtitleFile> subtitles = SubtitleWriter.write(output, script.getId(), narration);
            List<String> modes = orientation.equals("both") ? List.of("landscape", "portrait") : List.of(orientation);
            List<String> paths = new ArrayList<>();
            for (String mode : modes) {
                VideoRenderer renderer = new VideoRenderer(script, mode);
                job.renderer = renderer;
                Path file = output.resolve(script.getId() + "-" + mode + ".mp4");
                renderer.render(file, narration, (position, duration, scene) -> {
                    job.positionMs = position; job.durationMs = duration; job.scene = scene;
                });
                paths.add(file.toString());
                if (job.cancelled) { job.state = "cancelled"; return; }
            }
            StringBuilder result = new StringBuilder("{\"outputs\":[");
            for (String path : paths) {
                if (result.charAt(result.length() - 1) != '[') result.append(',');
                result.append('\"').append(VideoScript.escape(path)).append('\"');
            }
            result.append("],\"subtitles\":[");
            for (SubtitleWriter.SubtitleFile subtitle : subtitles) {
                if (result.charAt(result.length() - 1) != '[') result.append(',');
                result.append('"').append(VideoScript.escape(subtitle.path().toString())).append('"');
            }
            job.result = result.append("]}").toString();
            job.state = "completed";
        } catch (Exception ex) {
            job.error = String.valueOf(ex.getMessage());
            job.state = job.cancelled ? "cancelled" : "failed";
        }
    }

    private static String preview(String arguments) throws Exception {
        Map<String, Object> args = JSONParser.parseJSON(arguments);
        VideoScript script = VideoScript.load(Path.of(VideoScript.string(args.get("path"), "")));
        String orientation = VideoScript.string(args.get("orientation"), "landscape");
        requireOrientation(orientation);
        String previewOrientation = "both".equals(orientation) ? "landscape" : orientation;
        Display.getInstance().callSerially(() -> new VideoRenderer(script, previewOrientation).showPreview());
        return "{\"ok\":true}";
    }

    private static String status(String arguments) throws Exception {
        Job job = job(arguments);
        return "{\"jobId\":\"" + job.id + "\",\"state\":\"" + job.state + "\",\"positionMs\":"
                + job.positionMs + ",\"durationMs\":" + job.durationMs + ",\"scene\":\"" + VideoScript.escape(job.scene)
                + "\",\"result\":" + (job.result == null ? "null" : job.result) + ",\"error\":"
                + (job.error == null ? "null" : "\"" + VideoScript.escape(job.error) + "\"") + "}";
    }

    private static String cancel(String arguments) throws Exception {
        Job job = job(arguments);
        job.cancelled = true;
        if (job.renderer != null) job.renderer.cancel();
        if (job.thread != null) job.thread.interrupt();
        return "{\"jobId\":\"" + job.id + "\",\"state\":\"cancelling\"}";
    }

    private static Job job(String arguments) throws Exception {
        String id = VideoScript.string(JSONParser.parseJSON(arguments).get("jobId"), "");
        Job job = JOBS.get(id);
        if (job == null) throw new IllegalArgumentException("Unknown jobId: " + id);
        return job;
    }

    private static Path path(String arguments) throws Exception {
        return Path.of(VideoScript.string(JSONParser.parseJSON(arguments).get("path"), ""));
    }

    private static void requireOrientation(String orientation) {
        if (!"landscape".equals(orientation) && !"portrait".equals(orientation) && !"both".equals(orientation)) {
            throw new IllegalArgumentException("orientation must be landscape, portrait, or both");
        }
    }

    private static String schema() throws Exception {
        try (InputStream input = VideoBuilderMcp.class.getResourceAsStream("/video-script.schema.json")) {
            if (input == null) throw new IllegalStateException("Bundled schema is missing");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static String emptySchema() { return "{\"type\":\"object\",\"properties\":{}}"; }
    private static String pathSchema() { return "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}"; }
    private static String renderSchema() { return "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"orientation\":{\"enum\":[\"landscape\",\"portrait\",\"both\"]},\"output\":{\"type\":\"string\"}},\"required\":[\"path\"]}"; }
    private static String jobSchema() { return "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"}},\"required\":[\"jobId\"]}"; }

    private static final class Job {
        final String id;
        volatile String state = "queued";
        volatile long positionMs;
        volatile long durationMs;
        volatile String scene = "";
        volatile String result;
        volatile String error;
        volatile boolean cancelled;
        volatile Thread thread;
        volatile VideoRenderer renderer;
        Job(String id) { this.id = id; }
    }
}
