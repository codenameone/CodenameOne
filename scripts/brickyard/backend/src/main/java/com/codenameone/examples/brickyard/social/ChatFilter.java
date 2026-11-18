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
package com.codenameone.examples.brickyard.social;

/// The server-side chat filter. A line is stored as written and filtered for
/// each reader, so the strictness is the reader's: under 13 is strict (no
/// digits at all, no links, no contact words), 13 to 17 hides contact
/// details (links, e-mail addresses, phone-number-like digit runs), and an
/// adult sees everything but profanity. Matching is ASCII case-insensitive
/// and never folds with the device locale.
public final class ChatFilter {
    public static final int STRICT_UNDER = 13;
    public static final int TEEN_UNDER = 18;

    private static final String[] PROFANITY = {"fuck", "shit", "bitch", "bastard", "cunt", "dick", "asshole"};
    private static final String[] CONTACT_WORDS = {"snapchat", "snap", "insta", "instagram", "discord",
        "whatsapp", "telegram", "phone", "address", "email"};
    private static final String[] LINK_MARKS = {"http", "www.", ".com", ".net", ".org", ".gg", ".io"};

    private ChatFilter() {
    }

    public static String filter(String text, int readerAge) {
        StringBuilder out = new StringBuilder(text);
        mask(out, PROFANITY);
        if (readerAge < TEEN_UNDER) {
            maskTokensContaining(out, LINK_MARKS);
            maskTokensContaining(out, new String[] {"@"});
            maskDigitRuns(out, readerAge < STRICT_UNDER ? 1 : 7);
        }
        if (readerAge < STRICT_UNDER) {
            mask(out, CONTACT_WORDS);
        }
        return out.toString();
    }

    private static void mask(StringBuilder s, String[] words) {
        String text = s.toString();
        for (String w : words) {
            for (int i = 0; i + w.length() <= text.length(); i++) {
                if (text.regionMatches(true, i, w, 0, w.length())) {
                    for (int j = i; j < i + w.length(); j++) {
                        s.setCharAt(j, '#');
                    }
                }
            }
        }
    }

    private static void maskTokensContaining(StringBuilder s, String[] marks) {
        String text = s.toString();
        int start = 0;
        while (start < text.length()) {
            int end = start;
            while (end < text.length() && text.charAt(end) != ' ') {
                end++;
            }
            boolean hit = false;
            for (String m : marks) {
                for (int i = start; i + m.length() <= end && !hit; i++) {
                    hit = text.regionMatches(true, i, m, 0, m.length());
                }
            }
            if (hit) {
                for (int j = start; j < end; j++) {
                    s.setCharAt(j, '#');
                }
            }
            start = end + 1;
        }
    }

    /// Masks every run of at least `min` digits, counting digits separated
    /// only by spaces, dashes, dots or parentheses as one run (a phone number).
    private static void maskDigitRuns(StringBuilder s, int min) {
        int i = 0;
        while (i < s.length()) {
            if (!isDigit(s.charAt(i))) {
                i++;
                continue;
            }
            int end = i;
            int digits = 0;
            int lastDigit = i;
            while (end < s.length() && (isDigit(s.charAt(end)) || " -.()".indexOf(s.charAt(end)) >= 0)) {
                if (isDigit(s.charAt(end))) {
                    digits++;
                    lastDigit = end;
                }
                end++;
            }
            if (digits >= min) {
                for (int j = i; j <= lastDigit; j++) {
                    if (isDigit(s.charAt(j))) {
                        s.setCharAt(j, '#');
                    }
                }
            }
            i = end;
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
