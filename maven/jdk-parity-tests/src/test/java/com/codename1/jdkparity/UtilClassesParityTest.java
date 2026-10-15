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
package com.codename1.jdkparity;

import com.codename1.impl.JdkHost;
import jdkparity.java.io.UncheckedIOException;
import jdkparity.java.lang.MatchException;
import jdkparity.java.lang.reflect.InvocationTargetException;
import jdkparity.java.util.EventObject;
import jdkparity.java.util.HexFormat;
import jdkparity.java.util.Properties;
import jdkparity.java.util.StringJoiner;
import jdkparity.java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StringJoiner, UUID, Properties, HexFormat, EventObject and the exceptions
 * JavaAPI took over from compat-jdk, beside the JDK's own where the JDK this
 * runs on has the class, and against the JDK's documented answers where it
 * does not (HexFormat is Java 17).
 */
class UtilClassesParityTest {

    // A backslash, kept out of string literals next to a 'u': javac reads
    // that pair as an escape wherever it appears.
    private static final String BS = String.valueOf((char) 92);

    @AfterEach
    void restoreHost() {
        JdkHost.install(null);
    }

    // ------------------------------------------------------------------
    // StringJoiner

    @Test
    void stringJoinerMatchesTheJdk() {
        String[][] parts = {{}, {"a"}, {"a", "b"}, {"", ""}, {"x", "", "z"}, {"null", "1"}};
        for (String[] elements : parts) {
            for (int variant = 0; variant < 4; variant++) {
                java.util.StringJoiner theirs = variant < 2
                        ? new java.util.StringJoiner(", ") : new java.util.StringJoiner("-", "[", "]");
                StringJoiner mine = variant < 2 ? new StringJoiner(", ") : new StringJoiner("-", "[", "]");
                if (variant % 2 == 1) {
                    theirs.setEmptyValue("EMPTY");
                    mine.setEmptyValue("EMPTY");
                }
                assertEquals(theirs.toString(), mine.toString());
                assertEquals(theirs.length(), mine.length());
                for (String element : elements) {
                    theirs.add(element);
                    mine.add(element);
                    assertEquals(theirs.toString(), mine.toString());
                    assertEquals(theirs.length(), mine.length());
                }
                java.util.StringJoiner theirOther = new java.util.StringJoiner("+", "<", ">");
                StringJoiner myOther = new StringJoiner("+", "<", ">");
                theirs.merge(theirOther);
                mine.merge(myOther);
                assertEquals(theirs.toString(), mine.toString());
                theirOther.add("p").add("q");
                myOther.add("p").add("q");
                theirs.merge(theirOther);
                mine.merge(myOther);
                assertEquals(theirs.toString(), mine.toString());
                theirs.merge(theirs);
                mine.merge(mine);
                assertEquals(theirs.toString(), mine.toString());
                theirs.add(null);
                mine.add(null);
                assertEquals(theirs.toString(), mine.toString());
            }
        }
        assertThrows(NullPointerException.class, () -> new StringJoiner(null));
    }

    // ------------------------------------------------------------------
    // EventObject and the exceptions

    @Test
    void eventObjectMatchesTheJdk() {
        Object source = "the source";
        EventObject mine = new EventObject(source);
        java.util.EventObject theirs = new java.util.EventObject(source);
        assertSame(source, mine.getSource());
        assertEquals(theirs.toString().replace("java.util.EventObject", EventObject.class.getName()),
                mine.toString());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new EventObject(null));
        assertEquals("null source", e.getMessage());
    }

    @Test
    void exceptionsCarryWhatTheJdkOnesCarry() {
        IOException io = new IOException("disk");
        assertEquals(new java.io.UncheckedIOException(io).getMessage(), new UncheckedIOException(io).getMessage());
        assertSame(io, new UncheckedIOException("m", io).getCause());
        assertEquals("m", new UncheckedIOException("m", io).getMessage());
        assertThrows(NullPointerException.class, () -> new UncheckedIOException("m", null));

        Throwable target = new IllegalStateException("why");
        java.lang.reflect.InvocationTargetException theirs = new java.lang.reflect.InvocationTargetException(target);
        InvocationTargetException mine = new InvocationTargetException(target);
        assertSame(target, mine.getTargetException());
        assertSame(target, mine.getCause());
        assertEquals(theirs.getMessage(), mine.getMessage());
        assertEquals(new java.lang.reflect.InvocationTargetException(target, "m").getMessage(),
                new InvocationTargetException(target, "m").getMessage());

        MatchException match = new MatchException("no case", target);
        assertEquals("no case", match.getMessage());
        assertSame(target, match.getCause());
        assertTrue(match instanceof RuntimeException);
    }

    // ------------------------------------------------------------------
    // Properties

    private static final String TEXT = "# a comment\n! another\n\nplain=value\n  spaced   :   after colon  \n"
            + "nosep value with spaces\nempty=\nlonely\ncontinued = one, " + BS + "\n     two, " + BS + "\r\n  three\n"
            + "tab=a" + BS + "tb" + BS + "nc\nuni=" + BS + "u00e9t" + BS + "u00E9\n"
            + "key" + BS + " with" + BS + "=odd" + BS + ":chars = v\nlatin=caf" + (char) 0xe9 + "\nplain=again\n"
            + "back=one" + BS + BS + "two\nunknown=" + BS + "q" + BS + "z" + BS + "b\nformfeed" + (char) 12 + "value\n"
            + "trailing=" + BS;

    private static TreeMap<String, String> jdk(java.util.Properties p) {
        TreeMap<String, String> out = new TreeMap<String, String>();
        for (String name : p.stringPropertyNames()) {
            out.put(name, p.getProperty(name));
        }
        return out;
    }

    private static TreeMap<String, String> ours(Properties p) {
        TreeMap<String, String> out = new TreeMap<String, String>();
        for (String name : p.stringPropertyNames()) {
            out.put(name, p.getProperty(name));
        }
        return out;
    }

    @Test
    void loadReadsTheFormatAsTheJdkDoes() throws Exception {
        java.util.Properties expected = new java.util.Properties();
        expected.load(new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.ISO_8859_1)));
        Properties actual = new Properties();
        actual.load(new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals(jdk(expected), ours(actual));
        assertEquals("again", actual.getProperty("plain"));
        assertEquals("one, two, three", actual.getProperty("continued"));

        java.util.Properties expectedText = new java.util.Properties();
        expectedText.load(new StringReader(TEXT));
        Properties actualText = new Properties();
        actualText.load(new StringReader(TEXT));
        assertEquals(jdk(expectedText), ours(actualText));

        // A stream is ISO 8859-1, not UTF-8: each byte is one character.
        byte[] utf8 = ("k=" + (char) 0xe9).getBytes(StandardCharsets.UTF_8);
        java.util.Properties jdkUtf8 = new java.util.Properties();
        jdkUtf8.load(new ByteArrayInputStream(utf8));
        Properties ownUtf8 = new Properties();
        ownUtf8.load(new ByteArrayInputStream(utf8));
        assertEquals(jdkUtf8.getProperty("k"), ownUtf8.getProperty("k"));
        assertEquals(2, ownUtf8.getProperty("k").length());
    }

    /** What we store, the JDK reads back to the same table, and the other way around. */
    @Test
    void storeAndLoadRoundTripAcrossTheTwoImplementations() throws Exception {
        Random random = new Random(7);
        String alphabet = "ab =:#!" + BS + "\t\n\r\f" + (char) 0xe9 + (char) 0x4e2d + (char) 1 + "~";
        Properties ours = new Properties();
        java.util.Properties theirs = new java.util.Properties();
        for (int i = 0; i < 300; i++) {
            StringBuilder key = new StringBuilder("k" + i);
            StringBuilder value = new StringBuilder();
            for (int j = random.nextInt(8); j > 0; j--) {
                key.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            for (int j = random.nextInt(12); j > 0; j--) {
                value.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            ours.setProperty(key.toString(), value.toString());
            theirs.setProperty(key.toString(), value.toString());
        }
        TreeMap<String, String> expected = jdk(theirs);
        assertEquals(expected, ours(ours));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ours.store(bytes, "a comment\nof two lines " + (char) 0x4e2d);
        for (byte b : bytes.toByteArray()) {
            assertTrue(b == '\n' || (b >= 0x20 && b <= 0x7e), "a stream holds printable ASCII only");
        }
        java.util.Properties jdkRead = new java.util.Properties();
        jdkRead.load(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(expected, jdk(jdkRead));
        Properties ownRead = new Properties();
        ownRead.load(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(expected, ours(ownRead));

        StringWriter text = new StringWriter();
        ours.store(text, null);
        java.util.Properties jdkText = new java.util.Properties();
        jdkText.load(new StringReader(text.toString()));
        assertEquals(expected, jdk(jdkText));

        ByteArrayOutputStream jdkBytes = new ByteArrayOutputStream();
        theirs.store(jdkBytes, "from the JDK");
        Properties fromJdk = new Properties();
        fromJdk.load(new ByteArrayInputStream(jdkBytes.toByteArray()));
        assertEquals(expected, ours(fromJdk));

        Properties notAString = new Properties();
        notAString.put("k", Integer.valueOf(1));
        assertThrows(ClassCastException.class, () -> notAString.store(new StringWriter(), null));
    }

    @Test
    void defaultsStandBehindTheTable() {
        Properties defaults = new Properties();
        defaults.setProperty("a", "default a");
        defaults.setProperty("b", "default b");
        Properties p = new Properties(defaults);
        p.setProperty("a", "own a");
        p.put("notAString", Integer.valueOf(1));

        assertEquals("own a", p.getProperty("a"));
        assertEquals("default b", p.getProperty("b"));
        assertNull(p.getProperty("c"));
        assertEquals("fallback", p.getProperty("c", "fallback"));
        assertNull(p.getProperty("notAString"));
        assertEquals(new TreeSet<String>(Arrays.asList("a", "b")), new TreeSet<String>(p.stringPropertyNames()));
        Set<Object> names = new HashSet<Object>(Collections.list(p.propertyNames()));
        assertEquals(new HashSet<Object>(Arrays.asList("a", "b", "notAString")), names);
        // The table itself holds only its own entries, and is a Hashtable as the JDK's is.
        assertEquals(2, p.size());
        assertTrue(p instanceof java.util.Hashtable);
    }

    // ------------------------------------------------------------------
    // UUID

    /** A host with the JDK's secure random and digests, as a port provides them. */
    private static final class SecureHost extends JdkHost {
        private final SecureRandom random = new SecureRandom();

        @Override
        public void randomBytes(byte[] out) {
            random.nextBytes(out);
        }

        @Override
        public Digest digest(String algorithm) {
            final MessageDigest md;
            try {
                md = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
            return new Digest() {
                @Override
                public void update(byte[] data, int offset, int length) {
                    md.update(data, offset, length);
                }

                @Override
                public byte[] digest() {
                    return md.digest();
                }

                @Override
                public void reset() {
                    md.reset();
                }

                @Override
                public int length() {
                    return md.getDigestLength();
                }
            };
        }
    }

    @Test
    void uuidParsesPrintsAndComparesAsTheJdkDoes() {
        Random random = new Random(3);
        java.util.UUID previousJdk = null;
        UUID previous = null;
        for (int i = 0; i < 500; i++) {
            long most = random.nextLong();
            long least = random.nextLong();
            java.util.UUID expected = new java.util.UUID(most, least);
            UUID actual = new UUID(most, least);
            assertEquals(expected.toString(), actual.toString());
            assertEquals(expected.hashCode(), actual.hashCode());
            assertEquals(expected.version(), actual.version());
            assertEquals(expected.variant(), actual.variant());
            UUID parsed = UUID.fromString(expected.toString());
            assertEquals(actual, parsed);
            assertEquals(most, parsed.getMostSignificantBits());
            assertEquals(least, parsed.getLeastSignificantBits());
            if (previous != null) {
                assertEquals(Integer.signum(expected.compareTo(previousJdk)), Integer.signum(actual.compareTo(previous)));
            }
            previousJdk = expected;
            previous = actual;
        }
        assertEquals(java.util.UUID.fromString("1-2-3-4-5").toString(), UUID.fromString("1-2-3-4-5").toString());
        for (String bad : new String[] {"", "not-a-uuid", "1-2-3-4", "g-2-3-4-5"}) {
            assertThrows(IllegalArgumentException.class, () -> UUID.fromString(bad), bad);
        }
    }

    @Test
    void aRandomUuidIsVersionFourAndDistinct() {
        JdkHost.install(new SecureHost());
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 2000; i++) {
            UUID u = UUID.randomUUID();
            assertEquals(4, u.version());
            assertEquals(2, u.variant());
            assertEquals(u.toString(), java.util.UUID.fromString(u.toString()).toString());
            assertTrue(seen.add(u.toString()));
        }
    }

    /** No secure source installed: still a version 4 identifier, from the fallback. */
    @Test
    void aRandomUuidWithoutASecureSource() {
        assertThrows(UnsupportedOperationException.class, () -> JdkHost.get().randomBytes(new byte[1]));
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 200; i++) {
            UUID u = UUID.randomUUID();
            assertEquals(4, u.version());
            assertEquals(2, u.variant());
            assertTrue(seen.add(u.toString()));
        }
    }

    @Test
    void aNameUuidIsTheJdks() {
        JdkHost.install(new SecureHost());
        for (String name : new String[] {"", "a", "codenameone.com", "\u0000x"}) {
            byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
            java.util.UUID expected = java.util.UUID.nameUUIDFromBytes(bytes);
            UUID actual = UUID.nameUUIDFromBytes(bytes);
            assertEquals(expected.toString(), actual.toString());
            assertEquals(3, actual.version());
        }
        JdkHost.install(null);
        assertThrows(UnsupportedOperationException.class, () -> UUID.nameUUIDFromBytes(new byte[1]));
    }

    // ------------------------------------------------------------------
    // HexFormat: the JDK's answers, written out, since it is Java 17

    @Test
    void hexFormatFormatsAndParsesAsDocumented() {
        byte[] bytes = {0, 1, 0x7f, (byte) 0x80, (byte) 0xff, 0x2a};
        HexFormat plain = HexFormat.of();
        assertEquals("00017f80ff2a", plain.formatHex(bytes));
        assertArrayEquals(bytes, plain.parseHex("00017f80ff2a"));
        assertArrayEquals(bytes, plain.parseHex("00017F80FF2A"));
        assertEquals("017f", plain.formatHex(bytes, 1, 3));

        HexFormat fancy = HexFormat.ofDelimiter(", ").withPrefix("#").withSuffix(";").withUpperCase();
        assertEquals("#00;, #01;, #7F;, #80;, #FF;, #2A;", fancy.formatHex(bytes));
        assertArrayEquals(bytes, fancy.parseHex("#00;, #01;, #7F;, #80;, #FF;, #2A;"));
        assertEquals(", ", fancy.delimiter());
        assertEquals("#", fancy.prefix());
        assertEquals(";", fancy.suffix());
        assertTrue(fancy.isUpperCase());
        assertFalse(fancy.withLowerCase().isUpperCase());
        assertEquals(fancy, HexFormat.ofDelimiter(", ").withPrefix("#").withSuffix(";").withUpperCase());

        assertThrows(IllegalArgumentException.class, () -> plain.parseHex("0"));
        assertThrows(IllegalArgumentException.class, () -> plain.parseHex("zz"));
        assertThrows(IndexOutOfBoundsException.class, () -> plain.formatHex(bytes, 4, 2));

        assertEquals("2a", plain.toHexDigits((byte) 42));
        assertEquals("002a", plain.toHexDigits((short) 42));
        assertEquals("0000002a", plain.toHexDigits(42));
        assertEquals("000000000000002a", plain.toHexDigits(42L));
        assertEquals("02a", plain.toHexDigits(42L, 3));
        assertEquals('a', plain.toLowHexDigit(0x2a));
        assertEquals('2', plain.toHighHexDigit(0x2a));
        assertTrue(HexFormat.isHexDigit('F'));
        assertFalse(HexFormat.isHexDigit('g'));
        assertEquals(15, HexFormat.fromHexDigit('f'));
        assertEquals(0x7fffffff, HexFormat.fromHexDigits("7fffffff"));
        assertEquals(-1L, HexFormat.fromHexDigitsToLong("ffffffffffffffff"));
        assertThrows(NumberFormatException.class, () -> HexFormat.fromHexDigit('x'));
    }
}
