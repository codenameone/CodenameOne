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
package com.codenameone.examples.reel.player;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.CN;
import com.codenameone.examples.reel.ui.Art;

import java.util.ArrayList;
import java.util.List;

/// A subtitle track, read from WebVTT and drawn by the app over the picture.
///
/// Drawing them here, and not asking the platform player for a text track,
/// keeps subtitles the same on every device and every DRM scheme: a track is a
/// file beside the stream, never something the player has to find inside an
/// encrypted one. Only what the cues say is used -- the times and the text;
/// positioning and styling settings are ignored, and the tags a cue may carry
/// (`<i>`, `<c.yellow>`, `<v Speaker>`) are dropped from the text.
public final class Captions {
    /// Called with the parsed track, or null when it could not be fetched.
    public interface Loaded {
        void loaded(Captions captions);
    }

    private final List<int[]> times = new ArrayList<int[]>();
    private final List<String> texts = new ArrayList<String>();

    private Captions() {
    }

    /// Reads `vtt`. A file that is not WebVTT parses to no cues.
    public static Captions parse(String vtt) {
        Captions out = new Captions();
        if (vtt == null) {
            return out;
        }
        String text = vtt.length() > 0 && vtt.charAt(0) == '\uFEFF' ? vtt.substring(1) : vtt;
        if (!text.startsWith("WEBVTT")) {
            return out;
        }
        List<String> lines = lines(text);
        int iter = 0;
        while (iter < lines.size()) {
            String line = lines.get(iter);
            int arrow = line.indexOf("-->");
            if (arrow < 0) {
                iter++;
                continue;
            }
            int start = time(line.substring(0, arrow).trim());
            String rest = line.substring(arrow + 3).trim();
            int space = rest.indexOf(' ');
            int end = time(space < 0 ? rest : rest.substring(0, space));
            iter++;
            StringBuilder cue = new StringBuilder();
            while (iter < lines.size() && lines.get(iter).trim().length() > 0) {
                if (cue.length() > 0) {
                    cue.append('\n');
                }
                cue.append(stripTags(lines.get(iter)));
                iter++;
            }
            if (start >= 0 && end > start && cue.length() > 0) {
                out.times.add(new int[] {start, end});
                out.texts.add(cue.toString());
            }
        }
        return out;
    }

    /// Fetches the track at `url` (a server path) and parses it, on the EDT.
    public static void load(String url, final Loaded loaded) {
        final ConnectionRequest r = new ConnectionRequest(Art.url(url), false) {
            @Override
            protected void handleErrorResponseCode(int code, String message) {
                CN.callSerially(() -> loaded.loaded(null));
            }

            @Override
            protected void handleException(Exception err) {
                CN.callSerially(() -> loaded.loaded(null));
            }
        };
        r.setReadResponseForErrors(false);
        r.addResponseListener(e -> {
            byte[] data = r.getResponseData();
            Captions c;
            try {
                c = parse(data == null ? null : new String(data, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException never) {
                c = null;
            }
            loaded.loaded(c);
        });
        NetworkManager.getInstance().addToQueue(r);
    }

    /// The number of cues.
    public int size() {
        return texts.size();
    }

    /// What shows at `millis`: the cues that cover it, one per line, or "".
    public String at(int millis) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < times.size(); iter++) {
            int[] t = times.get(iter);
            if (millis >= t[0] && millis < t[1]) {
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append(texts.get(iter));
            }
        }
        return out.toString();
    }

    /// `hh:mm:ss.ttt` or `mm:ss.ttt` in milliseconds, or -1.
    static int time(String text) {
        int total = 0;
        int part = 0;
        int millis = 0;
        int digits = 0;
        boolean fraction = false;
        int fractionDigits = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c >= '0' && c <= '9') {
                if (fraction) {
                    if (fractionDigits < 3) {
                        millis = millis * 10 + (c - '0');
                        fractionDigits++;
                    }
                } else {
                    part = part * 10 + (c - '0');
                    digits++;
                }
            } else if (c == ':' && !fraction && digits > 0) {
                total = total * 60 + part;
                part = 0;
                digits = 0;
            } else if ((c == '.' || c == ',') && !fraction && digits > 0) {
                fraction = true;
            } else {
                return -1;
            }
        }
        if (digits == 0) {
            return -1;
        }
        while (fractionDigits < 3) {
            millis *= 10;
            fractionDigits++;
        }
        return (total * 60 + part) * 1000 + millis;
    }

    private static List<String> lines(String text) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            if (text.charAt(iter) == '\n') {
                int end = iter > from && text.charAt(iter - 1) == '\r' ? iter - 1 : iter;
                out.add(text.substring(from, end));
                from = iter + 1;
            }
        }
        if (from < text.length()) {
            out.add(text.substring(from));
        }
        return out;
    }

    static String stripTags(String line) {
        StringBuilder out = new StringBuilder(line.length());
        boolean inTag = false;
        for (int iter = 0; iter < line.length(); iter++) {
            char c = line.charAt(iter);
            if (c == '<') {
                inTag = true;
            } else if (c == '>' && inTag) {
                inTag = false;
            } else if (!inTag) {
                out.append(c);
            }
        }
        return decode(out.toString());
    }

    private static String decode(String text) {
        if (text.indexOf('&') < 0) {
            return text;
        }
        String[][] entities = {{"&amp;", "&"}, {"&lt;", "<"}, {"&gt;", ">"}, {"&nbsp;", " "}, {"&quot;", "\""}};
        String out = text;
        for (int iter = 0; iter < entities.length; iter++) {
            out = com.codename1.util.StringUtil.replaceAll(out, entities[iter][0], entities[iter][1]);
        }
        return out;
    }
}
