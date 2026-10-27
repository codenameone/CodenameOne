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
package com.codenameone.examples.reel;

/// The captions of the generated academy lessons, in four languages: the
/// three lines each lesson shows on screen, one every fifteen seconds (see
/// the packager's Seed.LESSONS, which draws them). Kept as text rather than
/// as files so a fresh checkout and the tests have them with no media.
final class DemoSubtitles {
    /// Language tag and what the player's menu calls it.
    static final String[][] LANGUAGES = {
        {"en", "English"},
        {"es", "Espa\u00f1ol"},
        {"fr", "Fran\u00e7ais"},
        {"de", "Deutsch"},
    };

    /// Per language of [#LANGUAGES], per lesson, the three lines.
    static final String[][][] LINES = {
        {
            {"Welcome to Reel Academy.", "Streams are cut into small segments.", "Each segment is encrypted."},
            {"Keys and licences.", "The player asks the server for a key.", "Only entitled viewers get one."},
            {"Watching offline.", "Downloads keep their encryption.", "Licences expire on schedule."},
        },
        {
            {"Bienvenido a Reel Academy.", "Las emisiones se cortan en segmentos peque\u00f1os.", "Cada segmento est\u00e1 cifrado."},
            {"Claves y licencias.", "El reproductor pide una clave al servidor.", "Solo los espectadores con derecho la reciben."},
            {"Ver sin conexi\u00f3n.", "Las descargas conservan su cifrado.", "Las licencias caducan seg\u00fan lo previsto."},
        },
        {
            {"Bienvenue \u00e0 Reel Academy.", "Les flux sont d\u00e9coup\u00e9s en petits segments.", "Chaque segment est chiffr\u00e9."},
            {"Cl\u00e9s et licences.", "Le lecteur demande une cl\u00e9 au serveur.", "Seuls les spectateurs autoris\u00e9s en re\u00e7oivent une."},
            {"Regarder hors ligne.", "Les t\u00e9l\u00e9chargements restent chiffr\u00e9s.", "Les licences expirent comme pr\u00e9vu."},
        },
        {
            {"Willkommen bei der Reel Academy.", "Streams werden in kleine Segmente zerlegt.", "Jedes Segment ist verschl\u00fcsselt."},
            {"Schl\u00fcssel und Lizenzen.", "Der Player fragt den Server nach einem Schl\u00fcssel.", "Nur berechtigte Zuschauer erhalten einen."},
            {"Offline ansehen.", "Downloads bleiben verschl\u00fcsselt.", "Lizenzen laufen planm\u00e4\u00dfig ab."},
        },
    };

    private DemoSubtitles() {
    }

    /// The WebVTT track of lesson `lesson` (from 0) in language `language`
    /// (an index of [#LANGUAGES]).
    static String vtt(int language, int lesson) {
        String[] lines = LINES[language][lesson];
        StringBuilder out = new StringBuilder("WEBVTT\n\n");
        for (int iter = 0; iter < lines.length; iter++) {
            int from = iter * 15 + 1;
            out.append(iter + 1).append('\n').append(time(from)).append(" --> ").append(time(from + 13)).append('\n')
                    .append(lines[iter]).append("\n\n");
        }
        return out.toString();
    }

    private static String time(int seconds) {
        return "00:00:" + (seconds < 10 ? "0" : "") + seconds + ".000";
    }
}
