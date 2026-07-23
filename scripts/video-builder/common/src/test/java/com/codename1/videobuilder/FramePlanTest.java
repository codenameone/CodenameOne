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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.codename1.ui.Component;
import com.codename1.ui.Label;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FramePlanTest {
    @TempDir Path directory;

    @Test void staticHoldsReuseRenderedImages() throws Exception {
        VideoScript script = script("""
                {"schemaVersion":1,"id":"sparse","output":{"frameRate":30},"scenes":[
                  {"id":"slide","durationMs":10000,"actions":[
                    {"type":"intro.show","id":"intro","atMs":0,"durationMs":1000},
                    {"type":"text.show","id":"late","atMs":3000,"text":"Late"}
                  ]}
                ]}
                """);
        FramePlan.Plan plan = FramePlan.create(script, 10_000);
        assertEquals(300, plan.frameCount());
        assertTrue(plan.frames().size() < 40, "only animated frames and event boundaries are rendered");
        assertEquals(0, plan.frames().get(0).frameIndex());
        assertEquals(299, plan.frames().get(plan.frames().size() - 1).frameIndex());
    }

    @Test void aMountedDemoRemainsFrameAccurate() throws Exception {
        VideoScript script = script("""
                {"schemaVersion":1,"id":"demo","output":{"frameRate":30},"scenes":[
                  {"id":"live","durationMs":2000,"actions":[
                    {"type":"demo.mount","id":"app","atMs":0,"class":"com.codename1.videobuilder.FramePlanTest$DummyDemo"}
                  ]}
                ]}
                """);
        assertEquals(60, FramePlan.create(script, 2_000).frames().size());
    }

    @Test void discreteDemoReusesFramesBetweenActions() throws Exception {
        VideoScript script = script("""
                {"schemaVersion":1,"id":"discrete-demo","output":{"frameRate":30},"scenes":[
                  {"id":"live","durationMs":4000,"actions":[
                    {"type":"demo.mount","id":"app","atMs":0,"animated":false,
                     "class":"com.codename1.videobuilder.FramePlanTest$DummyDemo"},
                    {"type":"demo.action","atMs":1000,"name":"first","arguments":{}},
                    {"type":"demo.action","atMs":2500,"name":"second","arguments":{}}
                  ]}
                ]}
                """);
        FramePlan.Plan plan = FramePlan.create(script, 4_000);
        assertTrue(plan.frames().size() <= 8,
                "only mount, actions, and the last frame are rendered; got " + plan.frames().size());
        assertTrue(plan.frames().stream().anyMatch(frame -> frame.frameIndex() == 30));
        assertTrue(plan.frames().stream().anyMatch(frame -> frame.frameIndex() == 75));
    }

    @Test void layerAnimationRendersEveryFrameOnlyWhileMoving() throws Exception {
        VideoScript script = script("""
                {"schemaVersion":1,"id":"motion","output":{"frameRate":30},"scenes":[
                  {"id":"live","durationMs":4000,"actions":[
                    {"type":"text.show","id":"visual","atMs":0,"text":"AR view"},
                    {"type":"layer.animate","target":"visual","atMs":1000,"durationMs":1000,
                     "fromX":0,"toX":0.1,"fromScale":1,"toScale":1.08}
                  ]}
                ]}
                """);
        FramePlan.Plan plan = FramePlan.create(script, 4_000);
        assertTrue(plan.frames().size() >= 31);
        assertTrue(plan.frames().size() < 40, "static holds around the motion reuse frames");
    }

    @Test void replayUsesItsDeclaredSamplingCadence() throws Exception {
        VideoScript script = script("""
                {"schemaVersion":1,"id":"sampled-replay","output":{"frameRate":30},"scenes":[
                  {"id":"proof","durationMs":4000,"actions":[
                    {"type":"text.show","id":"visual","atMs":0,"text":"proof"},
                    {"type":"replay","atMs":1000,"fromMs":0,"toMs":500,
                     "rewindDurationMs":500,"rewindFps":8,"replayFps":10,"playbackRate":0.5}
                  ]}
                ]}
                """);
        FramePlan.Plan plan = FramePlan.create(script, 4_000);
        assertEquals(120, plan.frameCount());
        assertTrue(plan.frames().size() >= 15, "declared replay samples are retained");
        assertTrue(plan.frames().size() < 25,
                "repeated output frames should link to sampled replay images; got " + plan.frames().size());
    }

    @Test void videotoolboxCommandRequiresHardwareAndSpeedPriority() {
        List<String> command = StagedVideoEncoder.command(Path.of("ffmpeg"),
                new StagedVideoEncoder.Encoder("h264_videotoolbox", true), directory,
                directory.resolve("audio.raw"), directory.resolve("out.mp4"), 30, 8_000_000, 5_000);
        assertTrue(command.contains("h264_videotoolbox"));
        assertTrue(command.contains("-allow_sw"));
        assertTrue(command.contains("-prio_speed"));
        assertTrue(command.contains("-power_efficient"));
        assertFalse(command.contains("-hwaccel"), "hardware decode flags do not accelerate PNG encoding");
    }

    private VideoScript script(String json) throws Exception {
        Path source = directory.resolve("video-" + System.nanoTime() + ".json");
        Files.writeString(source, json);
        return VideoScript.load(source);
    }

    public static final class DummyDemo implements DemoScene {
        public Component create(DemoContext context) { return new Label("demo"); }
        public void onAction(String name, Map<String, Object> arguments) { }
        public void reset() { }
        public void dispose() { }
    }
}
