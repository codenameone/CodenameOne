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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VideoScriptTest {
    @TempDir Path directory;

    @Test void loadsDefaultsAndTimeline() throws Exception {
        Files.writeString(directory.resolve("Demo.java"), "class Demo {}");
        Path scriptPath = write("""
                {"schemaVersion":1,"id":"demo","title":"Demo","scenes":[
                  {"id":"one","durationMs":1000,"actions":[
                    {"type":"code.show","id":"code","path":"Demo.java"},
                    {"type":"code.type","target":"code","atMs":100,"durationMs":500,"text":"hello"}
                  ]},
                  {"id":"two","durationMs":750,"actions":[{"type":"text.show","text":"done"}]}
                ]}
                """);
        VideoScript script = VideoScript.load(scriptPath);
        assertEquals("demo", script.getId());
        assertEquals(1750, script.getDurationMs());
        assertEquals(30, script.getOutput().frameRate());
        assertEquals(1920, script.getOutput().landscapeWidth());
        assertEquals("staged", script.getOutput().encodingPipeline());
        assertEquals(2, script.getScenes().size());
    }

    @Test void loadsVideoIOEncodingPipelineAndRejectsUnknownPipeline() throws Exception {
        Path scriptPath = write("""
                {"schemaVersion":1,"id":"videoio","output":{"encodingPipeline":"videoio"},
                 "scenes":[{"id":"one","durationMs":1000,"actions":[]}]}
                """);
        assertEquals("videoio", VideoScript.load(scriptPath).getOutput().encodingPipeline());

        scriptPath = write("""
                {"schemaVersion":1,"id":"bad-pipeline","output":{"encodingPipeline":"magic"},
                 "scenes":[{"id":"one","durationMs":1000,"actions":[]}]}
                """);
        Path invalid = scriptPath;
        VideoScript.ScriptException error = assertThrows(VideoScript.ScriptException.class,
                () -> VideoScript.load(invalid));
        assertTrue(error.getMessage().contains("encodingPipeline"));
    }

    @Test void rejectsUnknownActionAndOutOfRangeTiming() throws Exception {
        Path scriptPath = write("""
                {"schemaVersion":1,"id":"bad","scenes":[
                  {"id":"one","durationMs":100,"actions":[{"type":"magic","atMs":101}]}
                ]}
                """);
        VideoScript.ScriptException error = assertThrows(VideoScript.ScriptException.class,
                () -> VideoScript.load(scriptPath));
        assertTrue(error.getMessage().contains("unknown action"));
        assertTrue(error.getMessage().contains("outside the scene"));
    }

    @Test void rejectsEscapingAndMissingPaths() throws Exception {
        Path scriptPath = write("""
                {"schemaVersion":1,"id":"bad-path","audio":[{"path":"../outside.wav"}],"scenes":[
                  {"id":"one","durationMs":100,"actions":[{"type":"code.show","path":"missing.java"}]}
                ]}
                """);
        VideoScript.ScriptException error = assertThrows(VideoScript.ScriptException.class,
                () -> VideoScript.load(scriptPath));
        assertTrue(error.getMessage().contains("escapes project directory"));
        assertTrue(error.getMessage().contains("file not found"));
    }

    @Test void loadsCompositionFocusPointerNarrationAndReplay() throws Exception {
        Path scriptPath = write("""
                {"schemaVersion":1,"id":"rich","scenes":[
                  {"id":"one","durationMs":5000,
                   "composition":{"portrait":"code-over-demo","landscape":"code-left-demo-right"},
                   "actions":[
                    {"type":"text.show","id":"code","role":"code","text":"source"},
                    {"type":"focus.show","id":"focus","target":"code","atMs":100,"relativeBounds":{"x":0.1,"y":0.2,"width":0.8,"height":0.2}},
                    {"type":"pointer.show","id":"finger","area":"code","atMs":200,"style":"touch","x":0.5,"y":0.5},
                    {"type":"pointer.click","target":"finger","atMs":300,"durationMs":300},
                    {"type":"layer.animate","target":"code","atMs":350,"durationMs":400,"toScale":1.05},
                    {"type":"narration.cue","id":"explain","atMs":400,"text":"Say A. R. kit.","caption":"Say ARKit."},
                    {"type":"replay","atMs":3000,"fromMs":200,"toMs":1000,"label":"One more time"}
                  ]}
                ]}
                """);
        VideoScript script = VideoScript.load(scriptPath);
        assertEquals("code-over-demo", script.getScenes().get(0).composition().get("portrait"));
        assertEquals(7, script.getScenes().get(0).actions().size());
    }

    @Test void rejectsUnavailableFocusTarget() throws Exception {
        Path scriptPath = write("""
                {"schemaVersion":1,"id":"bad-focus","scenes":[
                  {"id":"one","durationMs":1000,"actions":[
                    {"type":"focus.show","target":"missing","atMs":10}
                  ]}
                ]}
                """);
        VideoScript.ScriptException error = assertThrows(VideoScript.ScriptException.class,
                () -> VideoScript.load(scriptPath));
        assertTrue(error.getMessage().contains("unavailable layer 'missing'"));
    }

    @Test void rejectsInvalidReplayWindow() throws Exception {
        Path scriptPath = write("""
                {"schemaVersion":1,"id":"bad-replay","scenes":[
                  {"id":"one","durationMs":1000,"actions":[
                    {"type":"replay","atMs":600,"fromMs":500,"toMs":800}
                  ]}
                ]}
                """);
        VideoScript.ScriptException error = assertThrows(VideoScript.ScriptException.class,
                () -> VideoScript.load(scriptPath));
        assertTrue(error.getMessage().contains("0 <= fromMs < toMs <= atMs"));
    }

    @Test void loadsPresentationActionsAndReplayTiming() throws Exception {
        Files.writeString(directory.resolve("idea.svg"), "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");
        Files.writeString(directory.resolve("idea-portrait.svg"), "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");
        Files.writeString(directory.resolve("logo.png"), "logo");
        Path scriptPath = write("""
                {"schemaVersion":1,"id":"presentation","narration":{"pronunciations":{"Codename One":"Code name One"},"minimumGapMs":250},"scenes":[
                  {"id":"slides","durationMs":7000,"actions":[
                    {"type":"intro.show","id":"welcome","durationMs":500,"path":"logo.png","title":"Welcome","subtitle":"A short intro"},
                    {"type":"outro.show","id":"outro","atMs":5500,"durationMs":1000,"eyebrow":"NEXT","title":"Keep going","subtitle":"One useful next step","prompt":"Comment with the next topic"},
                    {"type":"bullets.show","id":"points","durationMs":1200,"title":"Why","items":["One","Two"]},
                    {"type":"diagram.show","id":"flow","atMs":1500,"durationMs":1000,"text":"flowchart LR\\nA[Script] --> B[Video]"},
                    {"type":"svg.show","id":"art","atMs":3000,"paths":{"landscape":"idea.svg","portrait":"idea-portrait.svg"}},
                    {"type":"transition","target":"art","atMs":3200,"durationMs":300,"effect":"morph","easing":"spring"},
                    {"type":"replay","atMs":5000,"fromMs":1000,"toMs":2000,"rewindDurationMs":500,"playbackRate":1.0}
                  ]}
                ]}
                """);
        VideoScript script = VideoScript.load(scriptPath);
        assertEquals("Code name One", script.getNarration().pronunciations().get("Codename One"));
        assertEquals(250, script.getNarration().minimumGapMs());
        assertEquals(7, script.getScenes().get(0).actions().size());
    }

    @Test void commandParserPreservesAutomationOptions() {
        VideoBuilderCommand command = VideoBuilderCommand.parse(new String[]{"render", "video.json", "--orientation", "both", "--output", "build/video"});
        assertEquals("render", command.name());
        assertEquals("both", command.orientation());
        assertEquals(Path.of("build/video"), command.output());
    }

    private Path write(String json) throws Exception {
        Path output = directory.resolve("video.json");
        Files.writeString(output, json);
        return output;
    }
}
