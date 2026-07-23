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

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/** Resolves local TTS providers and caches their WAV output. */
public final class NarrationService {
    private final VideoScript script;
    private final Path cacheDirectory;
    private final List<NarrationProvider> providers;

    public NarrationService(VideoScript script) {
        this(script, defaultProviders(script));
    }

    NarrationService(VideoScript script, List<NarrationProvider> providers) {
        this.script = script;
        this.providers = List.copyOf(providers);
        String override = System.getProperty("video.cache", "");
        cacheDirectory = override.isEmpty()
                ? script.getProjectDirectory().resolve(".video-cache/narration")
                : Path.of(override).resolve("narration");
    }

    public List<PreparedNarration> prepare() throws Exception {
        Files.createDirectories(cacheDirectory);
        List<PreparedNarration> out = new ArrayList<>();
        long sceneStart = 0;
        long narrationEnd = Long.MIN_VALUE;
        for (VideoScript.Scene scene : script.getScenes()) {
            long sceneEnd = sceneStart + scene.durationMs();
            VideoScript.Narration item = scene.narration();
            if (item != null && !item.text().isBlank()) {
                long scheduled = schedule(sceneStart, narrationEnd, false);
                PreparedNarration prepared = prepareItem(scene.id(), scheduled, sceneEnd - scheduled,
                        item, item.text());
                out.add(prepared);
                narrationEnd = Math.max(narrationEnd, prepared.atMs() + prepared.durationMs());
            }
            for (VideoScript.Action action : scene.actions()) {
                if (!"narration.cue".equals(action.type())) continue;
                VideoScript.Narration cue = narration(action.values());
                String cueId = scene.id() + ":" + (action.id().isBlank() ? action.atMs() : action.id());
                long requested = sceneStart + action.atMs();
                long scheduled = schedule(requested, narrationEnd,
                        VideoScript.bool(action.values().get("allowOverlap"), false));
                String caption = VideoScript.string(action.values().get("caption"), cue.text());
                PreparedNarration prepared = prepareItem(cueId, scheduled, sceneEnd - scheduled,
                        cue, caption);
                out.add(prepared);
                narrationEnd = Math.max(narrationEnd, prepared.atMs() + prepared.durationMs());
            }
            sceneStart += scene.durationMs();
        }
        return out;
    }

    private long schedule(long requested, long narrationEnd, boolean allowOverlap) {
        if (allowOverlap || narrationEnd == Long.MIN_VALUE) return requested;
        return Math.max(requested, narrationEnd + script.getNarration().minimumGapMs());
    }

    private PreparedNarration prepareItem(String id, long atMs, long availableMs,
                                          VideoScript.Narration item, String caption) throws Exception {
        String providerId = item.provider().isBlank() ? script.getNarration().provider() : item.provider();
        NarrationProvider provider = select(providerId);
        String voice = item.voice().isBlank() ? script.getNarration().voice() : item.voice();
        String language = item.language().isBlank() ? script.getNarration().language() : item.language();
        float speed = item.speed() > 0 ? item.speed() : script.getNarration().speed();
        String spokenText = applyPronunciations(item.text());
        NarrationProvider.NarrationRequest request = new NarrationProvider.NarrationRequest(spokenText, voice, language, speed);
        String key = sha256(provider.getId() + "\n" + provider.fingerprint() + "\n" + voice + "\n"
                + language + "\n" + speed + "\n" + spokenText);
        Path wav = cacheDirectory.resolve(key + ".wav");
        if (!Files.isRegularFile(wav) || Files.size(wav) < 44) {
            Path partial = cacheDirectory.resolve(
                    key + ".partial-" + UUID.randomUUID() + ".wav");
            try {
                provider.synthesize(request, partial);
                WavPcm.read(partial);
                try {
                    Files.move(partial, wav, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(partial, wav);
                } catch (FileAlreadyExistsException ex) {
                    // Another process published the same content-addressed cue.
                    // Its validated file is the cache winner.
                }
            } finally {
                Files.deleteIfExists(partial);
            }
        }
        WavPcm.AudioData data = WavPcm.read(wav).normalized(48_000, 2);
        long durationMs = data.frameCount() * 1000L / data.sampleRate();
        if (durationMs > availableMs && !"extend".equals(item.overflow())) {
            throw new IOException("Narration '" + id + "' is " + durationMs
                    + " ms but only " + availableMs + " ms remain in the scene");
        }
        return new PreparedNarration(id, caption, atMs, wav, data, item.gain(), durationMs);
    }

    private String applyPronunciations(String text) {
        String out = text;
        List<Map.Entry<String, String>> pronunciations = new ArrayList<>(
                script.getNarration().pronunciations().entrySet());
        pronunciations.sort(Comparator.comparingInt(
                (Map.Entry<String, String> entry) -> entry.getKey().length()).reversed());
        for (Map.Entry<String, String> pronunciation : pronunciations) {
            String source = pronunciation.getKey();
            if (source.isEmpty()) continue;
            String prefix = isWordCharacter(source.charAt(0)) ? "(?<![A-Za-z0-9_])" : "";
            String suffix = isWordCharacter(source.charAt(source.length() - 1))
                    ? "(?![A-Za-z0-9_])" : "";
            out = Pattern.compile(prefix + Pattern.quote(source) + suffix)
                    .matcher(out).replaceAll(Matcher.quoteReplacement(pronunciation.getValue()));
        }
        return out;
    }

    private static boolean isWordCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    private static VideoScript.Narration narration(Map<String, Object> values) {
        return new VideoScript.Narration(VideoScript.string(values.get("text"), ""),
                VideoScript.string(values.get("provider"), ""), VideoScript.string(values.get("voice"), ""),
                VideoScript.string(values.get("language"), ""), VideoScript.decimal(values.get("speed"), -1f),
                VideoScript.decimal(values.get("gain"), 1f), VideoScript.string(values.get("overflow"), "error"));
    }

    private NarrationProvider select(String requested) throws IOException {
        if (!"auto".equals(requested)) {
            for (NarrationProvider provider : providers) {
                if (provider.getId().equals(requested)) {
                    if (!provider.isAvailable()) throw new IOException("Narration provider '" + requested + "' is not available");
                    return provider;
                }
            }
            throw new IOException("Unknown narration provider: " + requested);
        }
        for (NarrationProvider provider : providers) if (provider.isAvailable()) return provider;
        throw new IOException("No local narration provider is available. Configure Kokoro, Piper, or narration.command");
    }

    static List<NarrationProvider> defaultProviders(VideoScript script) {
        VideoScript.NarrationConfig config = script.getNarration();
        List<NarrationProvider> out = new ArrayList<>();
        String configured = config.executable();
        String kokoroExecutable = "kokoro".equals(config.provider()) && !configured.isBlank()
                ? configured : findExecutable("kokoro-tts");
        String piperExecutable = "piper".equals(config.provider()) && !configured.isBlank()
                ? configured : findExecutable("piper");
        out.add(new ProcessProvider("kokoro", kokoroExecutable,
                config, script.getProjectDirectory(), ProcessProvider.Kind.KOKORO));
        out.add(new ProcessProvider("piper", piperExecutable,
                config, script.getProjectDirectory(), ProcessProvider.Kind.PIPER));
        if (!config.command().isEmpty()) {
            out.add(new ProcessProvider("command", "configured", config,
                    script.getProjectDirectory(), ProcessProvider.Kind.GENERIC));
        }
        return out;
    }

    static String findExecutable(String name) {
        String path = System.getenv("PATH");
        if (path != null) {
            for (String directory : path.split(java.io.File.pathSeparator)) {
                String found = executableIn(Path.of(directory), name);
                if (!found.isBlank()) return found;
            }
        }
        String local = executableIn(Path.of(System.getProperty("user.home"), ".local", "bin"), name);
        if (!local.isBlank()) return local;
        return "";
    }

    private static String executableIn(Path directory, String name) {
        Path candidate = directory.resolve(name);
        if (Files.isExecutable(candidate)) return candidate.toString();
        Path windows = directory.resolve(name + ".exe");
        return Files.isExecutable(windows) ? windows.toString() : "";
    }

    private static String sha256(String input) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte value : digest) out.append(String.format("%02x", value & 0xff));
        return out.toString();
    }

    public record PreparedNarration(String sceneId, String text, long atMs, Path path,
                                    WavPcm.AudioData audio, float gain, long durationMs) { }

    private static final class ProcessProvider implements NarrationProvider {
        enum Kind { KOKORO, PIPER, GENERIC }
        private final String id;
        private final String executable;
        private final VideoScript.NarrationConfig config;
        private final Path projectDirectory;
        private final Kind kind;

        ProcessProvider(String id, String executable, VideoScript.NarrationConfig config,
                        Path projectDirectory, Kind kind) {
            this.id = id;
            this.executable = executable;
            this.config = config;
            this.projectDirectory = projectDirectory;
            this.kind = kind;
        }
        public String getId() { return id; }
        public String fingerprint() { return executable + "|" + config.model() + "|" + config.command(); }
        public boolean isAvailable() {
            if (kind == Kind.GENERIC) return !config.command().isEmpty();
            if (executable.isBlank() || !Files.isExecutable(Path.of(executable))) return false;
            return kind != Kind.PIPER || !config.model().isBlank();
        }

        public void synthesize(NarrationRequest request, Path outputWav) throws Exception {
            List<String> command = new ArrayList<>();
            Path input = outputWav.resolveSibling(outputWav.getFileName() + ".txt");
            Files.writeString(input, request.text(), StandardCharsets.UTF_8);
            if (kind == Kind.KOKORO) {
                command.add(executable); command.add(input.toString()); command.add(outputWav.toString());
                command.add("--format"); command.add("wav"); command.add("--speed"); command.add(String.valueOf(request.speed()));
                command.add("--lang"); command.add(request.language());
                if (!request.voice().isBlank()) { command.add("--voice"); command.add(request.voice()); }
                if (!config.model().isBlank()) {
                    Path model = resolveProjectPath(config.model());
                    command.add("--model"); command.add(model.toString());
                    command.add("--voices"); command.add(model.resolveSibling("voices-v1.0.bin").toString());
                }
            } else if (kind == Kind.PIPER) {
                command.add(executable); command.add("-m"); command.add(resolveProjectPath(config.model()).toString());
                command.add("-f"); command.add(outputWav.toString()); command.add("--"); command.add(request.text());
            } else {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("{input}", input.toString()); replacements.put("{output}", outputWav.toString());
                replacements.put("{text}", request.text()); replacements.put("{voice}", request.voice());
                replacements.put("{language}", request.language()); replacements.put("{speed}", String.valueOf(request.speed()));
                replacements.put("{model}", config.model());
                for (String part : config.command()) {
                    String resolved = part;
                    for (Map.Entry<String, String> replacement : replacements.entrySet()) resolved = resolved.replace(replacement.getKey(), replacement.getValue());
                    command.add(resolved);
                }
            }
            try {
                Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
                if (!process.waitFor(Duration.ofMinutes(5).toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    throw new IOException("Narration provider timed out: " + id);
                }
                if (process.exitValue() != 0 || !Files.isRegularFile(outputWav)) {
                    throw new IOException("Narration provider failed: " + id + " (exit " + process.exitValue() + ")");
                }
            } finally {
                Files.deleteIfExists(input);
            }
        }

        private Path resolveProjectPath(String value) {
            Path path = Path.of(value);
            return path.isAbsolute() ? path.normalize() : projectDirectory.resolve(path).normalize();
        }
    }
}
