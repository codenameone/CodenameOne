/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.playback;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.domain.Subtitle;
import com.codenameone.examples.reel.repository.SubtitleRepository;

import java.io.IOException;

/// Stores a video's subtitle tracks, one per language.
///
/// A track is checked only as far as the player depends on it: a WebVTT file
/// starts with `WEBVTT`, and it is small enough to keep in a row. The cues
/// themselves are the creator's; the app shows what is between the times.
@Component
public class Subtitles {
    /// The largest track kept, in characters. A two-hour film's subtitles are
    /// around 100 KB.
    public static final int MAX_LENGTH = 512 * 1024;

    /// The most tracks a video may have.
    public static final int MAX_TRACKS = 40;

    private final SubtitleRepository subtitles;

    public Subtitles(SubtitleRepository subtitles) {
        this.subtitles = subtitles;
    }

    /// Whether `language` is a language tag the file names can carry: two or
    /// three letters, then optional `-` and letters or digits (`en`, `pt-BR`).
    public static boolean validLanguage(String language) {
        if (language == null || language.length() < 2 || language.length() > 16) {
            return false;
        }
        int letters = 0;
        while (letters < language.length() && isLetter(language.charAt(letters))) {
            letters++;
        }
        if (letters < 2 || letters > 3) {
            return false;
        }
        for (int iter = letters; iter < language.length(); iter++) {
            char c = language.charAt(iter);
            boolean dash = c == '-';
            if (dash && (iter == language.length() - 1 || language.charAt(iter - 1) == '-')) {
                return false;
            }
            if (!dash && !isLetter(c) && !(c >= '0' && c <= '9')) {
                return false;
            }
            if (iter == letters && !dash) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    /// Adds the track in `language`, or replaces the one already there.
    public void put(String videoId, String language, String label, String vtt, long now) throws IOException {
        if (!validLanguage(language)) {
            throw new ResponseStatusException(400, "A language is a tag such as en or pt-BR");
        }
        String name = label == null ? "" : label.trim();
        if (name.length() == 0 || name.length() > 64) {
            throw new ResponseStatusException(400, "Name the track (at most 64 characters)");
        }
        String text = vtt == null ? "" : vtt;
        if (text.length() > 0 && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        if (!text.startsWith("WEBVTT")) {
            throw new ResponseStatusException(400, "A subtitle track is a WebVTT file, which starts with WEBVTT");
        }
        if (text.length() > MAX_LENGTH) {
            throw new ResponseStatusException(413, "A subtitle track is at most " + (MAX_LENGTH / 1024) + " KB");
        }
        Subtitle row = subtitles.findByVideoIdAndLanguage(videoId, language);
        if (row == null) {
            if (subtitles.findByVideoIdOrderByLanguageAsc(videoId).size() >= MAX_TRACKS) {
                throw new ResponseStatusException(400, "A video has at most " + MAX_TRACKS + " subtitle tracks");
            }
            row = new Subtitle();
            row.setId(Ids.next());
            row.setVideoId(videoId);
            row.setLanguage(language);
        }
        row.setLabel(name);
        row.setVtt(text);
        row.setCreatedAt(now);
        subtitles.save(row);
    }

    public void remove(String videoId, String language) throws IOException {
        subtitles.deleteByVideoIdAndLanguage(videoId, language);
    }

    public void removeAll(String videoId) throws IOException {
        subtitles.deleteByVideoId(videoId);
    }
}
