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
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Prints what java.net.URLEncoder and URLDecoder answer for a set of inputs,
 * including the ones that must be refused.
 *
 * UrlCodecTest runs this on the host JDK and through ParparVM and requires
 * identical output. Non-ASCII characters are built from code points so the
 * source stays ASCII, and decoded text is printed as code points so the
 * output does too.
 */
public class UrlCodecApp {

    public static void main(String[] args) throws Exception {
        String accented = "caf" + (char) 0xE9;
        String hebrew = "" + (char) 0x05E9 + (char) 0x05DC + (char) 0x05D5 + (char) 0x05DD;
        // U+1F600, a code point outside the BMP: one surrogate pair, four bytes.
        String emoji = "" + (char) 0xD83D + (char) 0xDE00;
        String[] inputs = {
            "", "plain", "a b", "a+b", "x=1&y=2", "~tilde", "safe.-*_", "100%",
            "/path/to?q=v#frag", accented, hebrew, emoji, "mix " + accented + emoji + "!",
            "" + (char) 0x7F + (char) 0x80 + (char) 0x7FF + (char) 0x800 + (char) 0xFFFF,
            // A lone surrogate, which the JDK's UTF-8 encoder writes as '?'.
            "x" + (char) 0xD800 + "y"
        };
        StringBuilder sb = new StringBuilder("RESULT=");
        for (int i = 0; i < inputs.length; i++) {
            String encoded = URLEncoder.encode(inputs[i], "UTF-8");
            sb.append(encoded).append('|');
            sb.append(codePoints(URLDecoder.decode(encoded, "UTF-8"))).append(';');
        }
        sb.append(URLEncoder.encode("a b" + accented, java.nio.charset.StandardCharsets.UTF_8)).append(';');
        sb.append(URLEncoder.encode("a b" + accented, "ISO-8859-1")).append(';');
        String[] decodes = {
            "a+b%20c", "%41%42c", "%e2%82%ac", "%E2%82%AC", "100%25", "plain", "%F0%9F%98%80"
        };
        for (int i = 0; i < decodes.length; i++) {
            sb.append(codePoints(URLDecoder.decode(decodes[i], "UTF-8"))).append(';');
        }
        sb.append(codePoints(URLDecoder.decode("caf%E9", "ISO-8859-1"))).append(';');
        String[] bad = {"%", "%4", "abc%", "%G1", "%-1", "%4z"};
        for (int i = 0; i < bad.length; i++) {
            try {
                URLDecoder.decode(bad[i], "UTF-8");
                sb.append("accepted;");
            } catch (IllegalArgumentException expected) {
                sb.append("refused;");
            }
        }
        try {
            URLDecoder.decode("x", "");
            sb.append("accepted;");
        } catch (java.io.UnsupportedEncodingException expected) {
            sb.append("unsupported;");
        }
        // A charset is checked when it is named, not when a character first
        // needs converting: text that needs no conversion is refused too.
        String[] unknown = {"NO_SUCH_CHARSET", "bad name!", ""};
        for (int i = 0; i < unknown.length; i++) {
            sb.append(encodeRefusal("abc", unknown[i])).append(';');
            sb.append(decodeRefusal("abc", unknown[i])).append(';');
            // Before the text is looked at at all.
            sb.append(encodeRefusal(null, unknown[i])).append(';');
            sb.append(decodeRefusal(null, unknown[i])).append(';');
        }
        sb.append(encodeRefusal("abc", null)).append(';');
        sb.append(decodeRefusal("abc", null)).append(';');
        // The single byte charsets: what they cannot hold is '?', once for a
        // surrogate pair, and a byte US-ASCII cannot read is U+FFFD.
        String wide = accented + (char) 0x20AC + emoji + (char) 0xD800 + "x";
        String[] names = {"UTF-8", "utf8", "ISO-8859-1", "ISO8859-1", "iso8859_1", "ISO_8859_1",
            "ISO_8859-1", "8859_1", "latin1", "l1", "US-ASCII", "ascii", "us", "ISO646-US",
            "ascii7", "646"};
        for (int i = 0; i < names.length; i++) {
            sb.append(URLEncoder.encode(wide, names[i])).append('|');
            sb.append(codePoints(URLDecoder.decode("%E9A+%41", names[i]))).append(';');
        }
        sb.append(URLEncoder.encode(wide, java.nio.charset.StandardCharsets.US_ASCII)).append(';');
        sb.append(codePoints(URLDecoder.decode("%E9",
                java.nio.charset.StandardCharsets.US_ASCII))).append(';');
        System.out.println(sb.toString());
    }

    private static String encodeRefusal(String s, String enc) {
        try {
            return "accepted:" + URLEncoder.encode(s, enc);
        } catch (java.io.UnsupportedEncodingException e) {
            return "unsupported";
        } catch (NullPointerException e) {
            return "npe";
        }
    }

    private static String decodeRefusal(String s, String enc) {
        try {
            return "accepted:" + URLDecoder.decode(s, enc);
        } catch (java.io.UnsupportedEncodingException e) {
            return "unsupported";
        } catch (NullPointerException e) {
            return "npe";
        }
    }

    private static String codePoints(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Integer.toHexString(s.charAt(i)));
        }
        return sb.toString();
    }
}
