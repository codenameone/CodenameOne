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
package com.codenameone.examples.bazaar.ops;

/// Customs data of a variant, checked the same way by the server and the app:
/// a Harmonized System code and an ISO 3166 country of origin.
public final class Customs {
    /// ISO 3166-1 alpha-2 codes, two letters each, in order.
    private static final String ISO =
            "ADAEAFAGAIALAMAOAQARASATAUAWAXAZBABBBDBEBFBGBHBIBJBLBMBNBOBQBRBSBTBVBW"
            + "BYBZCACCCDCFCGCHCICKCLCMCNCOCRCUCVCWCXCYCZDEDJDKDMDODZECEEEGEHERESETFI"
            + "FJFKFMFOFRGAGBGDGEGFGGGHGIGLGMGNGPGQGRGSGTGUGWGYHKHMHNHRHTHUIDIEILIMIN"
            + "IOIQIRISITJEJMJOJPKEKGKHKIKMKNKPKRKWKYKZLALBLCLILKLRLSLTLULVLYMAMCMDME"
            + "MFMGMHMKMLMMMNMOMPMQMRMSMTMUMVMWMXMYMZNANCNENFNGNINLNONPNRNUNZOMPAPEPF"
            + "PGPHPKPLPMPNPRPSPTPWPYQARERORSRURWSASBSCSDSESGSHSISJSKSLSMSNSOSRSSSTSV"
            + "SXSYSZTCTDTFTGTHTJTKTLTMTNTOTRTTTVTWTZUAUGUMUSUYUZVAVCVEVGVIVNVUWFWSYE"
            + "YTZAZMZW";

    private Customs() {
    }

    /// An HS code as stored: its digits, with the dots and spaces people type
    /// dropped. Empty for an empty text; null when it is not 6 to 10 digits
    /// (the six of the international heading, up to four national ones).
    public static String hsCode(String text) {
        String t = text == null ? "" : text.trim();
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c >= '0' && c <= '9') {
                out.append(c);
            } else if (c != '.' && c != ' ') {
                return null;
            }
        }
        if (out.length() == 0) {
            return "";
        }
        return out.length() < 6 || out.length() > 10 ? null : out.toString();
    }

    /// An ISO 3166-1 alpha-2 country in capitals. Empty for an empty text;
    /// null for anything that is not an assigned code.
    public static String country(String text) {
        String t = text == null ? "" : text.trim();
        if (t.length() == 0) {
            return "";
        }
        if (t.length() != 2) {
            return null;
        }
        char a = upper(t.charAt(0));
        char b = upper(t.charAt(1));
        for (int iter = 0; iter + 1 < ISO.length(); iter += 2) {
            if (ISO.charAt(iter) == a && ISO.charAt(iter + 1) == b) {
                return ISO.substring(iter, iter + 2);
            }
        }
        return null;
    }

    private static char upper(char c) {
        return c >= 'a' && c <= 'z' ? (char) (c - 'a' + 'A') : c;
    }
}
