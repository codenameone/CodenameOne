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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.api.AudioTrackDto;
import com.codenameone.examples.reel.api.SubtitleDto;
import com.codenameone.examples.reel.domain.Subtitle;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.SubtitleRepository;

import java.util.ArrayList;
import java.util.List;

/// A video's subtitle and audio tracks, and the qualities it can be watched in.
///
/// Subtitles are WebVTT files the app draws itself over the picture, so they
/// work the same with every player and every DRM scheme; the player is never
/// asked to find a text track inside an encrypted stream. Audio is chosen by
/// the server: a request names a track, and the master playlist the player
/// fetches makes that rendition the default (see [MediaOrigin]). Changing
/// either while watching costs the player a reopen at the same position, not
/// a track API every platform would have to provide.
@Component
public class Tracks {
    /// The audio a video has when nothing says otherwise.
    public static final String ORIGINAL = "main:en:English";

    private final SubtitleRepository subtitles;

    public Tracks(SubtitleRepository subtitles) {
        this.subtitles = subtitles;
    }

    /// The subtitle tracks of `videoId`, each with a URL under `base`, the
    /// session's tokened media path.
    public List<SubtitleDto> subtitles(String videoId, String base) {
        List<Subtitle> rows = subtitles.findByVideoIdOrderByLanguageAsc(videoId);
        List<SubtitleDto> out = new ArrayList<SubtitleDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Subtitle s = rows.get(iter);
            out.add(new SubtitleDto(s.getLanguage(), s.getLabel(),
                    base == null ? null : base + "sub_" + s.getLanguage() + ".vtt"));
        }
        return out;
    }

    /// What the subtitle tracks are called, for a title's page.
    public List<String> subtitleLabels(String videoId) {
        List<SubtitleDto> all = subtitles(videoId, null);
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(all.get(iter).label());
        }
        return out;
    }

    /// The audio tracks of `video`, the original first.
    public static List<AudioTrackDto> audio(Video video) {
        String raw = video == null || video.getAudio() == null || video.getAudio().length() == 0 ? ORIGINAL
                : video.getAudio();
        List<String> entries = Rows.split(raw);
        List<AudioTrackDto> out = new ArrayList<AudioTrackDto>();
        for (int iter = 0; iter < entries.size(); iter++) {
            List<String> parts = Rows.split(entries.get(iter), ':');
            if (parts.size() == 3) {
                out.add(new AudioTrackDto(parts.get(0), parts.get(1), parts.get(2)));
            }
        }
        if (out.isEmpty()) {
            out.add(new AudioTrackDto("main", "en", "English"));
        }
        return out;
    }

    /// What the audio tracks are called, for a title's page.
    public static List<String> audioLabels(Video video) {
        List<AudioTrackDto> all = audio(video);
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(all.get(iter).label());
        }
        return out;
    }

    /// `wanted` when the video has that track, else its original.
    public static String audioTrack(Video video, String wanted) {
        List<AudioTrackDto> all = audio(video);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(wanted)) {
                return wanted;
            }
        }
        return all.get(0).id();
    }

    /// The heights of the renditions a viewer capped at `maxHeight` can pick,
    /// tallest first.
    public static List<Integer> qualities(String renditions, int maxHeight) {
        List<String> all = Rows.split(renditions);
        List<Integer> out = new ArrayList<Integer>();
        for (int iter = 0; iter < all.size(); iter++) {
            int h = parse(all.get(iter));
            if (h > 0 && h <= maxHeight && !out.contains(Integer.valueOf(h))) {
                int at = 0;
                while (at < out.size() && out.get(at).intValue() > h) {
                    at++;
                }
                out.add(at, Integer.valueOf(h));
            }
        }
        return out;
    }

    /// The tallest rendition: what a title's page calls HD or 4K.
    public static int tallest(String renditions) {
        List<Integer> all = qualities(renditions, Integer.MAX_VALUE);
        return all.isEmpty() ? 0 : all.get(0).intValue();
    }

    private static int parse(String text) {
        int value = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return 0;
            }
            value = value * 10 + (c - '0');
            if (value > 10000) {
                return 0;
            }
        }
        return value;
    }
}
