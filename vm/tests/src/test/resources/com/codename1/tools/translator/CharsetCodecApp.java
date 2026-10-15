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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Prints what String.getBytes, new String(byte[], ...), InputStreamReader and
 * OutputStreamWriter answer for every charset name the class library accepts,
 * and for the names it must refuse.
 *
 * CharsetCodecTest runs this on the host JDK and through ParparVM and requires
 * identical output. Characters are built from code points so the source stays
 * ASCII, and every result is printed as hex.
 *
 * Two things are deliberately left out. Charsets the JDK supports and this
 * library does not (windows-1252, UTF-32, ...) answer differently by design,
 * so no name used here may be one of those. And a decoded U+FFFE is printed as
 * U+FFFD: JDK 11 reads the unit as malformed and replaces it, JDK 17 and later
 * keep it, and the reference JDK this runs on may be either.
 */
public class CharsetCodecApp {

    private static final String[] NAMES = {
        "UTF-8", "UTF8", "unicode-1-1-utf-8", "utf-8", "Utf8",
        "ISO-8859-1", "819", "8859_1", "IBM-819", "IBM819", "ISO8859-1", "ISO8859_1",
        "ISO_8859-1", "ISO_8859-1:1987", "ISO_8859_1", "cp819", "csISOLatin1",
        "iso-ir-100", "l1", "latin1", "Latin1", "L1",
        "US-ASCII", "646", "ANSI_X3.4-1968", "ANSI_X3.4-1986", "ASCII", "IBM367",
        "ISO646-US", "ISO_646.irv:1991", "ascii7", "cp367", "csASCII", "iso-ir-6",
        "iso_646.irv:1983", "us", "us-ascii",
        "UTF-16", "UTF_16", "UnicodeBig", "unicode", "utf16", "utf-16",
        "UTF-16BE", "ISO-10646-UCS-2", "UTF_16BE", "UnicodeBigUnmarked", "X-UTF-16BE",
        "UTF-16LE", "UTF_16LE", "UnicodeLittleUnmarked", "X-UTF-16LE", "utf-16le",
        "ISO-8859-2", "8859_2", "912", "ISO8859-2", "ISO_8859-2", "ISO_8859-2:1987",
        "cp912", "csISOLatin2", "ibm-912", "ibm912", "iso-ir-101", "iso8859_2", "l2",
        "latin2", "LATIN2"
    };

    /** The canonical names: the conversions are printed once per charset. */
    private static final String[] CANONICAL = {
        "UTF-8", "ISO-8859-1", "US-ASCII", "UTF-16", "UTF-16BE", "UTF-16LE", "ISO-8859-2"
    };

    /** Names no JDK supports either, and the empty name. */
    private static final String[] UNKNOWN = {
        "NO_SUCH_CHARSET", "bad name!", "UTF-9", "", "utf 8", " UTF-8"
    };

    public static void main(String[] args) throws Exception {
        String accented = "caf" + (char) 0xE9;
        String latin2 = "" + (char) 0x0141 + (char) 0x00F3 + (char) 0x017C + (char) 0x02D9
                + (char) 0x0104 + (char) 0x00A0;
        String emoji = "" + (char) 0xD83D + (char) 0xDE00;
        String[] texts = {
            "", "plain", accented, latin2, "" + (char) 0x20AC + "5", emoji,
            "a" + emoji + "b" + (char) 0xFFFF,
            // C1 controls, which ISO-8859-1 and ISO-8859-2 hold and US-ASCII does not.
            "" + (char) 0x7F + (char) 0x80 + (char) 0x9F + (char) 0xA0 + (char) 0xFF + (char) 0x100,
            // Lone surrogates, a high at the end and a low on its own.
            "x" + (char) 0xD800, "" + (char) 0xDC00 + "y", "" + (char) 0xD800 + (char) 0xD800,
            "" + (char) 0xFEFF + "bom",
            // Long enough to leave any short-input fast path.
            repeat(accented + latin2 + "0123456789", 20)
        };
        byte[] every = new byte[256];
        for (int i = 0; i < 256; i++) {
            every[i] = (byte) i;
        }
        byte[][] blobs = {
            {}, {0x41}, every,
            // Broken UTF-8: a cut sequence, an encoded surrogate, beyond U+10FFFF,
            // an overlong NUL, a stray continuation byte.
            {(byte) 0xC3}, {(byte) 0xE2, (byte) 0x82}, {(byte) 0xED, (byte) 0xA0, (byte) 0x80},
            {(byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80}, {(byte) 0xC0, (byte) 0x80},
            {0x41, (byte) 0x80, 0x42},
            // UTF-16: marks in both orders, none, an odd byte, cut and broken pairs.
            {(byte) 0xFE, (byte) 0xFF, 0x00, 0x41}, {(byte) 0xFF, (byte) 0xFE, 0x41, 0x00},
            {0x00, 0x41, 0x00, 0x42}, {0x00, 0x41, 0x00},
            {(byte) 0xD8, 0x3D, (byte) 0xDE, 0x00}, {0x3D, (byte) 0xD8, 0x00, (byte) 0xDE},
            {(byte) 0xD8, 0x00}, {(byte) 0xD8, 0x00, 0x41}, {(byte) 0xD8, 0x00, 0x00, 0x41},
            {(byte) 0xDC, 0x00, 0x00, 0x41}, {(byte) 0xFE, (byte) 0xFF}, {(byte) 0xFE},
            {(byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF}
        };

        StringBuilder sb = new StringBuilder("RESULT=");
        for (int c = 0; c < CANONICAL.length; c++) {
            String name = CANONICAL[c];
            for (int t = 0; t < texts.length; t++) {
                sb.append(hex(texts[t].getBytes(name))).append(';');
            }
            for (int b = 0; b < blobs.length; b++) {
                sb.append(codePoints(new String(blobs[b], name))).append('|');
                if (blobs[b].length > 2) {
                    sb.append(codePoints(new String(blobs[b], 1, blobs[b].length - 2, name)));
                }
                sb.append(';');
            }
            sb.append(codePoints(read(blobs[2], name))).append(';');
            sb.append(codePoints(read(blobs[9], name))).append(';');
            sb.append(hex(write(name, texts))).append(';');
            Charset charset = Charset.forName(name);
            sb.append(charset.name()).append('|');
            sb.append(hex(accented.getBytes(charset))).append('|');
            sb.append(codePoints(new String(every, charset))).append(';');
        }
        // Every name and alias names the same charset as its canonical name.
        String sample = accented + latin2 + emoji + (char) 0xD800;
        for (int n = 0; n < NAMES.length; n++) {
            String name = NAMES[n];
            sb.append(name).append('=').append(hex(sample.getBytes(name))).append('|');
            sb.append(codePoints(new String(every, name))).append('|');
            sb.append(Charset.forName(name).name()).append(';');
        }
        // Refused before anything is converted, so the empty input is refused too.
        for (int n = 0; n < UNKNOWN.length; n++) {
            sb.append(refusals(UNKNOWN[n])).append(';');
        }
        sb.append(refusals(null)).append(';');
        System.out.println(sb.toString());
    }

    private static String refusals(final String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(outcome(new Attempt() {
            public Object run() throws Exception {
                return "abc".getBytes(name);
            }
        })).append(',');
        sb.append(outcome(new Attempt() {
            public Object run() throws Exception {
                return "".getBytes(name);
            }
        })).append(',');
        sb.append(outcome(new Attempt() {
            public Object run() throws Exception {
                return new String(new byte[] {0x41}, name);
            }
        })).append(',');
        sb.append(outcome(new Attempt() {
            public Object run() throws Exception {
                return new String(new byte[0], 0, 0, name);
            }
        })).append(',');
        sb.append(outcome(new Attempt() {
            public Object run() throws Exception {
                return new InputStreamReader(new ByteArrayInputStream(new byte[0]), name);
            }
        })).append(',');
        sb.append(outcome(new Attempt() {
            public Object run() throws Exception {
                return new OutputStreamWriter(new ByteArrayOutputStream(), name);
            }
        }));
        return sb.toString();
    }

    interface Attempt {
        Object run() throws Exception;
    }

    private static String outcome(Attempt a) {
        try {
            a.run();
            return "accepted";
        } catch (UnsupportedEncodingException e) {
            return "unsupported";
        } catch (NullPointerException e) {
            return "npe";
        } catch (Exception e) {
            return "other";
        }
    }

    private static String read(byte[] bytes, String name) throws Exception {
        InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(bytes), name);
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[7];
        int n;
        while ((n = r.read(buf, 0, buf.length)) > 0) {
            sb.append(buf, 0, n);
        }
        r.close();
        return sb.toString();
    }

    /** Several writes, so a byte order mark written per write would show. */
    private static byte[] write(String name, String[] texts) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStreamWriter w = new OutputStreamWriter(bytes, name);
        w.write("ab");
        w.write(texts[2]);
        w.write(texts[3], 1, 3);
        w.write((int) 'z');
        w.write(new char[] {'q', (char) 0xE9}, 0, 2);
        w.close();
        return bytes.toByteArray();
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            sb.append("0123456789abcdef".charAt(v >> 4)).append("0123456789abcdef".charAt(v & 0xf));
        }
        return sb.toString();
    }

    private static String codePoints(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            char c = s.charAt(i);
            sb.append(Integer.toHexString(c == 0xFFFE ? 0xFFFD : c));
        }
        return sb.toString();
    }
}
