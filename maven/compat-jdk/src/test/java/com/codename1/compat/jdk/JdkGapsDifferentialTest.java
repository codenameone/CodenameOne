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
package com.codename1.compat.jdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds `Properties`, `UUID` and the stand-ins for the missing members of
/// `Objects` and `Locale` against the JDK's own.
public class JdkGapsDifferentialTest {

    // A backslash, kept out of string literals next to a 'u': javac reads
    // that pair as an escape wherever it appears.
    private static final String BS = String.valueOf((char) 92);

    // ------------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------------

    private static final String TEXT = "# a comment\n! another\n\nplain=value\n  spaced   :   after colon  \n"
            + "nosep value with spaces\nempty=\nlonely\ncontinued = one, " + BS + "\n     two, " + BS + "\r\n  three\n"
            + "tab=a" + BS + "tb" + BS + "nc\nuni=" + BS + "u00e9t" + BS + "u00E9\n"
            + "key" + BS + " with" + BS + "=odd" + BS + ":chars = v\nlatin=caf\u00e9\nplain=again\n"
            + "back=one" + BS + BS + "two\nunknown=" + BS + "q" + BS + "z\n";

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
    public void loadReadsTheFormatAsTheJdkDoes() throws Exception {
        java.util.Properties expected = new java.util.Properties();
        expected.load(new ByteArrayInputStream(TEXT.getBytes("ISO-8859-1")));
        Properties actual = new Properties();
        actual.load(new ByteArrayInputStream(TEXT.getBytes("ISO-8859-1")));
        assertEquals(jdk(expected), ours(actual));
        assertEquals("again", actual.getProperty("plain"));
        assertEquals("one, two, three", actual.getProperty("continued"));

        java.util.Properties expectedText = new java.util.Properties();
        expectedText.load(new StringReader(TEXT));
        Properties actualText = new Properties();
        actualText.load(new StringReader(TEXT));
        assertEquals(jdk(expectedText), ours(actualText));
    }

    /// What we store, the JDK reads back to the same table, and the other
    /// way around; through a stream and through a writer.
    @Test
    public void storeAndLoadRoundTripAcrossTheTwoImplementations() throws Exception {
        Random random = new Random(7);
        String alphabet = "ab =:#!" + BS + "\t\n\r\f\u00e9\u4e2d\u0001~";
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
        ours.store(bytes, "a comment\nof two lines \u4e2d");
        for (byte b : bytes.toByteArray()) {
            assertTrue("a stream holds printable ASCII only", b == '\n' || (b >= 0x20 && b <= 0x7e));
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
    }

    @Test
    public void defaultsStandBehindTheTable() {
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
        assertEquals(new TreeSet<String>(java.util.Arrays.asList("a", "b")),
                new TreeSet<String>(p.stringPropertyNames()));
        Set<Object> names = new HashSet<Object>(Collections.list(p.propertyNames()));
        assertEquals(new HashSet<Object>(java.util.Arrays.asList("a", "b", "notAString")), names);
        // The table itself holds only its own entries.
        assertEquals(2, p.size());
    }

    // ------------------------------------------------------------------
    // UUID
    // ------------------------------------------------------------------

    @Test
    public void uuidParsesPrintsAndComparesAsTheJdkDoes() {
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
            try {
                UUID.fromString(bad);
                fail(bad + " is not a UUID");
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected);
            }
        }
    }

    @Test
    public void aRandomUuidIsVersionFourAndDistinct() {
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 2000; i++) {
            UUID u = UUID.randomUUID();
            assertEquals(4, u.version());
            assertEquals(2, u.variant());
            assertEquals(u.toString(), java.util.UUID.fromString(u.toString()).toString());
            assertTrue(seen.add(u.toString()));
        }
    }

    // ------------------------------------------------------------------
    // Objects
    // ------------------------------------------------------------------

    @Test
    public void theObjectsMethodsBehaveAsTheJdkOnes() {
        assertTrue(JdkObjects.isNull(null));
        assertFalse(JdkObjects.isNull(""));
        assertEquals("v", JdkObjects.requireNonNull("v", (Supplier<String>) null));
        try {
            JdkObjects.requireNonNull(null, new Supplier<String>() {
                @Override
                public String get() {
                    return "the message";
                }
            });
            fail();
        } catch (NullPointerException expected) {
            assertEquals("the message", expected.getMessage());
        }
        assertEquals("v", JdkObjects.requireNonNullElse("v", "d"));
        assertEquals("d", JdkObjects.requireNonNullElse(null, "d"));
        try {
            JdkObjects.requireNonNullElse(null, null);
            fail();
        } catch (NullPointerException expected) {
            assertNotNull(expected);
        }
        assertEquals("made", JdkObjects.requireNonNullElseGet(null, new Supplier<String>() {
            @Override
            public String get() {
                return "made";
            }
        }));
        assertEquals(3, JdkObjects.checkIndex(3, 4));
        assertEquals(1, JdkObjects.checkFromToIndex(1, 4, 4));
        assertEquals(1, JdkObjects.checkFromIndexSize(1, 3, 4));
        int[][] bad = {{4, 4}, {-1, 4}, {0, 0}};
        for (int[] b : bad) {
            try {
                JdkObjects.checkIndex(b[0], b[1]);
                fail();
            } catch (IndexOutOfBoundsException expected) {
                assertNotNull(expected);
            }
        }
        try {
            JdkObjects.checkFromToIndex(3, 2, 4);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertNotNull(expected);
        }
        try {
            // Would overflow if the bound were computed as from + size.
            JdkObjects.checkFromIndexSize(2, Integer.MAX_VALUE, 4);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertNotNull(expected);
        }
    }

    // ------------------------------------------------------------------
    // Locale
    // ------------------------------------------------------------------

    @Test
    public void theLocaleConstantsAreTheJdkOnes() throws Exception {
        for (java.lang.reflect.Field f : JdkLocale.class.getFields()) {
            Locale ours = (Locale) f.get(null);
            Locale jdk = (Locale) Locale.class.getField(f.getName()).get(null);
            assertEquals(f.getName(), jdk.getLanguage(), ours.getLanguage());
            assertEquals(f.getName(), jdk.getCountry(), ours.getCountry());
        }
        assertSame(JdkLocale.CHINA, JdkLocale.SIMPLIFIED_CHINESE);
    }

    @Test
    public void languageTagsAndIdentityFollowTheJdk() {
        for (String tag : new String[] {"fr", "en-US", "EN-us", "zh-Hans-CN", "de-CH-1996", "es-419", "und", "",
            "pt_BR", "en-US-u-ca-gregory", "x"}) {
            Locale jdk = Locale.forLanguageTag(tag.replace('_', '-'));
            Locale ours = JdkLocale.forLanguageTag(tag);
            assertEquals(tag, jdk.getLanguage(), ours.getLanguage());
            assertEquals(tag, jdk.getCountry(), ours.getCountry());
            Locale plain = new Locale(jdk.getLanguage(), jdk.getCountry());
            assertEquals(tag, plain.toLanguageTag(), JdkLocale.toLanguageTag(ours));
            assertEquals(tag, plain.toString(), JdkLocale.toString(ours));
        }
        Locale a = new Locale("fr", "CA");
        assertTrue(JdkLocale.equals(a, new Locale("fr", "CA")));
        assertTrue(JdkLocale.equals(a, a));
        assertFalse(JdkLocale.equals(a, new Locale("fr", "")));
        assertFalse(JdkLocale.equals(a, "fr_CA"));
        assertFalse(JdkLocale.equals(a, null));
        assertEquals(JdkLocale.hashCode(a), JdkLocale.hashCode(new Locale("fr", "CA")));
        assertNotEquals(JdkLocale.hashCode(a), JdkLocale.hashCode(new Locale("fr", "FR")));
        assertEquals("", JdkLocale.getVariant(a));
        assertEquals("", JdkLocale.getScript(a));
        assertNotNull(JdkLocale.getDisplayName(a));
        assertTrue(JdkLocale.getAvailableLocales().length > 0);
    }
}
