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

import com.codename1.io.JSONParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Parsed, validated version 1 video script. */
public final class VideoScript {
    public record Output(int frameRate, int landscapeWidth, int landscapeHeight,
                         int portraitWidth, int portraitHeight, int videoBitRate,
                         String encodingPipeline) { }
    public record NarrationConfig(String provider, String voice, String language,
                                  float speed, String executable, String model,
                                  List<String> command, Map<String, String> pronunciations,
                                  int minimumGapMs) { }
    public record Narration(String text, String provider, String voice, String language,
                            float speed, float gain, String overflow) { }
    public record Bounds(float x, float y, float width, float height) { }
    public record Action(String type, long atMs, long durationMs, String id,
                         Map<String, Object> values) { }
    public record Scene(String id, long durationMs, Narration narration,
                        Bounds bounds, Map<String, Bounds> orientationBounds,
                        Map<String, String> composition,
                        List<Action> actions) { }
    public record AudioTrack(String path, long atMs, float gain, boolean loop) { }

    private final Path source;
    private final Path projectDirectory;
    private final String id;
    private final String title;
    private final Output output;
    private final NarrationConfig narration;
    private final List<AudioTrack> audio;
    private final List<Scene> scenes;

    private VideoScript(Path source, String id, String title, Output output,
                        NarrationConfig narration, List<AudioTrack> audio, List<Scene> scenes) {
        this.source = source;
        this.projectDirectory = source.toAbsolutePath().normalize().getParent();
        this.id = id;
        this.title = title;
        this.output = output;
        this.narration = narration;
        this.audio = Collections.unmodifiableList(audio);
        this.scenes = Collections.unmodifiableList(scenes);
    }

    public static VideoScript load(Path path) throws IOException {
        Path source = path.toAbsolutePath().normalize();
        Map<String, Object> root = JSONParser.parseJSON(Files.readAllBytes(source));
        List<String> errors = new ArrayList<>();
        int version = integer(root.get("schemaVersion"), 0);
        if (version != 1) errors.add("schemaVersion must be 1");
        String id = string(root.get("id"), "");
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) errors.add("id must be filesystem-safe");
        String title = string(root.get("title"), id);

        Map<String, Object> outputMap = map(root.get("output"));
        Output output = new Output(integer(outputMap.get("frameRate"), 30),
                integer(outputMap.get("landscapeWidth"), 1920),
                integer(outputMap.get("landscapeHeight"), 1080),
                integer(outputMap.get("portraitWidth"), 1080),
                integer(outputMap.get("portraitHeight"), 1920),
                integer(outputMap.get("videoBitRate"), 8_000_000),
                string(outputMap.get("encodingPipeline"), "staged"));
        if (output.frameRate <= 0 || output.frameRate > 60) errors.add("output.frameRate must be between 1 and 60");
        if (output.landscapeWidth <= 0 || output.landscapeHeight <= 0 || output.portraitWidth <= 0 || output.portraitHeight <= 0) {
            errors.add("output dimensions must be positive");
        }
        if ((output.landscapeWidth & 1) != 0 || (output.landscapeHeight & 1) != 0
                || (output.portraitWidth & 1) != 0 || (output.portraitHeight & 1) != 0) {
            errors.add("output dimensions must be even for H.264 encoding");
        }
        if (output.videoBitRate <= 0) errors.add("output.videoBitRate must be positive");
        if (!"staged".equals(output.encodingPipeline) && !"videoio".equals(output.encodingPipeline)) {
            errors.add("output.encodingPipeline must be staged or videoio");
        }

        Map<String, Object> narrationMap = map(root.get("narration"));
        NarrationConfig narration = new NarrationConfig(
                string(narrationMap.get("provider"), "auto"), string(narrationMap.get("voice"), ""),
                string(narrationMap.get("language"), "en-us"), decimal(narrationMap.get("speed"), 1f),
                string(narrationMap.get("executable"), ""), string(narrationMap.get("model"), ""),
                strings(narrationMap.get("command")), stringMap(narrationMap.get("pronunciations")),
                integer(narrationMap.get("minimumGapMs"), 180));
        validateProvider(narration.provider, "narration.provider", errors, false);
        if (narration.speed <= 0) errors.add("narration.speed must be positive");
        if (narration.minimumGapMs < 0) errors.add("narration.minimumGapMs must not be negative");
        if ("auto".equals(narration.provider) && !narration.executable.isBlank()) {
            errors.add("narration.executable requires an explicit kokoro or piper provider");
        }

        List<AudioTrack> audio = new ArrayList<>();
        for (Object value : list(root.get("audio"))) {
            Map<String, Object> item = map(value);
            long atMs = number(item.get("atMs"), 0);
            float gain = decimal(item.get("gain"), 1f);
            if (atMs < 0) errors.add("audio.atMs must not be negative");
            if (gain < 0) errors.add("audio.gain must not be negative");
            audio.add(new AudioTrack(string(item.get("path"), ""), atMs, gain, bool(item.get("loop"), false)));
        }

        List<Scene> scenes = new ArrayList<>();
        Set<String> sceneIds = new HashSet<>();
        long total = 0;
        for (Object sceneValue : list(root.get("scenes"))) {
            Map<String, Object> item = map(sceneValue);
            String sceneId = string(item.get("id"), "scene-" + (scenes.size() + 1));
            if (!sceneIds.add(sceneId)) errors.add("duplicate scene id: " + sceneId);
            long duration = number(item.get("durationMs"), -1);
            if (duration <= 0) errors.add("scene " + sceneId + " durationMs must be positive");
            Map<String, Object> narrationItem = map(item.get("narration"));
            Narration sceneNarration = narrationItem.isEmpty() ? null : new Narration(
                    string(narrationItem.get("text"), ""), string(narrationItem.get("provider"), ""),
                    string(narrationItem.get("voice"), ""), string(narrationItem.get("language"), ""),
                    decimal(narrationItem.get("speed"), -1f), decimal(narrationItem.get("gain"), 1f),
                    string(narrationItem.get("overflow"), "error"));
            if (sceneNarration != null) {
                validateProvider(sceneNarration.provider, "scene " + sceneId + " narration.provider", errors, true);
                if (sceneNarration.speed != -1f && sceneNarration.speed <= 0) {
                    errors.add("scene " + sceneId + " narration.speed must be positive or omitted");
                }
                if (sceneNarration.gain < 0) errors.add("scene " + sceneId + " narration.gain must not be negative");
                if (!"error".equals(sceneNarration.overflow) && !"extend".equals(sceneNarration.overflow)) {
                    errors.add("scene " + sceneId + " narration.overflow must be error or extend");
                }
            }
            Bounds bounds = bounds(item.get("bounds"), new Bounds(0, 0, 1, 1));
            validateBounds(bounds, "scene " + sceneId, errors);
            Map<String, Bounds> overrides = new LinkedHashMap<>();
            Map<String, Object> orientation = map(item.get("orientation"));
            if (orientation.containsKey("landscape")) {
                Bounds override = bounds(orientation.get("landscape"), bounds);
                validateBounds(override, "scene " + sceneId + " landscape", errors);
                overrides.put("landscape", override);
            }
            if (orientation.containsKey("portrait")) {
                Bounds override = bounds(orientation.get("portrait"), bounds);
                validateBounds(override, "scene " + sceneId + " portrait", errors);
                overrides.put("portrait", override);
            }
            Map<String, String> composition = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map(item.get("composition")).entrySet()) {
                if (!"landscape".equals(entry.getKey()) && !"portrait".equals(entry.getKey())) {
                    errors.add("scene " + sceneId + " composition only supports landscape and portrait");
                } else {
                    String preset = string(entry.getValue(), "");
                    if (!"code-over-demo".equals(preset) && !"code-left-demo-right".equals(preset)) {
                        errors.add("scene " + sceneId + " has unknown composition preset " + preset);
                    }
                    composition.put(entry.getKey(), preset);
                }
            }

            List<Action> actions = new ArrayList<>();
            for (Object actionValue : list(item.get("actions"))) {
                Map<String, Object> actionMap = new LinkedHashMap<>(map(actionValue));
                String type = string(actionMap.remove("type"), "");
                long at = number(actionMap.remove("atMs"), 0);
                long actionDuration = number(actionMap.remove("durationMs"), 0);
                String actionId = string(actionMap.remove("id"), "");
                if (!isActionType(type)) errors.add("scene " + sceneId + " has unknown action type " + type);
                if (at < 0 || at > duration) errors.add("scene " + sceneId + " action " + type + " is outside the scene");
                if (actionDuration < 0 || at + actionDuration > duration) errors.add("scene " + sceneId + " action " + type + " duration is outside the scene");
                if (actionMap.containsKey("bounds")) validateBounds(bounds(actionMap.get("bounds"), bounds), "scene " + sceneId + " action " + type, errors);
                Map<String, Object> actionOrientation = map(actionMap.get("orientation"));
                if (actionOrientation.containsKey("landscape")) validateBounds(bounds(actionOrientation.get("landscape"), bounds), "scene " + sceneId + " action " + type + " landscape", errors);
                if (actionOrientation.containsKey("portrait")) validateBounds(bounds(actionOrientation.get("portrait"), bounds), "scene " + sceneId + " action " + type + " portrait", errors);
                if ("narration.cue".equals(type)) {
                    Narration cue = narration(actionMap);
                    validateNarration(cue, "scene " + sceneId + " narration cue", errors);
                }
                if ("replay".equals(type)) {
                    long from = number(actionMap.get("fromMs"), -1);
                    long to = number(actionMap.get("toMs"), -1);
                    long rewindDuration = number(actionMap.get("rewindDurationMs"), 700);
                    float playbackRate = decimal(actionMap.get("playbackRate"), 0.65f);
                    if (from < 0 || to <= from || to > at) {
                        errors.add("scene " + sceneId + " replay requires 0 <= fromMs < toMs <= atMs");
                    }
                    if (rewindDuration <= 0) errors.add("scene " + sceneId + " replay rewindDurationMs must be positive");
                    if (playbackRate <= 0 || playbackRate > 2f) errors.add("scene " + sceneId + " replay playbackRate must be greater than 0 and at most 2");
                    long replayDuration = playbackRate > 0 ? (long) Math.ceil(Math.max(0, to - from) / playbackRate) : Long.MAX_VALUE;
                    if (at + rewindDuration + replayDuration > duration) {
                        errors.add("scene " + sceneId + " replay does not fit inside the scene");
                    }
                }
                if ("transition".equals(type)) {
                    validateTransitionOption(string(actionMap.get("effect"), "fade"),
                            "scene " + sceneId + " transition effect", errors);
                    for (Map.Entry<String, Object> effect : map(actionMap.get("effects")).entrySet()) {
                        validateTransitionOption(string(effect.getValue(), ""),
                                "scene " + sceneId + " transition " + effect.getKey() + " effect", errors);
                    }
                    validateEasing(string(actionMap.get("easing"), "ease-in-out"),
                            "scene " + sceneId + " transition easing", errors);
                    for (Map.Entry<String, Object> easing : map(actionMap.get("easings")).entrySet()) {
                        validateEasing(string(easing.getValue(), ""),
                                "scene " + sceneId + " transition " + easing.getKey() + " easing", errors);
                    }
                }
                if ("layer.animate".equals(type)) {
                    validateEasing(string(actionMap.get("easing"), "ease-in-out"),
                            "scene " + sceneId + " layer animation easing", errors);
                }
                actions.add(new Action(type, at, actionDuration, actionId, Collections.unmodifiableMap(actionMap)));
            }
            scenes.add(new Scene(sceneId, duration, sceneNarration, bounds,
                    Collections.unmodifiableMap(overrides), Collections.unmodifiableMap(composition),
                    Collections.unmodifiableList(actions)));
            total += Math.max(duration, 0);
        }
        if (scenes.isEmpty()) errors.add("at least one scene is required");
        if (total > 86_400_000L) errors.add("video duration exceeds 24 hours");
        for (AudioTrack track : audio) {
            if (track.atMs > total) errors.add("audio.atMs is outside the video timeline: " + track.path);
        }
        if (!errors.isEmpty()) throw new ScriptException(errors);
        VideoScript script = new VideoScript(source, id, title, output, narration, audio, scenes);
        script.validateReferences();
        return script;
    }

    private void validateReferences() throws IOException {
        List<String> errors = new ArrayList<>();
        if (("kokoro".equals(narration.provider) || "piper".equals(narration.provider))
                && !narration.model.isBlank()) {
            validatePath(narration.model, "narration model", errors);
            if ("kokoro".equals(narration.provider)) {
                Path model = projectDirectory.resolve(narration.model).normalize();
                Path voices = model.resolveSibling("voices-v1.0.bin");
                if (!Files.isRegularFile(voices)) {
                    errors.add("Kokoro voice data not found beside narration model: " + voices.getFileName());
                }
            }
        }
        for (AudioTrack track : audio) validatePath(track.path, "audio", errors);
        for (Scene scene : scenes) {
            Map<String, Long> layerCreationTimes = new HashMap<>();
            Long demoMountAt = null;
            for (Action action : scene.actions) {
                if (("code.show".equals(action.type) || "code.type".equals(action.type)) && action.values.containsKey("path")) {
                    validatePath(string(action.values.get("path"), ""), "code", errors);
                }
                if ("svg.show".equals(action.type)) {
                    validateVisualPaths(action, "SVG", true, errors);
                }
                if ("image.show".equals(action.type)) {
                    validateVisualPaths(action, "image", true, errors);
                }
                if ("intro.show".equals(action.type)
                        && (action.values.containsKey("path") || action.values.containsKey("paths"))) {
                    validateVisualPaths(action, "intro logo", true, errors);
                }
                if ("diagram.show".equals(action.type)) {
                    validateVisualPaths(action, "diagram", false, errors);
                }
                if ("demo.mount".equals(action.type)) {
                    String className = string(action.values.get("class"), "");
                    try {
                        Class<?> type = Class.forName(className);
                        if (!DemoScene.class.isAssignableFrom(type)) errors.add(className + " does not implement DemoScene");
                    } catch (Throwable ex) {
                        errors.add("demo class not found: " + className);
                    }
                    demoMountAt = action.atMs;
                }
                if ("text.show".equals(action.type) || "code.show".equals(action.type)
                        || "demo.mount".equals(action.type) || "focus.show".equals(action.type)
                        || "pointer.show".equals(action.type) || "bullets.show".equals(action.type)
                        || "diagram.show".equals(action.type) || "svg.show".equals(action.type)
                        || "image.show".equals(action.type)
                        || "intro.show".equals(action.type) || "outro.show".equals(action.type)) {
                    if (!action.id.isBlank()) layerCreationTimes.put(action.id, action.atMs);
                }
                if ("code.type".equals(action.type) || "transition".equals(action.type)
                        || "layer.animate".equals(action.type)
                        || "text.hide".equals(action.type) || "layer.hide".equals(action.type)
                        || "focus.hide".equals(action.type) || "pointer.move".equals(action.type)
                        || "pointer.click".equals(action.type) || "pointer.hide".equals(action.type)) {
                    String target = string(action.values.get("target"), action.id);
                    Long createdAt = layerCreationTimes.get(target);
                    if (target.isBlank() || createdAt == null || createdAt > action.atMs) {
                        errors.add("scene " + scene.id + " action " + action.type + " references unavailable layer '" + target + "'");
                    }
                }
                if ("focus.show".equals(action.type)) {
                    validateAvailableLayer(scene, action, string(action.values.get("target"), ""), layerCreationTimes, errors);
                }
                if ("pointer.show".equals(action.type) || "pointer.move".equals(action.type)) {
                    String area = string(action.values.get("area"), "");
                    if (!area.isBlank()) validateAvailableLayer(scene, action, area, layerCreationTimes, errors);
                }
                if ("demo.action".equals(action.type) && (demoMountAt == null || demoMountAt > action.atMs)) {
                    errors.add("scene " + scene.id + " demo.action is used before demo.mount");
                }
            }
        }
        if (!errors.isEmpty()) throw new ScriptException(errors);
    }

    private void validatePath(String value, String label, List<String> errors) {
        if (value.length() == 0) { errors.add(label + " path is empty"); return; }
        Path resolved = projectDirectory.resolve(value).normalize();
        if (!resolved.startsWith(projectDirectory)) errors.add(label + " path escapes project directory: " + value);
        else if (!Files.isRegularFile(resolved)) errors.add(label + " file not found: " + value);
    }

    private void validateVisualPaths(Action action, String label, boolean required, List<String> errors) {
        boolean found = false;
        if (action.values.containsKey("path")) {
            found = true;
            validatePath(string(action.values.get("path"), ""), label, errors);
        }
        for (Map.Entry<String, Object> entry : map(action.values.get("paths")).entrySet()) {
            found = true;
            if (!"landscape".equals(entry.getKey()) && !"portrait".equals(entry.getKey())) {
                errors.add(label + " paths only supports landscape and portrait");
            } else {
                validatePath(string(entry.getValue(), ""), label + " " + entry.getKey(), errors);
            }
        }
        if (required && !found) errors.add(label + " path is empty");
    }

    private static void validateTransitionOption(String effect, String label, List<String> errors) {
        if (!Set.of("none", "fade", "slide-left", "slide-right", "slide-up", "slide-down",
                "zoom-in", "zoom-out", "morph", "wipe-left", "wipe-right", "wipe-up", "wipe-down").contains(effect)) {
            errors.add(label + " is unknown: " + effect);
        }
    }

    private static void validateEasing(String easing, String label, List<String> errors) {
        if (!Set.of("linear", "ease-in", "ease-out", "ease-in-out", "spring").contains(easing)) {
            errors.add(label + " is unknown: " + easing);
        }
    }

    private static boolean isActionType(String type) {
        return type.equals("text.show") || type.equals("text.hide") || type.equals("code.show")
                || type.equals("code.type") || type.equals("demo.mount") || type.equals("demo.action")
                || type.equals("layer.hide") || type.equals("layer.animate") || type.equals("transition")
                || type.equals("focus.show") || type.equals("focus.hide")
                || type.equals("pointer.show") || type.equals("pointer.move")
                || type.equals("pointer.click") || type.equals("pointer.hide")
                || type.equals("replay") || type.equals("narration.cue")
                || type.equals("bullets.show") || type.equals("diagram.show")
                || type.equals("svg.show") || type.equals("image.show") || type.equals("intro.show")
                || type.equals("outro.show");
    }

    private static Narration narration(Map<String, Object> item) {
        return new Narration(string(item.get("text"), ""), string(item.get("provider"), ""),
                string(item.get("voice"), ""), string(item.get("language"), ""),
                decimal(item.get("speed"), -1f), decimal(item.get("gain"), 1f),
                string(item.get("overflow"), "error"));
    }

    private static void validateNarration(Narration item, String label, List<String> errors) {
        validateProvider(item.provider, label + ".provider", errors, true);
        if (item.text.isBlank()) errors.add(label + " text must not be empty");
        if (item.speed != -1f && item.speed <= 0) errors.add(label + " speed must be positive or omitted");
        if (item.gain < 0) errors.add(label + " gain must not be negative");
        if (!"error".equals(item.overflow) && !"extend".equals(item.overflow)) {
            errors.add(label + " overflow must be error or extend");
        }
    }

    private static void validateAvailableLayer(Scene scene, Action action, String target,
                                               Map<String, Long> creationTimes, List<String> errors) {
        Long createdAt = creationTimes.get(target);
        if (target.isBlank() || createdAt == null || createdAt > action.atMs) {
            errors.add("scene " + scene.id + " action " + action.type + " references unavailable layer '" + target + "'");
        }
    }

    private static void validateProvider(String provider, String label, List<String> errors, boolean allowEmpty) {
        if (allowEmpty && provider.isBlank()) return;
        if (!"auto".equals(provider) && !"kokoro".equals(provider) && !"piper".equals(provider)
                && !"command".equals(provider)) {
            errors.add(label + " must be auto, kokoro, piper, or command");
        }
    }

    private static void validateBounds(Bounds bounds, String label, List<String> errors) {
        if (bounds.x < 0 || bounds.y < 0 || bounds.width <= 0 || bounds.height <= 0
                || bounds.x + bounds.width > 1.0001f || bounds.y + bounds.height > 1.0001f) {
            errors.add(label + " bounds must fit inside normalized canvas coordinates");
        }
    }

    public long getDurationMs() { long out = 0; for (Scene scene : scenes) out += scene.durationMs; return out; }
    public Path getSource() { return source; }
    public Path getProjectDirectory() { return projectDirectory; }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public Output getOutput() { return output; }
    public NarrationConfig getNarration() { return narration; }
    public List<AudioTrack> getAudio() { return audio; }
    public List<Scene> getScenes() { return scenes; }

    public static final class ScriptException extends IOException {
        private final List<String> errors;
        ScriptException(List<String> errors) { super(String.join("; ", errors)); this.errors = List.copyOf(errors); }
        public List<String> getErrors() { return errors; }
    }

    @SuppressWarnings("unchecked") static Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }
    @SuppressWarnings("unchecked") static List<Object> list(Object value) {
        return value instanceof List ? (List<Object>) value : Collections.emptyList();
    }
    static Map<String, String> stringMap(Object value) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map(value).entrySet()) {
            out.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }
    static List<String> strings(Object value) { List<String> out = new ArrayList<>(); for (Object item : list(value)) out.add(String.valueOf(item)); return out; }
    static String string(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }
    static int integer(Object value, int fallback) { return (int) number(value, fallback); }
    static long number(Object value, long fallback) { return value instanceof Number ? ((Number) value).longValue() : fallback; }
    static float decimal(Object value, float fallback) { return value instanceof Number ? ((Number) value).floatValue() : fallback; }
    static boolean bool(Object value, boolean fallback) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            String text = ((String) value).trim();
            if ("true".equalsIgnoreCase(text)) return true;
            if ("false".equalsIgnoreCase(text)) return false;
        }
        return fallback;
    }
    static Bounds bounds(Object value, Bounds fallback) {
        Map<String, Object> map = map(value);
        if (map.isEmpty()) return fallback;
        return new Bounds(decimal(map.get("x"), fallback.x), decimal(map.get("y"), fallback.y),
                decimal(map.get("width"), fallback.width), decimal(map.get("height"), fallback.height));
    }

    public String summaryJson() {
        return "{\"ok\":true,\"id\":\"" + escape(id) + "\",\"title\":\"" + escape(title)
                + "\",\"durationMs\":" + getDurationMs() + ",\"scenes\":" + scenes.size() + "}";
    }
    static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}
