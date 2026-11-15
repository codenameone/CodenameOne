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
package com.codenameone.examples.brickyard.ui;

import com.codename1.io.Preferences;

/// A block avatar: one colour per body part. Kept on the server with the
/// account profile (so it follows the player to every device) and cached on
/// the device.
public final class Avatar {
    public static final int HEAD = 0;
    public static final int TORSO = 1;
    public static final int LEFT_ARM = 2;
    public static final int RIGHT_ARM = 3;
    public static final int LEFT_LEG = 4;
    public static final int RIGHT_LEG = 5;
    public static final String[] PARTS = {"Head", "Torso", "Left arm", "Right arm", "Left leg", "Right leg"};
    /// The colours the editor offers.
    public static final int[] PALETTE = {0xf5cd7a, 0xf0563c, 0xffc43c, 0x3caaff, 0x5ac86e,
        0x8a5cf0, 0xffffff, 0x2b2b2b, 0x8b5a2b, 0xff8fc8};
    private static final int[] DEFAULTS = {0xf5cd7a, 0x3caaff, 0xf5cd7a, 0xf5cd7a, 0x5ac86e, 0x5ac86e};
    private static final String KEY = "brickyard.avatar";

    private final int[] colors;

    public Avatar(int[] colors) {
        this.colors = new int[PARTS.length];
        for (int i = 0; i < PARTS.length; i++) {
            this.colors[i] = colors != null && i < colors.length ? colors[i] & 0xffffff : DEFAULTS[i];
        }
    }

    public int color(int part) {
        return colors[part];
    }

    public Avatar with(int part, int color) {
        int[] copy = new int[colors.length];
        System.arraycopy(colors, 0, copy, 0, colors.length);
        copy[part] = color & 0xffffff;
        return new Avatar(copy);
    }

    /// The colours as comma separated six digit hex, head first.
    public String encode() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) {
                b.append(',');
            }
            String hex = Integer.toHexString(colors[i] | 0x1000000);
            b.append(hex.substring(1));
        }
        return b.toString();
    }

    /// Reads [#encode]'s form; a missing or malformed part keeps its default.
    public static Avatar decode(String text) {
        int[] out = new int[PARTS.length];
        System.arraycopy(DEFAULTS, 0, out, 0, out.length);
        if (text != null) {
            int part = 0;
            int start = 0;
            while (part < out.length && start <= text.length()) {
                int end = text.indexOf(',', start);
                if (end < 0) {
                    end = text.length();
                }
                int v = hex(text, start, end);
                if (v >= 0) {
                    out[part] = v;
                }
                part++;
                start = end + 1;
            }
        }
        return new Avatar(out);
    }

    private static int hex(String s, int from, int to) {
        if (to - from != 6) {
            return -1;
        }
        int v = 0;
        for (int i = from; i < to; i++) {
            char c = s.charAt(i);
            int d;
            if (c >= '0' && c <= '9') {
                d = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                d = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                d = c - 'A' + 10;
            } else {
                return -1;
            }
            v = v * 16 + d;
        }
        return v;
    }

    public static Avatar load() {
        return decode(Preferences.get(KEY, ""));
    }

    public void save() {
        Preferences.set(KEY, encode());
    }
}
