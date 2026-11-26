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
package com.codenameone.examples.bazaar.docs;

import com.codename1.pdf.PdfCanvas;

/// Code 128, code set B: printable ASCII as a barcode any scanner reads.
///
/// A symbol is a start character, one character per letter, a check
/// character and a stop. Each character is six alternating bars and spaces
/// (the stop has seven) whose widths, in modules, add up to eleven. The
/// check character is the start's value plus each character's value times its
/// position (from 1), modulo 103.
public final class Code128 {
    /// Value of the code set B start character.
    public static final int START_B = 104;
    /// Value of the stop character.
    public static final int STOP = 106;

    /// Bar and space widths of every value, 0 to 106.
    static final String[] PATTERNS = {
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312",
        "132212", "221213", "221312", "231212", "112232", "122132", "122231", "113222",
        "123122", "123221", "223211", "221132", "221231", "213212", "223112", "312131",
        "311222", "321122", "321221", "312212", "322112", "322211", "212123", "212321",
        "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121",
        "313121", "211331", "231131", "213113", "213311", "213131", "311123", "311321",
        "331121", "312113", "312311", "332111", "314111", "221411", "431111", "111224",
        "111422", "121124", "121421", "141122", "141221", "112214", "112412", "122114",
        "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112",
        "421211", "212141", "214121", "412121", "111143", "111341", "131141", "114113",
        "114311", "411113", "411311", "113141", "114131", "311141", "411131", "211412",
        "211214", "211232", "2331112",
    };

    /// Modules of blank space either side, which scanners need.
    public static final int QUIET = 10;

    private Code128() {
    }

    /// The value of a character in code set B; characters outside printable
    /// ASCII are encoded as `?`.
    static int value(char ch) {
        return ch >= 32 && ch <= 126 ? ch - 32 : '?' - 32;
    }

    /// The check character of `text` under code set B.
    public static int checksum(String text) {
        long sum = START_B;
        for (int iter = 0; iter < text.length(); iter++) {
            sum += (long) (iter + 1) * value(text.charAt(iter));
        }
        return (int) (sum % 103);
    }

    /// Every value of the symbol: start, characters, check, stop.
    public static int[] encode(String text) {
        int[] out = new int[text.length() + 3];
        out[0] = START_B;
        for (int iter = 0; iter < text.length(); iter++) {
            out[iter + 1] = value(text.charAt(iter));
        }
        out[text.length() + 1] = checksum(text);
        out[text.length() + 2] = STOP;
        return out;
    }

    /// The symbol as a string of `1` (bar) and `0` (space) modules, without
    /// quiet zones.
    public static String modules(String text) {
        int[] values = encode(text);
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < values.length; iter++) {
            String p = PATTERNS[values[iter]];
            for (int w = 0; w < p.length(); w++) {
                char bit = w % 2 == 0 ? '1' : '0';
                for (int m = p.charAt(w) - '0'; m > 0; m--) {
                    out.append(bit);
                }
            }
        }
        return out.toString();
    }

    /// Draws the symbol for `text` as filled rectangles in black, quiet zones
    /// included, filling `width` by `height` points from `(x, y)` (top left).
    public static void draw(PdfCanvas canvas, String text, float x, float y, float width,
            float height) {
        String bits = modules(text);
        int total = bits.length() + 2 * QUIET;
        float module = width / total;
        canvas.save();
        canvas.setFillColor(0x000000);
        int iter = 0;
        while (iter < bits.length()) {
            if (bits.charAt(iter) != '1') {
                iter++;
                continue;
            }
            int run = iter;
            while (run < bits.length() && bits.charAt(run) == '1') {
                run++;
            }
            canvas.fillRect(x + (QUIET + iter) * module, y, (run - iter) * module, height);
            iter = run;
        }
        canvas.restore();
    }
}
