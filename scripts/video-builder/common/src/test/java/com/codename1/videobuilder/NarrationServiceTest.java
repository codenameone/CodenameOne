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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NarrationServiceTest {
    @TempDir Path directory;

    @Test void reservesEncodeHeadroomInPcmOutput() {
        assertEquals(Math.round(32767f * AudioTimeline.OUTPUT_GAIN), AudioTimeline.pcmSample(1f));
        assertEquals(-Math.round(32767f * AudioTimeline.OUTPUT_GAIN), AudioTimeline.pcmSample(-1f));
    }

    @Test void cachesProviderOutputAndNormalizesPcm() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        NarrationProvider provider = fakeProvider(calls, 100);
        VideoScript script = script("cached", 250, "error");

        List<NarrationService.PreparedNarration> first = new NarrationService(script, List.of(provider)).prepare();
        List<NarrationService.PreparedNarration> second = new NarrationService(script, List.of(provider)).prepare();

        assertEquals(1, calls.get());
        assertEquals(1, first.size());
        assertEquals(first.get(0).path(), second.get(0).path());
        assertEquals(48_000, first.get(0).audio().sampleRate());
        assertEquals(2, first.get(0).audio().channels());
        assertEquals(100, first.get(0).durationMs());
    }

    @Test void concurrentPreparationPublishesOneValidCacheFile() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch synthesizers = new CountDownLatch(2);
        NarrationProvider delegate = fakeProvider(calls, 100);
        NarrationProvider provider = new NarrationProvider() {
            public String getId() { return "command"; }
            public String fingerprint() { return "concurrent-fake-v1"; }
            public boolean isAvailable() { return true; }
            public void synthesize(NarrationRequest request, Path outputWav) throws Exception {
                synthesizers.countDown();
                assertTrue(synthesizers.await(5, TimeUnit.SECONDS));
                delegate.synthesize(request, outputWav);
            }
        };
        VideoScript script = script("concurrent", 250, "error");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<NarrationService.PreparedNarration>> first = executor.submit(
                    () -> new NarrationService(script, List.of(provider)).prepare());
            Future<List<NarrationService.PreparedNarration>> second = executor.submit(
                    () -> new NarrationService(script, List.of(provider)).prepare());
            Path firstPath = first.get(10, TimeUnit.SECONDS).get(0).path();
            Path secondPath = second.get(10, TimeUnit.SECONDS).get(0).path();
            assertEquals(firstPath, secondPath);
            assertTrue(Files.size(firstPath) >= 44);
            assertEquals(2, calls.get(), "both contenders may synthesize, but only one cache file wins");
            try (Stream<Path> files = Files.list(firstPath.getParent())) {
                assertEquals(0, files
                        .filter(path -> path.getFileName().toString().contains(".partial-"))
                        .count());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test void overflowPolicyControlsEffectiveAudioDuration() throws Exception {
        NarrationProvider provider = fakeProvider(new AtomicInteger(), 100);
        VideoScript rejected = script("reject", 50, "error");
        assertThrows(java.io.IOException.class,
                () -> new NarrationService(rejected, List.of(provider)).prepare());

        VideoScript extended = script("extend", 50, "extend");
        List<NarrationService.PreparedNarration> narration =
                new NarrationService(extended, List.of(provider)).prepare();
        assertEquals(100, new AudioTimeline(extended, narration).durationMs());
    }

    @Test void preparesTimedNarrationCueAtActionTimestamp() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        NarrationProvider provider = fakeProvider(calls, 100);
        Path project = directory.resolve("cue");
        Files.createDirectories(project);
        Path json = project.resolve("video.json");
        Files.writeString(json, """
                {"schemaVersion":1,"id":"cue","narration":{"provider":"command"},"scenes":[
                  {"id":"intro","durationMs":500,"actions":[]},
                  {"id":"demo","durationMs":1000,"actions":[
                    {"type":"narration.cue","id":"look","atMs":250,
                     "text":"Say A. R. kit.","caption":"Say ARKit."}
                  ]}
                ]}
                """);
        List<NarrationService.PreparedNarration> prepared =
                new NarrationService(VideoScript.load(json), List.of(provider)).prepare();
        assertEquals(1, calls.get());
        assertEquals(1, prepared.size());
        assertEquals("demo:look", prepared.get(0).sceneId());
        assertEquals(750, prepared.get(0).atMs());
        assertEquals("Say ARKit.", prepared.get(0).text());
    }

    @Test void appliesPronunciationsAndPreventsCueOverlap() throws Exception {
        List<String> spoken = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        NarrationProvider provider = new NarrationProvider() {
            public String getId() { return "command"; }
            public String fingerprint() { return "capture-v1"; }
            public boolean isAvailable() { return true; }
            public void synthesize(NarrationRequest request, Path outputWav) throws Exception {
                spoken.add(request.text());
                fakeProvider(calls, 200).synthesize(request, outputWav);
            }
        };
        Path project = directory.resolve("scheduled");
        Files.createDirectories(project);
        Path json = project.resolve("video.json");
        Files.writeString(json, """
                {"schemaVersion":1,"id":"scheduled","narration":{"provider":"command",
                 "minimumGapMs":50,"pronunciations":{"Codename One":"Code name One",
                 "JIT":"jeet","AOT":"ay oh tee","clang":"C lang"}},"scenes":[
                  {"id":"demo","durationMs":1000,"actions":[
                    {"type":"narration.cue","id":"first","atMs":100,"text":"Codename One JIT AOT clang source."},
                    {"type":"narration.cue","id":"second","atMs":150,"text":"Next segment."}
                  ]}
                ]}
                """);
        List<NarrationService.PreparedNarration> prepared =
                new NarrationService(VideoScript.load(json), List.of(provider)).prepare();
        assertEquals(List.of("Code name One jeet ay oh tee C lang source.", "Next segment."), spoken);
        assertEquals(100, prepared.get(0).atMs());
        assertEquals(350, prepared.get(1).atMs());
    }

    @Test void pronunciationSubstitutionsUseLongestTokenAwareMatches() throws Exception {
        List<String> spoken = new ArrayList<>();
        NarrationProvider provider = new NarrationProvider() {
            public String getId() { return "command"; }
            public String fingerprint() { return "capture-token-aware-v1"; }
            public boolean isAvailable() { return true; }
            public void synthesize(NarrationRequest request, Path outputWav) throws Exception {
                spoken.add(request.text());
                fakeProvider(new AtomicInteger(), 100).synthesize(request, outputWav);
            }
        };
        Path project = directory.resolve("token-aware");
        Files.createDirectories(project);
        Path json = project.resolve("video.json");
        Files.writeString(json, """
                {"schemaVersion":1,"id":"token-aware","narration":{"provider":"command",
                 "pronunciations":{"AR":"A. R.","VR":"V. R.","AR/VR":"augmented reality and virtual reality",
                 "ARKit":"A. R. kit","ARCore":"A. R. core"}},"scenes":[
                  {"id":"voice","durationMs":1000,"narration":{"text":"AR/VR uses ARKit and ARCore. AR and VR remain separate."},"actions":[]}
                ]}
                """);
        new NarrationService(VideoScript.load(json), List.of(provider)).prepare();
        assertEquals(List.of("augmented reality and virtual reality uses A. R. kit and A. R. core. A. R. and V. R. remain separate."), spoken);
    }

    private VideoScript script(String id, int durationMs, String overflow) throws Exception {
        Path project = directory.resolve(id);
        Files.createDirectories(project);
        Path json = project.resolve("video.json");
        Files.writeString(json, """
                {"schemaVersion":1,"id":"%s","narration":{"provider":"command"},"scenes":[
                  {"id":"voice","durationMs":%d,"narration":{"text":"hello","overflow":"%s"},"actions":[]}
                ]}
                """.formatted(id, durationMs, overflow));
        return VideoScript.load(json);
    }

    private static NarrationProvider fakeProvider(AtomicInteger calls, int durationMs) {
        return new NarrationProvider() {
            public String getId() { return "command"; }
            public String fingerprint() { return "fake-v1"; }
            public boolean isAvailable() { return true; }
            public void synthesize(NarrationRequest request, Path outputWav) throws Exception {
                calls.incrementAndGet();
                int sampleRate = 8_000;
                int frames = sampleRate * durationMs / 1000;
                byte[] pcm = new byte[frames * 2];
                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                try (AudioInputStream input = new AudioInputStream(new ByteArrayInputStream(pcm), format, frames)) {
                    AudioSystem.write(input, AudioFileFormat.Type.WAVE, outputWav.toFile());
                }
            }
        };
    }
}
