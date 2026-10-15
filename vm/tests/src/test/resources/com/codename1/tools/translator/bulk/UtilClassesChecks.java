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

import com.codename1.impl.JdkHost;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/// StringJoiner, UUID, Properties, HexFormat, EventObject and the exceptions
/// JavaAPI provides.
public final class UtilClassesChecks {
    // A backslash, kept out of string literals next to a 'u': javac reads
    // that pair as an escape wherever it appears.
    private static final String BS = String.valueOf((char) 92);

    private static final String TEXT = "# a comment\n! another\n\nplain=value\n  spaced   :   after colon  \n"
            + "nosep value with spaces\nempty=\nlonely\ncontinued = one, " + BS + "\n     two, " + BS + "\r\n  three\n"
            + "tab=a" + BS + "tb" + BS + "nc\nuni=" + BS + "u00e9t" + BS + "u00E9\n"
            + "key" + BS + " with" + BS + "=odd" + BS + ":chars = v\nlatin=caf" + (char) 0xe9 + "\nplain=again\n"
            + "back=one" + BS + BS + "two\nunknown=" + BS + "q" + BS + "z" + BS + "b\nformfeed" + (char) 12 + "value\n"
            + "trailing=" + BS;

    private UtilClassesChecks() {
    }

    private static TreeMap<String, String> table(Properties p) {
        TreeMap<String, String> out = new TreeMap<String, String>();
        for (String name : p.stringPropertyNames()) {
            out.put(name, p.getProperty(name));
        }
        return out;
    }

    /// What `store` wrote, comparable between the two runs: the comment
    /// lines in order, without the date line (any comment line other than the
    /// ones of `comments`), then the entries sorted, since they come in the
    /// table's hash order. The number of date lines is printed separately.
    private static String stored(String text, String[] comments) {
        List<String> commentLines = new ArrayList<String>();
        List<String> entries = new ArrayList<String>();
        int dates = 0;
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(start, end);
            start = end + 1;
            if (line.startsWith("#")) {
                boolean known = false;
                for (String c : comments) {
                    known |= line.startsWith(c);
                }
                if (known) {
                    commentLines.add(line);
                } else {
                    dates++;
                }
            } else {
                entries.add(line);
            }
        }
        Collections.sort(entries);
        return "dates=" + dates + " comments=" + commentLines + " entries=" + entries;
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("UtilClasses.stringJoinerMatchesTheJdk", () -> {
            String[][] parts = {{}, {"a"}, {"a", "b"}, {"", ""}, {"x", "", "z"}, {"null", "1"}};
            for (int p = 0; p < parts.length; p++) {
                for (int variant = 0; variant < 4; variant++) {
                    String key = "p" + p + "v" + variant;
                    StringJoiner mine = variant < 2 ? new StringJoiner(", ") : new StringJoiner("-", "[", "]");
                    if (variant % 2 == 1) {
                        mine.setEmptyValue("EMPTY");
                    }
                    StringBuilder seen = new StringBuilder();
                    seen.append(mine).append('/').append(mine.length());
                    for (String element : parts[p]) {
                        mine.add(element);
                        seen.append(" | ").append(mine).append('/').append(mine.length());
                    }
                    StringJoiner myOther = new StringJoiner("+", "<", ">");
                    mine.merge(myOther);
                    seen.append(" | ").append(mine);
                    myOther.add("p").add("q");
                    mine.merge(myOther);
                    seen.append(" | ").append(mine);
                    mine.merge(mine);
                    seen.append(" | ").append(mine);
                    mine.add(null);
                    seen.append(" | ").append(mine);
                    Bulk.p(key, seen.toString());
                }
            }
            Bulk.pThrown("nullDelimiter", NullPointerException.class, () -> new StringJoiner(null));
        });
        Bulk.check("UtilClasses.eventObjectMatchesTheJdk", () -> {
            Object source = "the source";
            EventObject mine = new EventObject(source);
            Bulk.p("sameSource", mine.getSource() == source);
            Bulk.p("toString", mine.toString());
            try {
                new EventObject(null);
                Bulk.p("nullSource", "none");
            } catch (IllegalArgumentException e) {
                Bulk.p("nullSource", e.getMessage());
            }
        });
        Bulk.check("UtilClasses.exceptionsCarryWhatTheJdkOnesCarry", () -> {
            IOException io = new IOException("disk");
            Bulk.p("uncheckedMessage", new UncheckedIOException(io).getMessage());
            Bulk.p("uncheckedCause", new UncheckedIOException("m", io).getCause() == io);
            Bulk.p("uncheckedOwnMessage", new UncheckedIOException("m", io).getMessage());
            Bulk.pThrown("uncheckedNullCause", NullPointerException.class, () -> new UncheckedIOException("m", null));

            Throwable target = new IllegalStateException("why");
            InvocationTargetException mine = new InvocationTargetException(target);
            Bulk.p("targetException", mine.getTargetException() == target);
            Bulk.p("targetCause", mine.getCause() == target);
            Bulk.p("targetMessage", mine.getMessage());
            Bulk.p("targetOwnMessage", new InvocationTargetException(target, "m").getMessage());
            // java.lang.MatchException is not compared: the program is compiled
            // for release 17, which does not have it.
        });
        Bulk.check("UtilClasses.loadReadsTheFormatAsTheJdkDoes", () -> {
            Properties actual = new Properties();
            actual.load(new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.ISO_8859_1)));
            Bulk.p("fromStream", table(actual));
            Bulk.p("plain", actual.getProperty("plain"));
            Bulk.p("continued", actual.getProperty("continued"));

            Properties actualText = new Properties();
            actualText.load(new StringReader(TEXT));
            Bulk.p("fromReader", table(actualText));

            // A stream is ISO 8859-1, not UTF-8: each byte is one character.
            byte[] utf8 = ("k=" + (char) 0xe9).getBytes(StandardCharsets.UTF_8);
            Properties ownUtf8 = new Properties();
            ownUtf8.load(new ByteArrayInputStream(utf8));
            Bulk.p("utf8AsLatin1", ownUtf8.getProperty("k"));
            Bulk.p("utf8Length", ownUtf8.getProperty("k").length());
        });
        // The original stored with JavaAPI and read back with the JDK, and the
        // other way around. One run has one implementation, so this compares
        // what each one WRITES, entry by entry, and that it reads it back.
        Bulk.check("UtilClasses.storeAndLoadRoundTripAcrossTheTwoImplementations", () -> {
            Random random = new Random(7);
            String alphabet = "ab =:#!" + BS + "\t\n\r\f" + (char) 0xe9 + (char) 0x4e2d + (char) 1 + "~";
            Properties ours = new Properties();
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
            }
            TreeMap<String, String> expected = table(ours);
            Bulk.p("table", expected);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ours.store(bytes, "a comment\nof two lines " + (char) 0x4e2d);
            boolean printable = true;
            for (byte b : bytes.toByteArray()) {
                printable &= b == '\n' || (b >= 0x20 && b <= 0x7e);
            }
            Bulk.p("streamIsPrintableAscii", printable);
            Bulk.p("storedStream", stored(new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1),
                    new String[] {"#a comment", "#of two lines"}));
            Properties ownRead = new Properties();
            ownRead.load(new ByteArrayInputStream(bytes.toByteArray()));
            Bulk.p("streamReadsBack", expected.equals(table(ownRead)));

            StringWriter text = new StringWriter();
            ours.store(text, null);
            Bulk.p("storedWriter", stored(text.toString(), new String[0]));
            Properties textRead = new Properties();
            textRead.load(new StringReader(text.toString()));
            Bulk.p("writerReadsBack", expected.equals(table(textRead)));

            Properties notAString = new Properties();
            notAString.put("k", Integer.valueOf(1));
            Bulk.pThrown("notAString", ClassCastException.class, () -> notAString.store(new StringWriter(), null));
        });
        Bulk.check("UtilClasses.defaultsStandBehindTheTable", () -> {
            Properties defaults = new Properties();
            defaults.setProperty("a", "default a");
            defaults.setProperty("b", "default b");
            Properties p = new Properties(defaults);
            p.setProperty("a", "own a");
            p.put("notAString", Integer.valueOf(1));

            Bulk.p("a", p.getProperty("a"));
            Bulk.p("b", p.getProperty("b"));
            Bulk.p("c", p.getProperty("c"));
            Bulk.p("cDefault", p.getProperty("c", "fallback"));
            Bulk.p("notAString", p.getProperty("notAString"));
            Bulk.p("stringPropertyNames", new TreeSet<String>(p.stringPropertyNames()));
            List<String> names = new ArrayList<String>();
            for (Object name : Collections.list(p.propertyNames())) {
                names.add(String.valueOf(name));
            }
            Bulk.p("propertyNames", Bulk.sorted(names));
            // The table itself holds only its own entries, and is a Hashtable as the JDK's is.
            Bulk.p("size", p.size());
            Bulk.p("isHashtable", p instanceof java.util.Hashtable);
        });
        Bulk.check("UtilClasses.uuidParsesPrintsAndComparesAsTheJdkDoes", () -> {
            Random random = new Random(3);
            UUID previous = null;
            int hash = 0;
            boolean roundTrips = true;
            for (int i = 0; i < 500; i++) {
                long most = random.nextLong();
                long least = random.nextLong();
                UUID actual = new UUID(most, least);
                String row = actual + " " + actual.hashCode() + " " + actual.version() + " " + actual.variant()
                        + " " + (previous == null ? 0 : Integer.signum(actual.compareTo(previous)));
                if (i < 3) {
                    Bulk.p("row" + i, row);
                }
                hash = 31 * hash + row.hashCode();
                UUID parsed = UUID.fromString(actual.toString());
                roundTrips &= actual.equals(parsed) && parsed.getMostSignificantBits() == most
                        && parsed.getLeastSignificantBits() == least;
                previous = actual;
            }
            Bulk.p("rows", hash);
            Bulk.p("roundTrips", roundTrips);
            Bulk.p("short", UUID.fromString("1-2-3-4-5").toString());
            for (String bad : new String[] {"", "not-a-uuid", "1-2-3-4", "g-2-3-4-5"}) {
                Bulk.pThrown("bad [" + bad + "]", IllegalArgumentException.class, () -> UUID.fromString(bad));
            }
        });
        // The ParparVM run's host reads /dev/urandom, the secure source a
        // port provides.
        Bulk.check("UtilClasses.aRandomUuidIsVersionFourAndDistinct", () -> {
            Set<String> seen = new HashSet<String>();
            boolean ok = true;
            for (int i = 0; i < 2000; i++) {
                UUID u = UUID.randomUUID();
                ok &= u.version() == 4 && u.variant() == 2 && u.toString().equals(UUID.fromString(u.toString())
                        .toString()) && seen.add(u.toString());
            }
            Bulk.p("versionFourVariantTwoDistinct", ok);
        });
        Bulk.cn1Only("UtilClasses.aRandomUuidWithoutASecureSource", () -> {
            JdkHost.install(new JdkHost() {
            });
            Bulk.expect("UnsupportedOperationException".equals(Bulk.thrown(UnsupportedOperationException.class,
                    () -> JdkHost.get().randomBytes(new byte[1]))), "no secure source");
            Set<String> seen = new HashSet<String>();
            for (int i = 0; i < 200; i++) {
                UUID u = UUID.randomUUID();
                Bulk.expect(u.version() == 4, "version 4");
                Bulk.expect(u.variant() == 2, "variant 2");
                Bulk.expect(seen.add(u.toString()), "distinct");
            }
        });
        Bulk.check("UtilClasses.aNameUuidIsTheJdks", () -> {
            // On ParparVM the MD5 is BulkHost's, the core's own Hash.
            for (String name : new String[] {"", "a", "codenameone.com", (char) 0 + "x"}) {
                UUID actual = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
                Bulk.p("[" + Bulk.esc(name) + "]", actual + " version " + actual.version());
            }
        });
        Bulk.cn1Only("UtilClasses.aNameUuidWithoutADigest", () -> {
            JdkHost.install(new JdkHost() {
            });
            Bulk.expectEquals("UnsupportedOperationException", Bulk.thrown(UnsupportedOperationException.class,
                    () -> UUID.nameUUIDFromBytes(new byte[1])), "no MD5");
        });
        Bulk.check("UtilClasses.hexFormatFormatsAndParsesAsDocumented", () -> {
            byte[] bytes = {0, 1, 0x7f, (byte) 0x80, (byte) 0xff, 0x2a};
            HexFormat plain = HexFormat.of();
            Bulk.p("format", plain.formatHex(bytes));
            Bulk.p("parse", plain.parseHex("00017f80ff2a"));
            Bulk.p("parseUpper", plain.parseHex("00017F80FF2A"));
            Bulk.p("formatRange", plain.formatHex(bytes, 1, 3));

            HexFormat fancy = HexFormat.ofDelimiter(", ").withPrefix("#").withSuffix(";").withUpperCase();
            Bulk.p("fancy", fancy.formatHex(bytes));
            Bulk.p("fancyParse", fancy.parseHex("#00;, #01;, #7F;, #80;, #FF;, #2A;"));
            Bulk.p("delimiter", fancy.delimiter());
            Bulk.p("prefix", fancy.prefix());
            Bulk.p("suffix", fancy.suffix());
            Bulk.p("isUpperCase", fancy.isUpperCase());
            Bulk.p("lowerIsUpperCase", fancy.withLowerCase().isUpperCase());
            Bulk.p("equals", fancy.equals(HexFormat.ofDelimiter(", ").withPrefix("#").withSuffix(";").withUpperCase()));

            Bulk.pThrown("parseOdd", IllegalArgumentException.class, () -> plain.parseHex("0"));
            Bulk.pThrown("parseBad", IllegalArgumentException.class, () -> plain.parseHex("zz"));
            Bulk.pThrown("formatBadRange", IndexOutOfBoundsException.class, () -> plain.formatHex(bytes, 4, 2));

            Bulk.p("byte", plain.toHexDigits((byte) 42));
            Bulk.p("short", plain.toHexDigits((short) 42));
            Bulk.p("int", plain.toHexDigits(42));
            Bulk.p("long", plain.toHexDigits(42L));
            Bulk.p("longDigits", plain.toHexDigits(42L, 3));
            Bulk.p("low", plain.toLowHexDigit(0x2a));
            Bulk.p("high", plain.toHighHexDigit(0x2a));
            Bulk.p("isHexF", HexFormat.isHexDigit('F'));
            Bulk.p("isHexG", HexFormat.isHexDigit('g'));
            Bulk.p("fromDigit", HexFormat.fromHexDigit('f'));
            Bulk.p("fromDigits", HexFormat.fromHexDigits("7fffffff"));
            Bulk.p("fromDigitsToLong", HexFormat.fromHexDigitsToLong("ffffffffffffffff"));
            Bulk.pThrown("fromDigitX", NumberFormatException.class, () -> HexFormat.fromHexDigit('x'));
        });
    }
}
