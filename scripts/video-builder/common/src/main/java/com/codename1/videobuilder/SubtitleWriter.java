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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Writes the original narration transcript with measured audio timing. */
public final class SubtitleWriter {
    private SubtitleWriter() { }

    public static List<SubtitleFile> write(Path directory, String id,
                                           List<NarrationService.PreparedNarration> narration) throws IOException {
        if (narration.isEmpty()) return List.of();
        List<NarrationService.PreparedNarration> cues = new ArrayList<>(narration);
        cues.sort(Comparator.comparingLong(NarrationService.PreparedNarration::atMs));
        Path srt = directory.resolve(id + ".srt");
        Path vtt = directory.resolve(id + ".vtt");
        StringBuilder srtText = new StringBuilder();
        StringBuilder vttText = new StringBuilder("WEBVTT\n\n");
        for (int i = 0; i < cues.size(); i++) {
            NarrationService.PreparedNarration cue = cues.get(i);
            long end = cue.atMs() + cue.durationMs();
            String transcript = cue.text().replaceAll("\\s+", " ").trim();
            srtText.append(i + 1).append('\n')
                    .append(timestamp(cue.atMs(), ',')).append(" --> ").append(timestamp(end, ',')).append('\n')
                    .append(transcript).append("\n\n");
            vttText.append(timestamp(cue.atMs(), '.')).append(" --> ").append(timestamp(end, '.')).append('\n')
                    .append(transcript).append("\n\n");
        }
        Files.writeString(srt, srtText.toString(), StandardCharsets.UTF_8);
        Files.writeString(vtt, vttText.toString(), StandardCharsets.UTF_8);
        return List.of(new SubtitleFile("srt", srt), new SubtitleFile("vtt", vtt));
    }

    static String timestamp(long milliseconds, char separator) {
        long hours = milliseconds / 3_600_000;
        long minutes = milliseconds / 60_000 % 60;
        long seconds = milliseconds / 1000 % 60;
        long millis = milliseconds % 1000;
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d%c%03d",
                hours, minutes, seconds, separator, millis);
    }

    public record SubtitleFile(String format, Path path) { }
}
