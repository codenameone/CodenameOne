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
package com.codename1.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A complete transcription: plain text plus the timed segments needed
/// for captions.
public final class TranscriptionResult {
    private final List<TranscriptionSegment> segments;
    private final String text;

    /// Creates a transcription result from timed segments.
    ///
    /// The supplied list is copied, validated, and exposed through [#getSegments()]
    /// as an immutable list. The plain text returned by [#getText()] is the
    /// concatenation of each segment's text in order.
    ///
    /// @param segments ordered transcript segments
    /// @throws IllegalArgumentException if `segments` is `null` or contains `null`
    public TranscriptionResult(List<TranscriptionSegment> segments) {
        if (segments == null) {
            throw new IllegalArgumentException("segments is required");
        }
        ArrayList<TranscriptionSegment> copy = new ArrayList<TranscriptionSegment>(segments.size());
        StringBuilder sb = new StringBuilder();
        for (TranscriptionSegment s : segments) {
            if (s == null) {
                throw new IllegalArgumentException("segments must not contain null");
            }
            copy.add(s);
            sb.append(s.getText());
        }
        this.segments = Collections.unmodifiableList(copy);
        this.text = sb.toString();
    }

    /// Creates a text-only result for providers that do not expose segment timing.
    ///
    /// The returned result contains one segment from `0` to `0` milliseconds.
    ///
    /// @param text recognized text
    /// @return a transcription result containing one untimed segment
    public static TranscriptionResult textOnly(String text) {
        ArrayList<TranscriptionSegment> segments = new ArrayList<TranscriptionSegment>(1);
        segments.add(new TranscriptionSegment(0, 0, text));
        return new TranscriptionResult(segments);
    }

    /// Gets the ordered timed transcript segments.
    ///
    /// @return immutable segment list
    public List<TranscriptionSegment> getSegments() {
        return segments;
    }

    /// Gets the plain transcript text.
    ///
    /// @return transcript text, never `null`
    public String getText() {
        return text;
    }

    /// Formats this transcription as SubRip captions.
    ///
    /// @return SRT text using millisecond timestamps
    public String toSrt() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            TranscriptionSegment s = segments.get(i);
            out.append(i + 1).append('\n');
            out.append(formatTime(s.getStartTimeMs(), ',')).append(" --> ")
                    .append(formatTime(s.getEndTimeMs(), ',')).append('\n');
            out.append(normalizeCaptionText(s.getText())).append("\n\n");
        }
        return out.toString();
    }

    /// Formats this transcription as WebVTT captions.
    ///
    /// @return WebVTT text using millisecond timestamps
    public String toVtt() {
        StringBuilder out = new StringBuilder("WEBVTT\n\n");
        for (TranscriptionSegment s : segments) {
            out.append(formatTime(s.getStartTimeMs(), '.')).append(" --> ")
                    .append(formatTime(s.getEndTimeMs(), '.')).append('\n');
            out.append(normalizeCaptionText(s.getText()).replace("-->", "-- >")).append("\n\n");
        }
        return out.toString();
    }

    private static String normalizeCaptionText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', '\n').trim();
    }

    private static String formatTime(long millis, char decimalSeparator) {
        long hours = millis / 3600000L;
        millis %= 3600000L;
        long minutes = millis / 60000L;
        millis %= 60000L;
        long seconds = millis / 1000L;
        long ms = millis % 1000L;
        return two(hours) + ":" + two(minutes) + ":" + two(seconds)
                + decimalSeparator + three(ms);
    }

    private static String two(long value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static String three(long value) {
        if (value < 10) {
            return "00" + value;
        }
        if (value < 100) {
            return "0" + value;
        }
        return String.valueOf(value);
    }
}
