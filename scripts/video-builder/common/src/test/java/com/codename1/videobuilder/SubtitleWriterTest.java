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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubtitleWriterTest {
    @TempDir Path directory;

    @Test void writesMeasuredSrtAndWebVttCuesInTimelineOrder() throws Exception {
        NarrationService.PreparedNarration second = cue("second", "Second cue.", 2500, 1250);
        NarrationService.PreparedNarration first = cue("first", "First\n cue.", 100, 900);
        List<SubtitleWriter.SubtitleFile> files = SubtitleWriter.write(
                directory, "demo", List.of(second, first));

        assertEquals(2, files.size());
        String srt = Files.readString(directory.resolve("demo.srt"));
        String vtt = Files.readString(directory.resolve("demo.vtt"));
        assertTrue(srt.startsWith("1\n00:00:00,100 --> 00:00:01,000\nFirst cue."));
        assertTrue(srt.contains("2\n00:00:02,500 --> 00:00:03,750\nSecond cue."));
        assertTrue(vtt.startsWith("WEBVTT\n\n00:00:00.100 --> 00:00:01.000"));
    }

    private NarrationService.PreparedNarration cue(String id, String text, long atMs, long durationMs) {
        return new NarrationService.PreparedNarration(id, text, atMs,
                directory.resolve(id + ".wav"), null, 1f, durationMs);
    }
}
