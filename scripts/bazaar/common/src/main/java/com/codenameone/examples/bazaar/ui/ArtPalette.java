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
package com.codenameone.examples.bazaar.ui;

/// The colours and the picture of a product that has no photo, worked out
/// from its id alone so that the same product looks the same on every screen,
/// every device and every run. Pure arithmetic; [ProductArt] paints it.
public final class ArtPalette {
    private ArtPalette() {
    }

    /// A stable, non-negative hash of `text` (FNV-1a). `String.hashCode` is
    /// stable too, but its low bits cluster for ids that differ in the last
    /// character, and neighbouring products would come out the same colour.
    public static int hash(String text) {
        int h = 0x811c9dc5;
        if (text != null) {
            for (int iter = 0; iter < text.length(); iter++) {
                h ^= text.charAt(iter);
                h *= 0x01000193;
            }
        }
        // Not Math.abs: the absolute value of Integer.MIN_VALUE is negative.
        return h & 0x7fffffff;
    }

    /// The two ends of the gradient, 0xRRGGBB: a hue from the hash, the second
    /// end the hue turned a little and darker. Dark mode takes both deeper so
    /// the art does not glare out of a dark page.
    public static int[] colors(String id, boolean dark) {
        int h = hash(id);
        float hue = (h % 360) / 360f;
        float turn = ((h >> 9) % 40 + 20) / 360f;
        float sat = 0.45f + ((h >> 17) % 20) / 100f;
        float light = dark ? 0.50f : 0.80f;
        float end = dark ? 0.32f : 0.58f;
        return new int[] {hsb(hue, sat, light), hsb(hue + turn, sat + 0.1f, end)};
    }

    /// The angle, in steps of 45 degrees, of the soft disc over the gradient,
    /// so two products of one hue still differ.
    public static int corner(String id) {
        return (hash(id) >> 5) % 4;
    }

    /// HSB (each 0..1, hue wrapping) to 0xRRGGBB.
    public static int hsb(float hue, float saturation, float brightness) {
        float h = hue - (float) Math.floor(hue);
        float s = Math.max(0f, Math.min(1f, saturation));
        float b = Math.max(0f, Math.min(1f, brightness));
        float scaled = h * 6f;
        int sector = (int) Math.floor(scaled);
        float f = scaled - sector;
        float p = b * (1f - s);
        float q = b * (1f - s * f);
        float t = b * (1f - s * (1f - f));
        float r;
        float g;
        float bl;
        switch (sector % 6) {
            case 0:
                r = b;
                g = t;
                bl = p;
                break;
            case 1:
                r = q;
                g = b;
                bl = p;
                break;
            case 2:
                r = p;
                g = b;
                bl = t;
                break;
            case 3:
                r = p;
                g = q;
                bl = b;
                break;
            case 4:
                r = t;
                g = p;
                bl = b;
                break;
            default:
                r = b;
                g = p;
                bl = q;
                break;
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(bl * 255);
    }

    /// The first letters of the first two words of a title, upper case for
    /// ASCII letters only (a locale-free fold).
    public static String initials(String title) {
        if (title == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        boolean start = true;
        for (int iter = 0; iter < title.length() && out.length() < 2; iter++) {
            char c = title.charAt(iter);
            if (c == ' ' || c == '-' || c == '/') {
                start = true;
            } else if (start) {
                if (c >= 'a' && c <= 'z') {
                    c = (char) (c - 'a' + 'A');
                }
                if (Character.isLetterOrDigit(c)) {
                    out.append(c);
                }
                start = false;
            }
        }
        return out.toString();
    }

    /// Words in a product type and the icon each one is drawn with, as
    /// indexes into [ProductArt]'s icon table. First match wins; checked
    /// without case folding by `regionMatches`.
    static final String[] TYPE_WORDS = {"mug", "cup", "ceramic", "plate", "bowl", "vase",
        "shirt", "tee", "apparel", "hoodie", "jacket", "dress", "shoe", "sneaker", "bag",
        "book", "coffee", "tea", "food", "candle", "lamp", "light", "plant", "toy", "game",
        "watch", "jewel", "ring", "phone", "audio", "headphone", "camera", "art", "print",
        "gift", "home", "kitchen", "beauty", "soap", "sport", "bike", "pet"};

    /// Index into [#TYPE_WORDS] of the first word found in `type` or `title`,
    /// or -1.
    public static int typeWord(String type, String title) {
        int found = find(type);
        return found >= 0 ? found : find(title);
    }

    private static int find(String text) {
        if (text == null) {
            return -1;
        }
        for (int word = 0; word < TYPE_WORDS.length; word++) {
            String w = TYPE_WORDS[word];
            for (int at = 0; at + w.length() <= text.length(); at++) {
                if (text.regionMatches(true, at, w, 0, w.length())) {
                    return word;
                }
            }
        }
        return -1;
    }
}
